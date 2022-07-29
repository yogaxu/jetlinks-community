package org.jetlinks.community.network.manager.debug;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.coap.server.CoapServer;
import org.jetlinks.core.message.codec.DefaultCoapResponseMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class CoapServerDebugSubscriptionProvider implements SubscriptionProvider {

    private final NetworkManager networkManager;

    public CoapServerDebugSubscriptionProvider(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public String id() {
        return "coap-server-debug";
    }

    @Override
    public String name() {
        return "COAP服务调试";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/network/coap/server/*/_subscribe"
        };
    }

    @Override
    public Flux<String> subscribe(SubscribeRequest request) {
        String id = request.getTopic().split("[/]")[4];
        return subscribe(id, request);
    }

    public Flux<String> subscribe(String id, SubscribeRequest request) {
        String message = request.getString("request")
            .orElseThrow(() -> new IllegalArgumentException("参数[response]不能为空"));

        return Flux.create(sink -> sink
            .onDispose(networkManager
                .<CoapServer>getNetwork(DefaultNetworkType.COAP_SERVER, id)
                .flatMap(server -> server
                    .handleMessage()
                    .flatMap(coapMessage -> Mono
                        .create(sk -> coapMessage
                            .response(DefaultCoapResponseMessage.of(message))
                        )
                        .thenReturn("响应成功")
                    )
                    .doOnNext(sink::next)
                    .then()
                )
                .doOnError(sink::error)
                .doOnSubscribe(sub -> log.debug("start coap server[{}] debug", id))
                .doOnCancel(() -> log.debug("stop coap server[{}] debug", id))
                .subscribe()
            ));
    }
}
