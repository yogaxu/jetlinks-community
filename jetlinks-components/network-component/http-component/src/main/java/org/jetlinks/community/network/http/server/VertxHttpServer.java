package org.jetlinks.community.network.http.server;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.http.HttpMessage;
import org.jetlinks.core.message.codec.EncodedMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Collection;
import java.util.List;

@Slf4j
public class VertxHttpServer implements HttpServer {

    @Getter
    private final String id;

    private final Sinks.Many<HttpMessage> processor = Sinks
        .many()
        .multicast()
        .onBackpressureBuffer(Integer.MAX_VALUE);

    private Collection<io.vertx.core.http.HttpServer> servers;

    public VertxHttpServer(String id) {
        this.id = id;
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.HTTP_SERVER;
    }

    @Override
    public void shutdown() {
        if (null != servers) {
            for (io.vertx.core.http.HttpServer server : servers) {
                execute(server::close);
            }
            servers = null;
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
        return servers != null;
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }

    @Override
    public Flux<HttpMessage> handleMessage() {
        return processor.asFlux();
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        return Mono.empty().thenReturn(true);
    }

    public void setServer(List<io.vertx.core.http.HttpServer> servers) {
        if (this.servers != null && !this.servers.isEmpty()) {
            shutdown();
        }

        this.servers = servers;
        for (io.vertx.core.http.HttpServer server : servers) {
            server
                .exceptionHandler(error -> log.error(error.getMessage(), error))
                .requestHandler(request -> {
                    request.handler(buffer -> {
                        log.debug("handle http request from [{}]", request.remoteAddress());
                        processor.tryEmitNext(new HttpMessage(request, buffer));
                    });
                });
        }
    }
}
