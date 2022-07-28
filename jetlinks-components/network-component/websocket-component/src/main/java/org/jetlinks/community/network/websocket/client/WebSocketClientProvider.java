package org.jetlinks.community.network.websocket.client;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocketConnectOptions;
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
import java.time.Duration;

@Component
@Slf4j
public class WebSocketClientProvider implements NetworkProvider<WebSocketClientProperties> {

    private final CertificateManager certificateManager;

    private final Vertx vertx;

    public WebSocketClientProvider(CertificateManager certificateManager, Vertx vertx) {
        this.certificateManager = certificateManager;
        this.vertx = vertx;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.WEB_SOCKET_CLIENT;
    }

    @Nonnull
    @Override
    public Network createNetwork(@Nonnull WebSocketClientProperties properties) {
        VertxWebSocketClient client = new VertxWebSocketClient(properties.getId(), false);

        initClient(client, properties);

        return client;
    }

    private void initClient(VertxWebSocketClient client, WebSocketClientProperties properties) {
        HttpClient httpClient = vertx.createHttpClient(properties.getOptions());
        client.setClient(httpClient);
        client.setKeepAliveTimeoutMs(properties.getLong("keepAliveTimeout").orElse(Duration.ofMinutes(10).toMillis()));
        WebSocketConnectOptions options = new WebSocketConnectOptions()
            .setHost(properties.getHost())
            .setPort(properties.getPort())
            .setURI(properties.getUri());
        httpClient.webSocket(options, result -> {
            if (result.succeeded()) {
                client.setSocket(result.result());
                log.info("ws client startup on {}", result.result().localAddress());
            } else {
                log.error("ws client startup error", result.cause());
            }
        });
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull WebSocketClientProperties properties) {
        initClient((VertxWebSocketClient) network, properties);
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return null;
    }

    @Nonnull
    @Override
    public Mono<WebSocketClientProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            WebSocketClientProperties config = FastBeanCopier.copy(properties.getConfigurations(), new WebSocketClientProperties());
            config.setId(properties.getId());
            if (config.getOptions() == null) {
                config.setOptions(new HttpClientOptions());
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
