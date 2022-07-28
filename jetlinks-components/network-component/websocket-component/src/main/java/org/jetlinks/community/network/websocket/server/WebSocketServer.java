package org.jetlinks.community.network.websocket.server;

import org.jetlinks.community.network.Network;
import org.jetlinks.community.network.websocket.client.WebsocketClient;
import reactor.core.publisher.Flux;

public interface WebSocketServer extends Network {

    Flux<WebsocketClient> handleConnection();

}
