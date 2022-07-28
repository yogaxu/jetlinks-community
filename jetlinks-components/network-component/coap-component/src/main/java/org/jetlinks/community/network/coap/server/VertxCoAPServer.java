package org.jetlinks.community.network.coap.server;

import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;

public class VertxCoAPServer implements CoAPServer {
    @Override
    public String getId() {
        return null;
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.COAP_SERVER;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public boolean isAlive() {
        return false;
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }
}
