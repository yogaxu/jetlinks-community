package org.jetlinks.community.network.websocket.server;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SocketAddress;
import lombok.*;
import org.jetlinks.community.ValueObject;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketServerProperties implements ValueObject {

    private String id;

    private HttpServerOptions options;

    private Map<String, Object> parserConfiguration = new HashMap<>();

    private String host;

    private int port;

    private boolean ssl;

    private int instance = Runtime.getRuntime().availableProcessors();

    private String certId;

    public SocketAddress createSocketAddress() {
        if (!StringUtils.hasText(host)) {
            host = "localhost";
        }
        return SocketAddress.inetSocketAddress(port, host);
    }

    @Override
    public Map<String, Object> values() {
        return parserConfiguration;
    }
}
