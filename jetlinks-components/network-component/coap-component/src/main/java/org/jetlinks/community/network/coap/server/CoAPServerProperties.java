package org.jetlinks.community.network.coap.server;

import lombok.*;
import org.jetlinks.community.ValueObject;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoapServerProperties implements ValueObject {

    private String id;

    private Map<String, Object> parserConfiguration = new HashMap<>();

    private String address;

    private int port;

    private boolean enableDtls;

    private String privateKeyAlias;

    private String certId;

    @Override
    public Map<String, Object> values() {
        return parserConfiguration;
    }
}
