package org.jetlinks.community.network.websocket.client;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocketBase;
import io.vertx.core.net.SocketAddress;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.http.websocket.DefaultWebSocketMessage;
import org.jetlinks.core.message.codec.http.websocket.WebSocketMessage;
import reactor.core.publisher.*;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class VertxWebSocketClient implements WebsocketClient {

    @Getter
    private final String id;

    private final List<Runnable> disconnectListener = new CopyOnWriteArrayList<>();

    private final Sinks.Many<WebSocketMessage> processor = Sinks
        .many()
        .multicast()
        .onBackpressureBuffer(Integer.MAX_VALUE);

    private final boolean serverClient;

    public volatile HttpClient client;

    public WebSocketBase socket;

    @Setter
    private long keepAliveTimeoutMs = Duration.ofMinutes(10).toMillis();

    private volatile long lastKeepAliveTime = System.currentTimeMillis();

    public VertxWebSocketClient(String id, boolean serverClient) {
        this.id = id;
        this.serverClient = serverClient;
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.WEB_SOCKET_CLIENT;
    }

    @Override
    public void shutdown() {
        log.debug("ws client [{}] disconnect", getId());
        synchronized (this) {
            if (null != client) {
                execute(client::close);
                client = null;
            }
            if (null != socket) {
                execute(socket::close);
                this.socket = null;
            }
        }
        for (Runnable runnable : disconnectListener) {
            execute(runnable);
        }
        disconnectListener.clear();
        if (serverClient) {
            processor.tryEmitComplete();
        }
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("close tcp client error", e);
        }
    }

    @Override
    public InetSocketAddress address() {
        return getRemoteAddress();
    }

    @Override
    public Mono<Void> sendMessage(EncodedMessage message) {
        return Mono.empty();
    }

    protected void received(WebSocketMessage message) {
        processor.tryEmitNext(message);
    }

    @Override
    public Flux<EncodedMessage> receiveMessage() {
        return this
            .subscribe()
            .cast(EncodedMessage.class);
    }

    @Override
    public void disconnect() {
        shutdown();
    }

    @Override
    public boolean isAlive() {
        return socket != null && (keepAliveTimeoutMs < 0 || System.currentTimeMillis() - lastKeepAliveTime < keepAliveTimeoutMs);
    }

    @Override
    public boolean isAutoReload() {
        return true;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        if (null == socket) {
            return null;
        }
        SocketAddress socketAddress = socket.remoteAddress();
        return new InetSocketAddress(socketAddress.host(), socketAddress.port());
    }

    @Override
    public Flux<WebSocketMessage> subscribe() {
        return processor.asFlux();
    }

    @Override
    public Mono<Boolean> send(WebSocketMessage message) {
        return Mono.create(sink -> {
            if (socket == null) {
                sink.error(new SocketException("socket closed"));
                return;
            }
            if (message.getType().equals(WebSocketMessage.Type.TEXT)) {
                socket.writeTextMessage(message.payloadAsString(), result -> {
                    keepAlive();
                    if (result.succeeded()) {
                        sink.success();
                    } else {
                        sink.error(result.cause());
                    }
                });
            } else {
                socket.writeBinaryMessage(Buffer.buffer(message.getPayload()), result -> {
                    keepAlive();
                    if (result.succeeded()) {
                        sink.success();
                    } else {
                        sink.error(result.cause());
                    }
                });
            }
        }).thenReturn(true);
    }

    @Override
    public void onDisconnect(Runnable disconnected) {
        disconnectListener.add(disconnected);
    }

    @Override
    public void keepAlive() {
        lastKeepAliveTime = System.currentTimeMillis();
    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        keepAliveTimeoutMs = timeout.toMillis();
    }

    @Override
    public void reset() {

    }

    @Override
    public String getWebSocketPath() {
        if (socket instanceof ServerWebSocket) {
            return ((ServerWebSocket) socket).path();
        }
        return null;
    }

    /**
     * socket处理
     *
     * @param socket socket
     */
    public void setSocket(WebSocketBase socket) {
        synchronized (this) {
            if (this.socket != null && this.socket != socket) {
                this.socket.close();
            }

            socket
                .closeHandler(v -> shutdown())
                .textMessageHandler(msg -> {
                    if (log.isDebugEnabled()) {
                        log.debug("handle ws client[{}] payload:[{}]", socket.remoteAddress(), msg);
                    }
                    keepAlive();
                    checkMemoryLeakClose(socket);
                    received(DefaultWebSocketMessage.of(WebSocketMessage.Type.TEXT, Buffer.buffer(msg).getByteBuf()));
                })
                .binaryMessageHandler(msg -> {
                    if (log.isDebugEnabled()) {
                        log.debug("handle ws client[{}] payload:[{}]", socket.remoteAddress(), Hex.encodeHexString(msg.getBytes()));
                    }
                    keepAlive();
                    checkMemoryLeakClose(socket);
                    received(DefaultWebSocketMessage.of(WebSocketMessage.Type.BINARY, msg.getByteBuf()));
                });

            this.socket = socket;
        }
    }

    private void checkMemoryLeakClose(WebSocketBase socket) {
        if (this.socket != socket) {
            log.warn("ws client [{}] memory leak ", socket.remoteAddress());
            socket.close();
        }
    }

    public void setClient(HttpClient client) {
        if (this.client != null && this.client != client) {
            this.client.close();
        }
        keepAlive();
        this.client = client;
    }
}
