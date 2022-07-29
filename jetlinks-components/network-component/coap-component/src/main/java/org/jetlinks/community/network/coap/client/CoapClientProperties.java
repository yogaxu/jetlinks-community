package org.jetlinks.community.network.coap.client;

import lombok.*;
import org.jetlinks.community.ValueObject;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoapClientProperties implements ValueObject {

    private String id;

    private Map<String, Object> parserConfiguration = new HashMap<>();

    private String url;

    // 超时时间 ms
    private long timeout;

    private boolean enableDtls;

    private String certId;

    @Override
    public Map<String, Object> values() {
        return parserConfiguration;
    }
}
