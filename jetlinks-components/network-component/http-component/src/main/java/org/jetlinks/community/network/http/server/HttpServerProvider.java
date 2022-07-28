package org.jetlinks.community.network.http.server;

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
public class HttpServerProvider implements NetworkProvider<HttpServerProperties> {

    private final CertificateManager certificateManager;

    private final Vertx vertx;

    public HttpServerProvider(CertificateManager certificateManager, Vertx vertx) {
        this.certificateManager = certificateManager;
        this.vertx = vertx;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.HTTP_SERVER;
    }

    @Nonnull
    @Override
    public Network createNetwork(@Nonnull HttpServerProperties properties) {
        VertxHttpServer server = new VertxHttpServer(properties.getId());
        initServer(server, properties);
        return server;
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull HttpServerProperties properties) {
        VertxHttpServer server = (VertxHttpServer) network;
        server.shutdown();
        initServer(server, properties);
    }

    private void initServer(VertxHttpServer server, HttpServerProperties properties) {
        int instance = Math.max(2, properties.getInstance());
        List<HttpServer> instances = new ArrayList<>(instance);
        for (int i = 0; i < instance; i++) {
            instances.add(vertx.createHttpServer(properties.getOptions()));
        }
        server.setServer(instances);
        for (HttpServer httpServer : instances) {
            httpServer.listen(properties.getPort(), result -> {
                if (result.succeeded()) {
                    log.debug("http server startup on {}", result.result().actualPort());
                } else {
                    log.error("http server startup error", result.cause());
                }
            });
        }
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return null;
    }

    @Nonnull
    @Override
    public Mono<HttpServerProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            HttpServerProperties config = FastBeanCopier.copy(properties.getConfigurations(), new HttpServerProperties());
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
