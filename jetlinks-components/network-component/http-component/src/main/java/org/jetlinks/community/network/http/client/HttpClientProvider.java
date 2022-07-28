package org.jetlinks.community.network.http.client;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
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

@Component
@Slf4j
public class HttpClientProvider implements NetworkProvider<HttpClientProperties> {

    private final CertificateManager certificateManager;

    private final Vertx vertx;

    public HttpClientProvider(CertificateManager certificateManager, Vertx vertx) {
        this.certificateManager = certificateManager;
        this.vertx = vertx;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.HTTP_CLIENT;
    }

    @Nonnull
    @Override
    public Network createNetwork(@Nonnull HttpClientProperties properties) {
        VertxHttpClient client = new VertxHttpClient(properties.getId());

        initClient(client, properties);

        return client;
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull HttpClientProperties properties) {
        initClient((VertxHttpClient) network, properties);
    }

    private void initClient(VertxHttpClient client, HttpClientProperties properties) {
        HttpClient httpClient = vertx.createHttpClient(properties.getOptions());
        client.setClient(httpClient);
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return null;
    }

    @Nonnull
    @Override
    public Mono<HttpClientProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            HttpClientProperties config = FastBeanCopier.copy(properties.getConfigurations(), new HttpClientProperties());
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
