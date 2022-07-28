package org.jetlinks.community.network.manager.debug;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.http.server.HttpServer;
import org.jetlinks.core.message.codec.http.SimpleHttpResponseMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class HttpServerDebugSubscriptionProvider implements SubscriptionProvider {

    private final NetworkManager networkManager;

    public HttpServerDebugSubscriptionProvider(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public String id() {
        return "network-http-server-debug";
    }

    @Override
    public String name() {
        return "HTTP服务调试";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/network/http/server/*/_subscribe"
        };
    }

    @Override
    public Flux<String> subscribe(SubscribeRequest request) {
        return subscribe(request.getTopic().split("[/]")[4], request);
    }

    public Flux<String> subscribe(String id, SubscribeRequest request) {
        String message = request.getString("response")
            .orElseThrow(() -> new IllegalArgumentException("参数[response]不能为空"));

        return Flux.create(sink ->
            sink.onDispose(networkManager
                .<HttpServer>getNetwork(DefaultNetworkType.HTTP_SERVER, id)
                .flatMap(server -> server
                    .handleMessage()
                    .flatMap(httpMessage -> httpMessage
                        .response(SimpleHttpResponseMessage.of(message))
                        .thenReturn("响应成功")
                    )
                    .doOnNext(sink::next)
                    .then()
                )
                .doOnError(sink::error)
                .doOnSubscribe(sub -> log.debug("start http server[{}] debug", id))
                .doOnCancel(() -> log.debug("stop http server[{}] debug", id))
                .subscribe()
            ));
    }
}
