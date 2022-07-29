package org.jetlinks.community.network.manager.debug;

import org.eclipse.californium.core.Utils;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.coap.client.CoapClient;
import org.jetlinks.core.message.codec.DefaultCoapMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class CoapClientDebugSubscriptionProvider implements SubscriptionProvider {

    private final NetworkManager networkManager;

    public CoapClientDebugSubscriptionProvider(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public String id() {
        return "coap-client-debug";
    }

    @Override
    public String name() {
        return "COAP客户端调试";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/network/coap/client/*/_send"
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
            .<CoapClient>getNetwork(DefaultNetworkType.COAP_CLIENT, id)
            .flatMap(client -> client
                .send(DefaultCoapMessage.of(message))
                .flatMap(response -> Mono.just(Utils.prettyPrint(response)))
            )
            .flux();
    }
}
