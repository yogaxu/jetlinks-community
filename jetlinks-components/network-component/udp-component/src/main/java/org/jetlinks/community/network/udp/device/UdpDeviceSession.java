package org.jetlinks.community.network.udp.device;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.network.udp.local.UdpLocal;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * UDP设备会话
 *
 * @author yoga_xu
 */
public class UdpDeviceSession implements DeviceSession {

    @Getter
    @Setter
    private DeviceOperator operator;

    @Setter
    private UdpLocal udpLocal;

    @Getter
    private final Transport transport;

    private long lastPingTime = System.currentTimeMillis();

    @Setter
    private long keepAliveTimeoutMs = Duration.ofMinutes(1).toMillis();

    private final long connectTime = System.currentTimeMillis();

    private final DeviceGatewayMonitor monitor;

    public UdpDeviceSession(DeviceOperator operator,
                            UdpLocal udpLocal,
                            Transport transport,
                            DeviceGatewayMonitor monitor) {
        this.operator = operator;
        this.udpLocal = udpLocal;
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
        monitor.sentMessage();
        return udpLocal.send(encodedMessage);
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
