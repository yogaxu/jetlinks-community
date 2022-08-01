package org.jetlinks.community.network.udp.local;

import io.vertx.core.datagram.DatagramSocketOptions;
import lombok.*;
import org.jetlinks.community.ValueObject;

import java.util.HashMap;
import java.util.Map;

/**
 * UDP组件配置
 *
 * @author yoga_xu
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UdpLocalProperties implements ValueObject {

    private String id;

    // 其他UDP配置, 详见vertx文档
    private DatagramSocketOptions options;

    private Map<String, Object> parserConfiguration = new HashMap<>();

    // 本地地址
    private String localAddress;

    // 本地端口
    private int localPort;

    // 远程地址
    private String remoteAddress;

    // 远程端口
    private int remotePort;

    // 是否开启DTLS
    private boolean ssl;

    // 证书
    private String certId;

    // 私钥别名
    private String privateKeyAlias;

    @Override
    public Map<String, Object> values() {
        return parserConfiguration;
    }
}
