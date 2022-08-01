package org.jetlinks.community.network.manager.debug;

import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.udp.UdpMessage;
import org.jetlinks.community.network.udp.local.UdpLocal;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class UdpSendDebugSubscriptionProvider implements SubscriptionProvider {

    private final NetworkManager networkManager;

    public UdpSendDebugSubscriptionProvider(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public String id() {
        return "udp-debug-send";
    }

    @Override
    public String name() {
        return "UDP推送调试";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/network/udp/*/_send"
        };
    }

    @Override
    public Flux<String> subscribe(SubscribeRequest request) {
        String id = request.getTopic().split("[/]")[3];
        return send(id, request);
    }

    public Flux<String> send(String id, SubscribeRequest request) {
        String message = request.getString("request")
            .orElseThrow(() -> new IllegalArgumentException("参数[request]不能为空"));

        return networkManager
            .<UdpLocal>getNetwork(DefaultNetworkType.UDP, id)
            .flatMap(local -> {
                UdpMessage udpMessage = UdpMessage.of(message);
                return local.send(udpMessage.getRemoteAddress(), udpMessage.getRemotePort(), udpMessage);
            })
            .thenReturn("推送成功")
            .flux();
    }
}
