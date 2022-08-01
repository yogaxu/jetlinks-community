package org.jetlinks.community.network.manager.debug;

import io.vertx.core.buffer.Buffer;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.websocket.client.WebsocketClient;
import org.jetlinks.core.message.codec.http.websocket.DefaultWebSocketMessage;
import org.jetlinks.core.message.codec.http.websocket.WebSocketMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class WebSocketClientDebugSubscriptionProvider implements SubscriptionProvider {

    private final NetworkManager networkManager;

    public WebSocketClientDebugSubscriptionProvider(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public String id() {
        return "websocket-client-debug";
    }

    @Override
    public String name() {
        return "WebSocket客户端调试";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/network/websocket/client/*/_subscribe",
            "/network/websocket/client/*/_publish/*"
        };
    }

    @Override
    public Flux<String> subscribe(SubscribeRequest request) {
        String id = request.getTopic().split("[/]")[4];
        if (request.getTopic().endsWith("_subscribe")) {
            return subscribe(id);
        } else {
            return publish(id, request);
        }
    }

    public Flux<String> publish(String id, SubscribeRequest request) {
        String message = request.getString("request")
            .orElseThrow(() -> new IllegalArgumentException("参数[request]不能为空"));

        String type = request.getTopic().split("[/]")[6];

        return networkManager
            .<WebsocketClient>getNetwork(DefaultNetworkType.WEB_SOCKET_CLIENT, id)
            .flatMapMany(client -> client
                .send(
                    DefaultWebSocketMessage.of(
                        type.equals("JSON") || type.equals("STRING") ? WebSocketMessage.Type.TEXT : WebSocketMessage.Type.BINARY, Buffer.buffer(message).getByteBuf()
                    )
                )
                .thenReturn("推送成功")
            );
    }

    public Flux<String> subscribe(String id) {
        return networkManager
            .<WebsocketClient>getNetwork(DefaultNetworkType.WEB_SOCKET_CLIENT, id)
            .flatMapMany(client -> client
                .subscribe()
                .map(WebSocketMessage::payloadAsString)
            );
    }
}
