package org.jetlinks.community.network.coap.device;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.gateway.AbstractDeviceGateway;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.coap.server.CoapServer;
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
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class CoapServerDeviceGateway extends AbstractDeviceGateway {

    private final CoapServer server;

    private final String protocol;

    private final ProtocolSupports supports;

    private final DeviceRegistry registry;

    private Disposable disposable;

    private final DeviceGatewayHelper helper;

    public CoapServerDeviceGateway(String id,
                                   String protocol,
                                   ProtocolSupports supports,
                                   DeviceRegistry registry,
                                   DecodedClientMessageHandler clientMessageHandler,
                                   DeviceSessionManager sessionManager,
                                   CoapServer server) {
        super(id);
        this.protocol = protocol;
        this.supports = supports;
        this.registry = registry;
        this.server = server;
        this.helper = new DeviceGatewayHelper(registry, sessionManager, clientMessageHandler);
    }

    public Mono<ProtocolSupport> getProtocol() {
        return supports.getProtocol(protocol);
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
            .flatMap(message -> {
                log.debug("handle coap msg: {}", message.payloadAsString());

                AtomicReference<DeviceSession> sessionRef = new AtomicReference<>();
                sessionRef.set(new CoapDeviceSession(null, server, getTransport(), monitor));

                return getProtocol()
                    .filter(coap -> isStarted())
                    .flatMap(pt -> pt.getMessageCodec(getTransport()))
                    .flatMapMany(codec -> codec.decode(
                        FromDeviceMessageContext.of(sessionRef.get(), message, registry)
                    ))
                    .cast(DeviceMessage.class)
                    .onErrorResume(error -> {
                        log.error("device msg cast error: ", error);
                        return Mono.empty();
                    })
                    .flatMap(deviceMessage -> {
                        monitor.receivedMessage();
                        return helper
                            .handleDeviceMessage(
                                deviceMessage,
                                device -> new CoapDeviceSession(device, server, getTransport(), monitor),
                                sessionRef::set,
                                () -> log.warn("The device[{}] in the message body does not exist:{}", deviceMessage.getDeviceId(), deviceMessage)
                            )
                            .thenReturn(deviceMessage);
                    });
            })
            .subscribe();
    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.CoAP;
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.COAP_SERVER;
    }
}
