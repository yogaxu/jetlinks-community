package org.jetlinks.community.network.coap.server;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.MessageDeliverer;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.network.*;
import org.jetlinks.community.network.security.CertificateManager;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetSocketAddress;

@Component
@Slf4j
public class CoapServerProvider implements NetworkProvider<CoapServerProperties> {

    private final CertificateManager certificateManager;

    public CoapServerProvider(CertificateManager certificateManager) {
        this.certificateManager = certificateManager;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.COAP_SERVER;
    }

    @Nonnull
    @Override
    public Network createNetwork(@Nonnull CoapServerProperties properties) {
        CoapServerImpl server = new CoapServerImpl(properties.getId());
        initServer(server, properties);
        return server;
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull CoapServerProperties properties) {
        CoapServerImpl server = (CoapServerImpl) network;
        server.shutdown();
        initServer(server, properties);
    }

    private void initServer(CoapServerImpl server, CoapServerProperties properties) {
        CoapServer coapServer = new CoapServer();
        coapServer.addEndpoint(CoapEndpoint
            .builder()
            .setInetSocketAddress(new InetSocketAddress(properties.getAddress(), properties.getPort()))
            .build());

        server.setServer(coapServer);
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return null;
    }

    @Nonnull
    @Override
    public Mono<CoapServerProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            CoapServerProperties config = FastBeanCopier.copy(properties.getConfigurations(), new CoapServerProperties());
            config.setId(properties.getId());
            return Mono.just(config);
        });
    }
}
