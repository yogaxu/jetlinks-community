package org.jetlinks.community.network.coap.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.network.*;
import org.jetlinks.community.network.security.CertificateManager;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Component
@Slf4j
public class CoapClientProvider implements NetworkProvider<CoapClientProperties> {

    private final CertificateManager certificateManager;

    public CoapClientProvider(CertificateManager certificateManager) {
        this.certificateManager = certificateManager;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.COAP_CLIENT;
    }

    @Nonnull
    @Override
    public Network createNetwork(@Nonnull CoapClientProperties properties) {
        CoapClientImpl client = new CoapClientImpl(properties.getId());
        initClient(client, properties);
        return client;
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull CoapClientProperties properties) {
        CoapClientImpl client = (CoapClientImpl) network;
        initClient(client, properties);
    }

    private void initClient(CoapClientImpl client, CoapClientProperties properties) {
        CoapClient coapClient = new CoapClient()
            .setURI(properties.getUrl())
            .setTimeout(properties.getTimeout());
        client.setClient(coapClient);
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return null;
    }

    @Nonnull
    @Override
    public Mono<CoapClientProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            CoapClientProperties config = FastBeanCopier.copy(properties.getConfigurations(), new CoapClientProperties());
            config.setId(properties.getId());
            return Mono.just(config);
        });
    }
}
