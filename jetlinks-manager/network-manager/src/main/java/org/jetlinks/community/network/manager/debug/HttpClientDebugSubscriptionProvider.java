package org.jetlinks.community.network.manager.debug;

import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.http.HttpMessage;
import org.jetlinks.community.network.http.client.HttpClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class HttpClientDebugSubscriptionProvider implements SubscriptionProvider {

    private final NetworkManager networkManager;

    public HttpClientDebugSubscriptionProvider(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public String id() {
        return "network-http-client-debug";
    }

    @Override
    public String name() {
        return "HTTP客户端调试";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/network/http/client/*/_send"
        };
    }

    @Override
    public Flux<String> subscribe(SubscribeRequest request) {
        String id = request.getTopic().split("[/]")[4];
        return send(id, request);
    }

    public Flux<String> send(String id, SubscribeRequest request) {
        String message = request.getString("request")
            .orElseThrow(() -> new IllegalArgumentException("参数[request]不能为空"));

        return networkManager
            .<HttpClient>getNetwork(DefaultNetworkType.HTTP_CLIENT, id)
            .flatMap(client -> client.send(HttpMessage.of(message)))
            .thenReturn("推送成功")
            .flux();
    }
}
