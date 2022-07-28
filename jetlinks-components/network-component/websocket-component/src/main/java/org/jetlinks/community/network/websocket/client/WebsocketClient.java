package org.jetlinks.community.network.websocket.client;

import org.jetlinks.community.network.Network;
import org.jetlinks.core.message.codec.http.websocket.WebSocketMessage;
import org.jetlinks.core.server.ClientConnection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;

public interface WebsocketClient extends Network, ClientConnection {
    InetSocketAddress getRemoteAddress();

    Flux<WebSocketMessage> subscribe();

    Mono<Boolean> send(WebSocketMessage message);

    void onDisconnect(Runnable disconnected);

    void keepAlive();

    void setKeepAliveTimeout(Duration timeout);

    void reset();

    String getWebSocketPath();
}
