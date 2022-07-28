package org.jetlinks.community.network.http.server;

import org.jetlinks.community.network.Network;
import org.jetlinks.community.network.http.HttpMessage;
import org.jetlinks.core.message.codec.EncodedMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface HttpServer extends Network {

    Flux<HttpMessage> handleMessage();

    Mono<Boolean> send(EncodedMessage encodedMessage);

}
