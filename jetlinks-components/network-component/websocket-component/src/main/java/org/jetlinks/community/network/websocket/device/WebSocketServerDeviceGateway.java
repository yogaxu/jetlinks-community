package org.jetlinks.community.network.websocket.device;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.community.gateway.AbstractDeviceGateway;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.utils.DeviceGatewayHelper;
import org.jetlinks.community.network.websocket.client.WebsocketClient;
import org.jetlinks.community.network.websocket.server.WebSocketServer;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.FromDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.codec.http.websocket.WebSocketMessage;
import org.jetlinks.core.server.DeviceGatewayContext;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.trace.DeviceTracer;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
public class WebSocketServerDeviceGateway extends AbstractDeviceGateway {

    private final WebSocketServer server;

    private final ArrayList<Map<String, String>> routes;

    private final ProtocolSupports supports;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final LongAdder counter = new LongAdder();

    private Disposable disposable;

    private final DeviceGatewayHelper helper;

    public WebSocketServerDeviceGateway(String id,
                                        ArrayList<Map<String, String>> routes,
                                        ProtocolSupports supports,
                                        DeviceRegistry registry,
                                        DecodedClientMessageHandler clientMessageHandler,
                                        DeviceSessionManager sessionManager,
                                        WebSocketServer server) {
        super(id);
        this.routes = routes;
        this.supports = supports;
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.helper = new DeviceGatewayHelper(registry, sessionManager, clientMessageHandler);
        this.server = server;
    }

    /**
     * 解析路由配置，获取请求协议
     *
     * @param path 请求路径
     */
    public Mono<ProtocolSupport> getProtocol(String path) {
        AntPathMatcher matcher = new AntPathMatcher();
        for (Map<String, String> route : routes) {
            if (matcher.match(StringUtils.hasText(route.get("url")) ? route.get("url") : "/**", path)) {
                return supports.getProtocol(route.get("protocol"));
            }
        }
        return Mono.empty();
    }

    @Override
    protected Mono<Void> doShutdown() {
        if (disposable != null) {
            disposable.dispose();
        }
        return Mono.empty();
    }

    @Override
    protected Mono<Void> doStartup() {
        return Mono.fromRunnable(this::doStart);
    }

    private void doStart() {
        if (disposable != null) {
            disposable.dispose();
        }

        disposable = server
            .handleConnection()
            .publishOn(Schedulers.parallel())
            .flatMap(client -> new WebSocketConnection(client)
                    .accept(client.getWebSocketPath())
                    .onErrorResume(err -> {
                        log.error("handle tcp client[{}] error", client.getRemoteAddress(), err);
                        return Mono.empty();
                    })
                , Integer.MAX_VALUE)
            .onErrorContinue((err, obj) -> log.error(err.getMessage(), err))
            .contextWrite(ReactiveLogger.start("network", server.getId()))
            .subscribe(
                ignore -> {
                },
                error -> log.error(error.getMessage(), error)
            );
    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.WebSocket;
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.WEB_SOCKET_SERVER;
    }

    class WebSocketConnection implements DeviceGatewayContext {
        final WebsocketClient client;
        final AtomicReference<DeviceSession> sessionRef = new AtomicReference<>();
        final InetSocketAddress address;

        WebSocketConnection(WebsocketClient client) {
            this.client = client;
            this.address = client.getRemoteAddress();
            monitor.totalConnection(counter.sum());
            client.onDisconnect(() -> {
                counter.decrement();
                monitor.disconnected();
                monitor.totalConnection(counter.sum());
                //check session
                sessionManager
                    .getSession(client.getId())
                    .subscribe();
            });
            monitor.connected();
            sessionRef.set(new UnknownWebSocketDeviceSession(client.getId(), client, getTransport()));
        }

        Mono<Void> accept(String path) {
            return getProtocol(path)
                .flatMap(protocol -> protocol.onClientConnect(getTransport(), client, this))
                .then(
                    client
                        .subscribe()
                        .filter(ws -> isStarted())
                        .flatMap(message -> this.handleWebSocketMessage(message, path))
                        .onErrorResume((err) -> {
                            log.error(err.getMessage(), err);
                            client.shutdown();
                            return Mono.empty();
                        })
                        .then()
                )
                .doOnCancel(client::shutdown);
        }

        Mono<Void> handleWebSocketMessage(WebSocketMessage message, String path) {
            long time = System.nanoTime();
            return getProtocol(path)
                .flatMap(pt -> pt.getMessageCodec(getTransport()))
                .flatMapMany(codec -> codec.decode(FromDeviceMessageContext.of(sessionRef.get(), message, registry)))
                .cast(DeviceMessage.class)
                .flatMap(msg -> this
                    .handleDeviceMessage(msg)
                    .as(MonoTracer.create(
                        DeviceTracer.SpanName.decode(msg.getDeviceId()),
                        builder -> {
                            builder.setAttribute(DeviceTracer.SpanKey.message, msg.toString());
                            builder.setStartTimestamp(time, TimeUnit.NANOSECONDS);
                        })))
                .doOnEach(ReactiveLogger
                    .onError(err -> log.error("Handle WebSocket[{}] message failed:\n{}",
                        address,
                        message
                        , err)))

                .onErrorResume((err) -> Mono.fromRunnable(client::reset))
                .subscribeOn(Schedulers.parallel())
                .then();
        }

        Mono<DeviceMessage> handleDeviceMessage(DeviceMessage message) {
            monitor.receivedMessage();
            return helper
                .handleDeviceMessage(message,
                    device -> new WebSocketDeviceSession(device, client, getTransport(), monitor),
                    session -> {
                        WebSocketDeviceSession deviceSession = session.unwrap(WebSocketDeviceSession.class);
                        deviceSession.setClient(client);
                        sessionRef.set(deviceSession);
                    },
                    () -> log.warn("WebSocket[{}]: The device[{}] in the message body does not exist:{}", address, message.getDeviceId(), message)
                )
                .thenReturn(message);
        }

        @Override
        public Mono<DeviceOperator> getDevice(String deviceId) {
            return registry.getDevice(deviceId);
        }

        @Override
        public Mono<DeviceProductOperator> getProduct(String productId) {
            return registry.getProduct(productId);
        }

        @Override
        public Mono<Void> onMessage(DeviceMessage message) {
            return handleDeviceMessage(message).then();
        }
    }
}
