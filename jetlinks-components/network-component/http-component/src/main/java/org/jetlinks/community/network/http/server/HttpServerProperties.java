package org.jetlinks.community.network.http.server;

import io.vertx.core.http.HttpServerOptions;
import lombok.*;
import org.jetlinks.community.ValueObject;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpServerProperties implements ValueObject {

    private String id;

    private HttpServerOptions options;

    private Map<String, Object> parserConfiguration = new HashMap<>();

    private int port;

    private boolean ssl;

    private int instance = Runtime.getRuntime().availableProcessors();

    private String certId;

    @Override
    public Map<String, Object> values() {
        return parserConfiguration;
    }
}
