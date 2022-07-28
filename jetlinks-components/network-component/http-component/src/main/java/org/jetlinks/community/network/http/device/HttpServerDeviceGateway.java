package org.jetlinks.community.network.http.device;

import io.vertx.core.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.gateway.AbstractDeviceGateway;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.http.server.HttpServer;
import org.jetlinks.community.network.utils.DeviceGatewayHelper;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.FromDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
public class HttpServerDeviceGateway extends AbstractDeviceGateway {

    private final HttpServer server;

    private final ArrayList<Map<String, String>> routes;

    private final ProtocolSupports supports;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final LongAdder counter = new LongAdder();

    private Disposable disposable;

    private final DeviceGatewayHelper helper;

    public HttpServerDeviceGateway(String id,
                                   ArrayList<Map<String, String>> routes,
                                   ProtocolSupports supports,
                                   DeviceRegistry registry,
                                   DecodedClientMessageHandler clientMessageHandler,
                                   DeviceSessionManager sessionManager,
                                   HttpServer server) {
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
            .handleMessage()
            .subscribeOn(Schedulers.parallel())
            .filter(udp -> isStarted())
            .flatMap(message -> {
                AtomicReference<DeviceSession> sessionRef = new AtomicReference<>();
                sessionRef.set(new HttpDeviceSession(null, server, getTransport(), monitor));

                return getProtocol(message.getUrl())
                    .flatMap(pt -> pt.getMessageCodec(getTransport()))
                    .flatMapMany(codec -> codec.decode(
                        FromDeviceMessageContext.of(sessionRef.get(), message, registry))
                    )
                    .cast(DeviceMessage.class)
                    .flatMap(deviceMessage -> {
                        monitor.receivedMessage();
                        return helper
                            .handleDeviceMessage(
                                deviceMessage,
                                device -> new HttpDeviceSession(device, server, getTransport(), monitor),
                                sessionRef::set,
                                () -> log.warn("The device[{}] in the message body does not exist:{}", deviceMessage.getDeviceId(), deviceMessage)
                            )
                            .thenReturn(deviceMessage);
                    });
            })
            .onErrorResume(error -> {
                log.error(error.getMessage(), error.getCause());
                return Mono.empty();
            })
            .subscribe();
    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.HTTP;
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.HTTP_SERVER;
    }
}
