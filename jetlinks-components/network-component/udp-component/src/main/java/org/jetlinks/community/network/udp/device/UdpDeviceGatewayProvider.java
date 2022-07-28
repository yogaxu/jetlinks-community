package org.jetlinks.community.network.udp.device;

import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.supports.DeviceGatewayProperties;
import org.jetlinks.community.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.udp.local.UdpLocal;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

/**
 * UDP设备网关提供商
 *
 * @author yoga_xu
 */
@Component
public class UdpDeviceGatewayProvider implements DeviceGatewayProvider {
    private final NetworkManager networkManager;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final DecodedClientMessageHandler messageHandler;

    private final ProtocolSupports protocolSupports;

    public UdpDeviceGatewayProvider(NetworkManager networkManager,
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
        return "udp-device-gateway";
    }

    @Override
    public String getName() {
        return "UDP 接入";
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.UDP;
    }

    @Override
    public Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {
        return networkManager
            .<UdpLocal>getNetwork(getNetworkType(), properties.getNetworkId())
            .map(udpLocal -> {
                String protocol = (String) properties.getConfiguration().get("protocol");

                Assert.hasText(protocol, "protocol can not be empty");

                return new UdpDeviceGateway(properties.getId(),
                    protocol,
                    protocolSupports,
                    registry,
                    messageHandler,
                    sessionManager,
                    udpLocal);
            });
    }
}
