package org.jetlinks.community.network.websocket.client;

import io.vertx.core.http.HttpClientOptions;
import lombok.*;
import org.jetlinks.community.ValueObject;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketClientProperties implements ValueObject {

    private String id;

    private HttpClientOptions options;

    private Map<String, Object> parserConfiguration = new HashMap<>();

    private String host;

    private int port;

    private String uri;

    private boolean verifyHost;

    private boolean ssl;

    private String certId;

    @Override
    public Map<String, Object> values() {
        return parserConfiguration;
    }
}
