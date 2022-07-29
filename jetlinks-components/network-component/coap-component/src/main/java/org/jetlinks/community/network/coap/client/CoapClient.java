package org.jetlinks.community.network.coap.client;

import org.eclipse.californium.core.CoapResponse;
import org.jetlinks.community.network.Network;
import org.jetlinks.core.message.codec.CoapMessage;
import reactor.core.publisher.Mono;

public interface CoapClient extends Network {

    /**
     * 客户端(调试器)发送消息
     * @param message 请求消息
     * @return
     */
    Mono<CoapResponse> send(CoapMessage message);

}
