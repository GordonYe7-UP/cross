package com.jumper.port.proxy;

import com.jumper.port.DisplayMsg;
import com.jumper.port.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;


public class ProxyWrite {
    private ByteBuffer buffer;

    public ProxyWrite(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public void handleWrite(SelectionKey key, int count) throws IOException {
        key.interestOps(SelectionKey.OP_WRITE);
        buffer.flip();
        SocketChannel channel = (SocketChannel) key.channel();
        while (buffer.hasRemaining()) {
            channel.write(buffer);
            Utils.put(new DisplayMsg("message send to：", channel.getRemoteAddress() + " " + key.attachment() + " " + count + " bytes"));
        }
        buffer.clear();
        key.interestOps(SelectionKey.OP_READ);
    }
}
