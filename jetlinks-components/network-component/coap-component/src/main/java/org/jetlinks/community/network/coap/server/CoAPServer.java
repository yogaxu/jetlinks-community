package org.jetlinks.community.network.coap.server;

import org.jetlinks.community.network.Network;
import org.jetlinks.core.message.codec.CoapExchangeMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CoapServer extends Network {

    Flux<CoapExchangeMessage> handleMessage();

    Mono<Boolean> send(CoapExchangeMessage message);

}
