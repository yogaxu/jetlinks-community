package org.jetlinks.community.network.udp.local;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class UdpLocalProviderTest {

//    static UdpLocal udpLocal;
//
//    @BeforeAll
//    static void init() {
//        UdpLocalProperties properties = UdpLocalProperties.builder()
//            .id("test")
//            .localPort(8181)
//            .options(new DatagramSocketOptions())
//            .build();
//        UdpLocalProvider provider = new UdpLocalProvider(Vertx.vertx());
//
//        udpLocal = provider.createNetwork(properties);
//    }
//
//    @Test
//    void test() {
//        DatagramSocket socket = Vertx.vertx().createDatagramSocket();
//        Buffer buffer = Buffer.buffer("test");
//        socket.send(buffer, 8181, "localhost", result -> {
//            log.info("send succ {}", result.succeeded());
//        });
//    }

    @Test
    public void urlTest() throws MalformedURLException {
        String parseUrl = "http://localhost:8181/test?key=000&val=111";
        URL url = new URL(parseUrl);
        log.debug("url: {}", url.toString());
        String parameters = parseUrl.substring(parseUrl.indexOf("?") + 1);
        parseUrl = parseUrl.substring(0, parseUrl.indexOf("?"));
        Map<String, String> map = Stream.of(parameters.split("[&]"))
            .map(str -> str.split("[=]", 2))
            .filter(arr -> arr.length > 1)
            .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1], (a, b) -> String.join(",", a, b)));
        log.debug("parseUrl: {}, map: {}",
            parseUrl,
            map
        );

        log.debug("protocol: {}, host: {}, port: {}, path: {}, query: {}",
            url.getProtocol(),
            url.getHost(),
            url.getPort(),
            url.getPath(),
            url.getQuery()
        );
    }
}
