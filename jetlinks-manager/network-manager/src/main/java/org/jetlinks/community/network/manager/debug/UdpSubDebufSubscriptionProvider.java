package org.jetlinks.community.network.manager.debug;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.udp.local.UdpLocal;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class UdpSubDebufSubscriptionProvider implements SubscriptionProvider {

    private final NetworkManager networkManager;

    public UdpSubDebufSubscriptionProvider(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public String id() {
        return "udp-debug-subscribe";
    }

    @Override
    public String name() {
        return "UDP订阅调试";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/network/udp/*/_subscribe"
        };
    }

    @Override
    public Flux<String> subscribe(SubscribeRequest request) {
        String id = request.getTopic().split("[/]")[3];
        return subscribe(id);
    }

    public Flux<String> subscribe(String id) {

        return Flux.create(sink ->
            sink.onDispose(networkManager
                .<UdpLocal>getNetwork(DefaultNetworkType.UDP, id)
                .flatMap(local -> local
                    .handleMessage()
                    .flatMap(udpMessage -> Mono.just(udpMessage.payloadAsString()))
                    .doOnNext(sink::next)
                    .then()
                )
                .doOnError(sink::error)
                .doOnSubscribe(sub -> log.debug("start udp subscribe[{}] debug", id))
                .doOnCancel(() -> log.debug("stop udp subscribe[{}] debug", id))
                .subscribe()
            ));
    }
}
