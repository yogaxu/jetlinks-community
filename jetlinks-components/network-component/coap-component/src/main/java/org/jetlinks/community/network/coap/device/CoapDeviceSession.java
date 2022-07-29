package org.jetlinks.community.network.coap.device;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.network.coap.server.CoapServer;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class CoapDeviceSession implements DeviceSession {

    @Getter
    @Setter
    private DeviceOperator operator;

    @Setter
    private CoapServer server;

    @Getter
    private final Transport transport;

    private long lastPingTime = System.currentTimeMillis();

    @Setter
    private long keepAliveTimeoutMs = Duration.ofMinutes(1).toMillis();

    private final long connectTime = System.currentTimeMillis();

    private final DeviceGatewayMonitor monitor;

    public CoapDeviceSession(DeviceOperator operator,
                             CoapServer server,
                             Transport transport,
                             DeviceGatewayMonitor monitor) {
        this.operator = operator;
        this.server = server;
        this.transport = transport;
        this.monitor = monitor;
    }

    @Override
    public String getId() {
        return getDeviceId();
    }

    @Override
    public String getDeviceId() {
        if (operator != null) {
            return operator.getDeviceId();
        }
        return "unknown";
    }

    @Override
    public long lastPingTime() {
        return lastPingTime;
    }

    @Override
    public long connectTime() {
        return connectTime;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        return null;
    }

    @Override
    public void close() {
        monitor.disconnected();
    }

    @Override
    public void ping() {
        lastPingTime = System.currentTimeMillis();
    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        this.keepAliveTimeoutMs = timeout.toMillis();
    }

    @Override
    public boolean isAlive() {
        return keepAliveTimeoutMs < 0 || System.currentTimeMillis() - lastPingTime < keepAliveTimeoutMs;
    }

    @Override
    public void onClose(Runnable call) {

    }
}
