package org.jetlinks.community.network.manager.debug;

import io.vertx.core.buffer.Buffer;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.websocket.server.WebSocketServer;
import org.jetlinks.core.message.codec.http.websocket.DefaultWebSocketMessage;
import org.jetlinks.core.message.codec.http.websocket.WebSocketMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@Slf4j
public class WebSocketServerDebugSubscriptionProvider implements SubscriptionProvider {

    private final NetworkManager networkManager;

    public WebSocketServerDebugSubscriptionProvider(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public String id() {
        return "websocket-server-debug";
    }

    @Override
    public String name() {
        return "WebSocket服务调试";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/network/websocket/server/*/_subscribe"
        };
    }

    @Override
    public Flux<String> subscribe(SubscribeRequest request) {
        String id = request.getTopic().split("[/]")[4];
        return subscribe(id, request);
    }

    public Flux<String> subscribe(String id, SubscribeRequest request) {
        String message = request.getString("response")
            .orElseThrow(() -> new IllegalArgumentException("参数[response]不能为空"));

        return Flux.create(sink ->
            sink.onDispose(networkManager
                .<WebSocketServer>getNetwork(DefaultNetworkType.WEB_SOCKET_SERVER, id)
                .flatMap(server -> server
                    .handleConnection()
                    .flatMap(client -> client
                        .subscribe()
                        .flatMap(webSocketMessage -> client
                            .send(DefaultWebSocketMessage.of(WebSocketMessage.Type.BINARY, Buffer.buffer(message).getByteBuf()))
                            .thenReturn(webSocketMessage.payloadAsString())
                        )
                    )
                    .doOnNext(sink::next)
                    .then()
                )
                .doOnError(sink::error)
                .doOnSubscribe(sub -> log.debug("start ws server[{}] debug", id))
                .doOnCancel(() -> log.debug("stop ws server[{}] debug", id))
                .subscribe()
            ));
    }
}
