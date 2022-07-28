package org.jetlinks.community.network.http.client;

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
public class HttpClientProperties implements ValueObject {

    private String id;

    private HttpClientOptions options;

    private Map<String, Object> parserConfiguration = new HashMap<>();

    private String baseUrl;

    // 验证HOST
    private boolean verifyHost;

    // 开启SSL
    private boolean ssl;

    // 证书
    private String certId;

    // 信任所有证书
    private boolean trustAll;

    @Override
    public Map<String, Object> values() {
        return parserConfiguration;
    }
}
