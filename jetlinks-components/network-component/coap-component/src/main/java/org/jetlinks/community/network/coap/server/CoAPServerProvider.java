package org.jetlinks.community.network.coap.server;

import org.jetlinks.community.network.*;
import org.jetlinks.core.metadata.ConfigMetadata;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CoAPServerProvider implements NetworkProvider<CoAPServerProperties> {
    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.COAP_SERVER;
    }

    @Nonnull
    @Override
    public Network createNetwork(@Nonnull CoAPServerProperties properties) {
        return null;
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull CoAPServerProperties properties) {

    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return null;
    }

    @Nonnull
    @Override
    public Mono<CoAPServerProperties> createConfig(@Nonnull NetworkProperties properties) {
        return null;
    }
}
