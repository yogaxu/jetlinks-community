package org.jetlinks.community.network.coap.client;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.core.message.codec.CoapMessage;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Slf4j
public class CoapClientImpl implements CoapClient {

    @Getter
    private final String id;

    private org.eclipse.californium.core.CoapClient client;

    public CoapClientImpl(String id) {
        this.id = id;
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.COAP_CLIENT;
    }

    @Override
    public void shutdown() {
        if (client != null) {
            client.shutdown();
        }
        client = null;
    }

    @Override
    public boolean isAlive() {
        return client != null;
    }

    @Override
    public boolean isAutoReload() {
        return true;
    }

    @Override
    public Mono<CoapResponse> send(CoapMessage message) {
        return Mono.create(sink -> {
            Request request = null;
            switch(message.getCode()){
                case GET:
                    request = Request.newGet();
                    break;

                case POST:
                    request = Request.newPost();
                    request.setPayload(message.payloadAsBytes());
                    break;

                case PUT:
                    request = Request.newPut();
                    request.setPayload(message.payloadAsBytes());
                    break;

                case DELETE:
                    request = Request.newDelete();
                    break;

                default:
                    break;
            }

            if(request != null){
                if(StringUtils.hasText(message.getPath())){
                    request.setURI(message.getPath());
                }

                try {
                    sink.success(client.advanced(request));
                } catch (ConnectorException | IOException e) {
                    sink.error(e);
                }
            }
            sink.success(null);
        });
    }

    public void setClient(org.eclipse.californium.core.CoapClient client) {
        if(this.client != null){
            shutdown();
        }
        this.client = client;
    }
}
