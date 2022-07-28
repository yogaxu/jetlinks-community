package org.jetlinks.community.network.http.client;

import org.jetlinks.community.network.Network;
import org.jetlinks.community.network.http.HttpMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface HttpClient extends Network {

    Flux<HttpMessage> handleMessage();

    Mono<Boolean> send(HttpMessage message);

}
