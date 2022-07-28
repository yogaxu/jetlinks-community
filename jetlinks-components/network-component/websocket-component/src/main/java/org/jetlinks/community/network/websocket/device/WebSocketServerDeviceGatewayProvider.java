package org.jetlinks.community.network.websocket.device;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.supports.DeviceGatewayProperties;
import org.jetlinks.community.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.websocket.server.WebSocketServer;
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
@Slf4j
public class WebSocketServerDeviceGatewayProvider implements DeviceGatewayProvider {

    private final NetworkManager networkManager;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final DecodedClientMessageHandler messageHandler;

    private final ProtocolSupports protocolSupports;

    public WebSocketServerDeviceGatewayProvider(NetworkManager networkManager,
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
        return "websocket-server";
    }

    @Override
    public String getName() {
        return "WebSocket服务接入";
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.WEB_SOCKET_SERVER;
    }

    @Override
    public Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {
        return networkManager
            .<WebSocketServer>getNetwork(getNetworkType(), properties.getNetworkId())
            .map(server -> {
                ArrayList<Map<String, String>> routes = new ArrayList<>();

                if(properties.getConfiguration().get("routes") != null){
                    routes = (ArrayList<Map<String, String>>) properties.getConfiguration().get("routes");
                    routes.forEach(item -> {
                        Assert.hasText(item.get("protocol"), "protocol can not be empty");
                    });
                }

                return new WebSocketServerDeviceGateway(properties.getId(),
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
