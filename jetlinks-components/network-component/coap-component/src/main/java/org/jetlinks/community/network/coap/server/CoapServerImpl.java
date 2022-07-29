package org.jetlinks.community.network.coap.server;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.MessageDeliverer;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.core.message.codec.CoapExchangeMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Slf4j
public class CoapServerImpl implements CoapServer {

    @Getter
    private final String id;

    private final Sinks.Many<CoapExchangeMessage> processor = Sinks
        .many()
        .multicast()
        .onBackpressureBuffer(Integer.MAX_VALUE);

    private org.eclipse.californium.core.CoapServer server;

    public CoapServerImpl(String id) {
        this.id = id;
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.COAP_SERVER;
    }

    @Override
    public void shutdown() {
        if (server != null) {
            server.destroy();
        }
        server = null;
    }

    @Override
    public boolean isAlive() {
        return server != null;
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }

    public void setServer(org.eclipse.californium.core.CoapServer server) {
        if (this.server != null) {
            shutdown();
        }
        this.server = server;

        server.setMessageDeliverer(new MessageDeliverer() {
            @Override
            public void deliverRequest(Exchange exchange) {
                String path = exchange.getRequest().getOptions().getUriPathString();
                processor.tryEmitNext(new CoapExchangeMessage(new CoapExchange(exchange, new CoapResource(path))));
            }

            @Override
            public void deliverResponse(Exchange exchange, Response response) {
                response.setPayload("ok");
                exchange.sendResponse(response);
            }
        });
        server.start();
    }

    @Override
    public Flux<CoapExchangeMessage> handleMessage() {
        return processor.asFlux();
    }

    @Override
    public Mono<Boolean> send(CoapExchangeMessage message) {
        return null;
    }
}
