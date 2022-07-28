package org.jetlinks.community.network.websocket.server;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.network.*;
import org.jetlinks.community.network.security.CertificateManager;
import org.jetlinks.community.network.security.VertxKeyCertTrustOptions;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class WebSocketServerProvider implements NetworkProvider<WebSocketServerProperties> {

    private final CertificateManager certificateManager;

    private final Vertx vertx;

    public WebSocketServerProvider(CertificateManager certificateManager, Vertx vertx) {
        this.certificateManager = certificateManager;
        this.vertx = vertx;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.WEB_SOCKET_SERVER;
    }

    @Nonnull
    @Override
    public Network createNetwork(@Nonnull WebSocketServerProperties properties) {
        VertxWebSocketServer server = new VertxWebSocketServer(properties.getId());
        initServer(server, properties);
        return server;
    }

    private void initServer(VertxWebSocketServer server, WebSocketServerProperties properties) {
        int instance = Math.max(2, properties.getInstance());
        List<HttpServer> instances = new ArrayList<>(instance);
        for (int i = 0; i < instance; i++) {
            instances.add(vertx.createHttpServer(properties.getOptions()));
        }
        server.setServer(instances);

        for (HttpServer httpServer : instances) {
            httpServer.listen(properties.createSocketAddress(), result -> {
                if (result.succeeded()) {
                    log.info("ws server startup on {}", result.result().actualPort());
                } else {
                    log.error("ws server startup error", result.cause());
                }
            });
        }
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull WebSocketServerProperties properties) {
        VertxWebSocketServer server = ((VertxWebSocketServer) network);
        server.shutdown();
        initServer(server, properties);
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return null;
    }

    @Nonnull
    @Override
    public Mono<WebSocketServerProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            WebSocketServerProperties config = FastBeanCopier.copy(properties.getConfigurations(), new WebSocketServerProperties());
            config.setId(properties.getId());
            if (config.getOptions() == null) {
                config.setOptions(new HttpServerOptions());
            }
            if (config.isSsl()) {
                config.getOptions().setSsl(true);
                return certificateManager.getCertificate(config.getCertId())
                    .map(VertxKeyCertTrustOptions::new)
                    .doOnNext(config.getOptions()::setKeyCertOptions)
                    .doOnNext(config.getOptions()::setTrustOptions)
                    .thenReturn(config);
            }
            return Mono.just(config);
        });
    }
}
