package org.jetlinks.community.network.websocket.server;

import io.vertx.core.http.HttpServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.websocket.client.VertxWebSocketClient;
import org.jetlinks.community.network.websocket.client.WebsocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Collection;

@Slf4j
public class VertxWebSocketServer implements WebSocketServer {

    @Getter
    private final String id;

    private final Sinks.Many<WebsocketClient> processor = Sinks
        .many()
        .multicast()
        .onBackpressureBuffer(Integer.MAX_VALUE);

    private Collection<HttpServer> servers;

    public VertxWebSocketServer(String id) {
        this.id = id;
    }

    public void setServer(Collection<HttpServer> servers) {
        if (this.servers != null && !this.servers.isEmpty()) {
            shutdown();
        }

        this.servers = servers;
        for (HttpServer server : servers) {
            server
                .exceptionHandler(error -> log.error(error.getMessage(), error))
                .webSocketHandler(socket -> {
                    // 下游未订阅当前流
                    if (processor.currentSubscriberCount() <= 0) {
                        log.warn("not handler for ws client[{}]", socket.remoteAddress());
                        socket.close();
                        return;
                    }

                    VertxWebSocketClient client = new VertxWebSocketClient(id + "_" + socket.remoteAddress(), true);
                    client.setSocket(socket);

                    socket
                        .exceptionHandler(error -> log.error("ws client [{}] error", socket.remoteAddress(), error))
                        .closeHandler(nil -> log.debug("ws client [{}] closed", socket.remoteAddress()));

                    client.setSocket(socket);
                    processor.tryEmitNext(client);
                });
        }
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.WEB_SOCKET_SERVER;
    }

    @Override
    public void shutdown() {
        if (null != servers) {
            for (HttpServer server : servers) {
                execute(server::close);
            }
            servers = null;
        }
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("close ws server error", e);
        }
    }

    @Override
    public boolean isAlive() {
        return servers != null;
    }

    @Override
    public boolean isAutoReload() {
        return true;
    }

    @Override
    public Flux<WebsocketClient> handleConnection() {
        return processor.asFlux();
    }
}
