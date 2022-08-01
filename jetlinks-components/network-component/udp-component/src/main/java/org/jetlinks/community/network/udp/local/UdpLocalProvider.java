package org.jetlinks.community.network.udp.local;

import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.network.*;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * UDP组件提供商
 *
 * @author yoga_xu
 */
@Component
@Slf4j
public class UdpLocalProvider implements NetworkProvider<UdpLocalProperties> {

    private final Vertx vertx;

    public UdpLocalProvider(Vertx vertx) {
        this.vertx = vertx;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.UDP;
    }

    /**
     * UDP组件创建
     *
     * @param properties 配置信息
     */
    @Nonnull
    @Override
    public VertxUdpLocal createNetwork(@Nonnull UdpLocalProperties properties) {
        VertxUdpLocal udpLocal = new VertxUdpLocal(properties.getId());
        initUdpLocal(udpLocal, properties);
        return udpLocal;
    }

    /**
     * UDP组件初始化
     *
     * @param udpLocal   UDP单元
     * @param properties UDP配置
     */
    private void initUdpLocal(VertxUdpLocal udpLocal, UdpLocalProperties properties) {
        DatagramSocket socket = vertx.createDatagramSocket(properties.getOptions());
        udpLocal.setSocket(socket);
        socket.listen(properties.getLocalPort(), properties.getLocalAddress(), result -> {
            if (result.succeeded()) {
                log.info("udp local startup on {}", result.result().localAddress().port());
            } else {
                log.error("udp local startup error", result.cause());
            }
        });
    }

    /**
     * UDP组件重载
     *
     * @param network    网络组件
     * @param properties 配置信息
     */
    @Override
    public void reload(@Nonnull Network network, @Nonnull UdpLocalProperties properties) {
        VertxUdpLocal udpLocal = ((VertxUdpLocal) network);
        udpLocal.shutdown();
        initUdpLocal(udpLocal, properties);
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return null;
    }

    /**
     * UDP配置构建
     *
     * @param properties 原始配置信息
     */
    @Nonnull
    @Override
    public Mono<UdpLocalProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            UdpLocalProperties config = FastBeanCopier.copy(properties.getConfigurations(), new UdpLocalProperties());
            config.setId(properties.getId());
            if (config.getOptions() == null) {
                config.setOptions(new DatagramSocketOptions());
            }
            return Mono.just(config);
        });
    }
}
