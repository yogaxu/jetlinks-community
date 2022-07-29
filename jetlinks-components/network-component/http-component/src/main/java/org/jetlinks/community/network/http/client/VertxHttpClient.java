package org.jetlinks.community.network.http.client;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.RequestOptions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.http.HttpMessage;
import reactor.core.publisher.Mono;

@Slf4j
public class VertxHttpClient implements HttpClient {

    @Getter
    private final String id;

    private io.vertx.core.http.HttpClient client;

    public VertxHttpClient(String id) {
        this.id = id;
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.HTTP_CLIENT;
    }

    @Override
    public void shutdown() {
        if (null != client) {
            execute(client::close);
            client = null;
        }
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("close http server error", e);
        }
    }

    @Override
    public boolean isAlive() {
        return client != null;
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }

    @Override
    public Mono<Boolean> send(HttpMessage message) {
        return Mono.create(sink -> {
            RequestOptions options = new RequestOptions()
                .setHost(message.getHost())
                .setPort(message.getPort())
                .setURI(message.getPath());
            client.request(options, result -> {
                if (result.succeeded()) {
                    HttpClientRequest request = result.result();
                    request.putHeader("Content-Type", message.getContentType().toString());
                    request.send(Buffer.buffer(message.getPayload()), sendResult -> {
                        if (sendResult.succeeded()) {
                            log.debug("http client debug send succeeded, response status code: {}", sendResult.result().statusCode());
                            sink.success();
                        } else {
                            sink.error(sendResult.cause());
                        }
                    });
                } else {
                    sink.error(result.cause());
                }
            });
        });
    }

    public void setClient(io.vertx.core.http.HttpClient client) {
        if (this.client != null) {
            shutdown();
        }
        this.client = client;
    }
}
