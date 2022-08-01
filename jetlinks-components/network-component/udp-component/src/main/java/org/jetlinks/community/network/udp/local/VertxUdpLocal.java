package org.jetlinks.community.network.udp.local;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.udp.UdpMessage;
import org.jetlinks.core.message.codec.EncodedMessage;
import reactor.core.publisher.*;

/**
 * 基于Vertx扩展的UDP组件
 *
 * @author yoga_xu
 */
@Slf4j
public class VertxUdpLocal implements UdpLocal {

    @Getter
    private final String id;

    private final Sinks.Many<UdpMessage> processor = Sinks
        .many()
        .multicast()
        .onBackpressureBuffer(Integer.MAX_VALUE);

    DatagramSocket socket;

    public VertxUdpLocal(String id) {
        this.id = id;
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.UDP;
    }

    @Override
    public void shutdown() {
        if (null != socket) {
            execute(socket::close);
            socket = null;
        }
    }

    @Override
    public boolean isAlive() {
        return socket != null;
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("close udp error", e);
        }
    }

    public void setSocket(DatagramSocket socket) {
        if (this.socket != null) {
            shutdown();
        }
        this.socket = socket;

        socket.handler(packet -> processor
            .tryEmitNext(
                new UdpMessage(packet.data().getByteBuf(), packet.sender().host(), packet.sender().port())
            )
        );
    }

    @Override
    public Flux<UdpMessage> handleMessage() {
        return processor.asFlux();
    }

    @Override
    public Mono<Boolean> send(String remoteAddress, int remotePort, EncodedMessage encodedMessage) {
        return Mono
            .create((sink) -> {
                Buffer buffer = Buffer.buffer(encodedMessage.getPayload());

                socket.send(buffer, remotePort, remoteAddress, result -> {
//                    keepAlive();
                    if (result.succeeded()) {
                        sink.success();
                    } else {
                        sink.error(result.cause());
                    }
                });
            })
            .thenReturn(true);
    }
}
