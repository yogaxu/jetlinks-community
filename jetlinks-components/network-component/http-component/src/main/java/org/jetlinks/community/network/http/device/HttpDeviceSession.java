package org.jetlinks.community.network.http.device;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.network.http.server.HttpServer;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
public class HttpDeviceSession implements DeviceSession {

    @Getter
    @Setter
    private DeviceOperator operator;

    @Setter
    private HttpServer server;

    @Getter
    private final Transport transport;

    private long lastPingTime = System.currentTimeMillis();

    @Setter
    private long keepAliveTimeoutMs = Duration.ofMinutes(1).toMillis();

    private final long connectTime = System.currentTimeMillis();

    private final DeviceGatewayMonitor monitor;

    public HttpDeviceSession(DeviceOperator operator,
                             HttpServer server,
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
        monitor.sentMessage();
        return server.send(encodedMessage);
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
    public void onClose(Runnable runnable) {

    }
}
