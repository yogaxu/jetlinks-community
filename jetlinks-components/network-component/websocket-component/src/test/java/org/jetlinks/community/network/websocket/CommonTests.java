package org.jetlinks.community.network.websocket;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.message.codec.http.Header;
import org.junit.Test;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CommonTests {

    @Test
    public void patternTest() {
        AntPathMatcher matcher = new AntPathMatcher();
        log.debug("{}", matcher.match("/**", "/test"));
    }

    @Test
    public void listIndexOfTest() {
        Header header = new Header();
        header.setName("test");

        Header header1 = new Header();
        header1.setName("test");

        List<Header> headers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Header h = new Header();
            h.setName("test" + i);
            h.setValue(new String[]{"test-val" + i});
        }
        headers.add(header1);

        log.debug("index: {}", headers.indexOf(header));
    }
}
