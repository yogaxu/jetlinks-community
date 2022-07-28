package org.jetlinks.community.network.udp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.message.codec.EncodedMessage;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UdpMessage implements EncodedMessage {

    private ByteBuf payload;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        ByteBufUtil.appendPrettyHexDump(builder, payload);

        return builder.toString();
    }
}
