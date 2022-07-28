package org.jetlinks.community.network.http.device;

import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.supports.DeviceGatewayProperties;
import org.jetlinks.community.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.http.server.HttpServer;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Map;

@Component
public class HttpServerDeviceGatewayProvider implements DeviceGatewayProvider {

    private final NetworkManager networkManager;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final DecodedClientMessageHandler messageHandler;

    private final ProtocolSupports protocolSupports;

    public HttpServerDeviceGatewayProvider(NetworkManager networkManager,
                                           DeviceRegistry registry,
                                           DeviceSessionManager sessionManager,
                                           DecodedClientMessageHandler messageHandler,
                                           ProtocolSupports protocolSupports) {
        this.networkManager = networkManager;
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.messageHandler = messageHandler;
        this.protocolSupports = protocolSupports;
    }

    @Override
    public String getId() {
        return "http-server-gateway";
    }

    @Override
    public String getName() {
        return "HTTP 推送接入";
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.HTTP_SERVER;
    }

    @Override
    public Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {
        return networkManager
            .<HttpServer>getNetwork(getNetworkType(), properties.getNetworkId())
            .map(server -> {
                ArrayList<Map<String, String>> routes = new ArrayList<>();

                if(properties.getConfiguration().get("routes") != null){
                    routes = (ArrayList<Map<String, String>>) properties.getConfiguration().get("routes");
                    routes.forEach(item -> {
                        Assert.hasText(item.get("protocol"), "protocol can not be empty");
                    });
                }

                return new HttpServerDeviceGateway(properties.getId(),
                    routes,
                    protocolSupports,
                    registry,
                    messageHandler,
                    sessionManager,
                    server
                );
            });
    }
}
