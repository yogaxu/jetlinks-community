package org.jetlinks.community.network.udp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.TextMessageParser;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UdpMessage implements EncodedMessage {

    private ByteBuf payload;

    private String remoteAddress;

    private int remotePort;

    public static UdpMessage of(String udpString) {
        UdpMessage udpMessage = new UdpMessage();
        TextMessageParser.of(
            start -> {
                String[] firstLine = start.split("[:]");
                udpMessage.setRemoteAddress(firstLine[0]);
                udpMessage.setRemotePort(Integer.parseInt(firstLine[1]));
            },
            (key, value) -> {
            },
            body -> udpMessage.setPayload(Unpooled.wrappedBuffer(body.getBody())),
            () -> udpMessage.setPayload(Unpooled.EMPTY_BUFFER)
        ).parse(udpString);

        return udpMessage;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        ByteBufUtil.appendPrettyHexDump(builder, payload);

        return builder.toString();
    }
}
