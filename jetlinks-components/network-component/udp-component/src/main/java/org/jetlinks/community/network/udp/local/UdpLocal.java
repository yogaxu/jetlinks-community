package org.jetlinks.community.network.udp.local;

import org.jetlinks.community.network.Network;
import org.jetlinks.community.network.udp.UdpMessage;
import org.jetlinks.core.message.codec.EncodedMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * UDP组件
 *
 * @author yoga_xu
 */
public interface UdpLocal extends Network {
    Flux<UdpMessage> handleMessage();

    Mono<Boolean> send(String remoteAddress, int remotePort, EncodedMessage encodedMessage);
}
