package org.jetlinks.community.network.udp.device;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.gateway.AbstractDeviceGateway;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.udp.UdpMessage;
import org.jetlinks.community.network.udp.local.UdpLocal;
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


/**
 * UDP设备网关
 *
 * @author yoga_xu
 */
@Slf4j
public class UdpDeviceGateway extends AbstractDeviceGateway {

    private final UdpLocal udpLocal;

    private final String protocol;

    private final ProtocolSupports supports;

    private final DeviceRegistry registry;

    private Disposable disposable;

    private final DeviceGatewayHelper helper;

    public UdpDeviceGateway(String id,
                            String protocol,
                            ProtocolSupports supports,
                            DeviceRegistry registry,
                            DecodedClientMessageHandler clientMessageHandler,
                            DeviceSessionManager sessionManager,
                            UdpLocal udpLocal) {
        super(id);
        this.protocol = protocol;
        this.supports = supports;
        this.registry = registry;
        this.udpLocal = udpLocal;
        this.helper = new DeviceGatewayHelper(registry, sessionManager, clientMessageHandler);
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

    @Override
    public Transport getTransport() {
        return DefaultTransport.UDP;
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.UDP;
    }

    @Override
    public boolean isStarted() {
        return super.isStarted();
    }

    public Mono<ProtocolSupport> getProtocol() {
        return supports.getProtocol(protocol);
    }

    private void doStart() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = udpLocal
            .handleMessage()
            .subscribeOn(Schedulers.parallel())
            .filter(udp -> isStarted())
            .flatMap(message -> {
                AtomicReference<DeviceSession> sessionRef = new AtomicReference<>();
                sessionRef.set(new UdpDeviceSession(null, udpLocal, getTransport(), monitor));

                return getProtocol()
                    .flatMap(pt -> pt.getMessageCodec(getTransport()))
                    .flatMapMany(codec -> codec.decode(
                        FromDeviceMessageContext.of(sessionRef.get(), message, registry)
                    ))
                    .cast(DeviceMessage.class)
                    .flatMap(deviceMessage -> {
                        monitor.receivedMessage();
                        return helper
                            .handleDeviceMessage(
                                deviceMessage,
                                device -> new UdpDeviceSession(device, udpLocal, getTransport(), monitor),
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
}