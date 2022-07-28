package org.jetlinks.community.network.websocket.device;

import lombok.Getter;
import org.jetlinks.community.network.websocket.client.WebsocketClient;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.codec.http.websocket.DefaultWebSocketMessage;
import org.jetlinks.core.message.codec.http.websocket.WebSocketMessage;
import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

public class UnknownWebSocketDeviceSession implements DeviceSession {

    @Getter
    private final String id;

    private final WebsocketClient client;

    @Getter
    private final Transport transport;

    private long lastPingTime = System.currentTimeMillis();

    private final long connectTime = System.currentTimeMillis();

    UnknownWebSocketDeviceSession(String id, WebsocketClient client, Transport transport) {
        this.id = id;
        this.client = client;
        this.transport = transport;
    }

    @Override
    public String getDeviceId() {
        return "unknown";
    }

    @Override
    public DeviceOperator getOperator() {
        return null;
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
        return client.send(DefaultWebSocketMessage.of(WebSocketMessage.Type.TEXT, encodedMessage.getPayload()));
    }

    @Override
    public void close() {
        client.shutdown();
    }

    @Override
    public void ping() {
        lastPingTime = System.currentTimeMillis();
        client.keepAlive();
    }

    @Override
    public boolean isAlive() {
        return client.isAlive();
    }

    @Override
    public void onClose(Runnable call) {
        client.onDisconnect(call);
    }

}
