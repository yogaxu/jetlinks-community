package org.jetlinks.community.network.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.message.codec.MessagePayloadType;
import org.jetlinks.core.message.codec.TextMessageParser;
import org.jetlinks.core.message.codec.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
@NoArgsConstructor
public class HttpMessage implements HttpExchangeMessage {

    private HttpServerRequest request;

    @Getter
    @Setter
    private String url;

    @Getter
    @Setter
    private HttpMethod method;

    @Getter
    @Setter
    private MediaType contentType;

    @Getter
    @Setter
    private ByteBuf payload;

    @Getter
    @Setter
    private List<Header> headers = new ArrayList<>();

    @Getter
    @Setter
    private Map<String, String> queryParameters = new HashMap<>();

    @Getter
    @Setter
    private String host;

    @Getter
    @Setter
    private int port;

    @Getter
    @Setter
    private String path;

    public HttpMessage(HttpServerRequest request, Buffer buffer) {
        this.request = request;
        this.url = request.path();
        this.method = HttpMethod.resolve(request.method().name());
        this.contentType = MediaType.valueOf(request.getHeader("Content-Type"));
        this.payload = buffer.getByteBuf();

        for (String headerName : request.headers().names()) {
            this.headers.add(new Header(headerName, new String[]{request.getHeader(headerName)}));
        }
        for (String paramName : request.params().names()) {
            this.queryParameters.put(paramName, request.getParam(paramName));
        }
    }

    @Nonnull
    @Override
    public Mono<Void> response(@Nonnull HttpResponseMessage message) {
        request
            .response()
            .send(Buffer.buffer(message.payloadAsBytes()), result -> {
                if (result.succeeded()) {
                    log.debug("http server debug response succeeded");
                } else {
                    log.error("http server debug response failed", result.cause());
                }
            });
        return Mono.empty();
    }

    public static HttpMessage of(String httpString) {
        HttpMessage request = new HttpMessage();
        HttpHeaders httpHeaders = new HttpHeaders();
        TextMessageParser.of(
            start -> {
                try {
                    String[] firstLine = start.split("[ ]");
                    request.setMethod(HttpMethod.resolve(firstLine[0]));

                    URL url = new URL(firstLine[1]);
                    request.setUrl(url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + url.getPath());
                    request.setHost(url.getHost());
                    request.setPort(url.getPort());
                    request.setPath(url.getPath());
                    if (url.getQuery() != null) {
                        request.setQueryParameters(
                            Stream.of(url.getQuery().split("[&]"))
                                .map(str -> str.split("[=]", 2))
                                .filter(arr -> arr.length > 1)
                                .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1], (a, b) -> String.join(",", a, b)))
                        );
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            },
            httpHeaders::add,
            body -> {
                request.setPayload(Unpooled.wrappedBuffer(body.getBody()));

                if (httpHeaders.getContentType() == null) {
                    if (body.getType() == MessagePayloadType.JSON) {
                        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    } else if (body.getType() == MessagePayloadType.STRING) {
                        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                    }
                }
                request.setContentType(httpHeaders.getContentType());
            },
            () -> {
                if (request.getContentType() == null) {
                    request.setContentType(httpHeaders.getContentType());
                }
                request.setPayload(Unpooled.EMPTY_BUFFER);
            }
        ).parse(httpString);

        request.setHeaders(httpHeaders.entrySet()
            .stream()
            .map(e -> new Header(e.getKey(), e.getValue().toArray(new String[0])))
            .collect(Collectors.toList()));

        return request;
    }
}
