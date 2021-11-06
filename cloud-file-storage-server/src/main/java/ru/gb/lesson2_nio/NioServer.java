package ru.gb.lesson2_nio;

import com.sun.tools.javac.comp.Todo;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Slf4j
public class NioServer {

    private ByteBuffer buf;
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;

    public NioServer() {
        buf = ByteBuffer.allocate(10);
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(8189));
            serverSocketChannel.configureBlocking(false);
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            log.debug("Server started ...");
            while (serverSocketChannel.isOpen()) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                for (SelectionKey key : keys) {
                    if (key.isAcceptable()) {
                        handleAccept(key);
                    }
                    if (key.isReadable()) {
                        handleRead(key);
                    }
                }
                keys.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        StringBuilder sb = new StringBuilder("From server: ");
        while (true) {
            int read = channel.read(buf);
            if (read == -1) {
                channel.close();
                return;
            }
            if (read == 0) {
                break;
            }
            if (read > 0) {
                buf.flip();
                while (buf.hasRemaining()) {
                    sb.append((char) buf.get());
                }
                buf.clear();
            }
        }
        //Todo add ls, cat, cd, mkdir, touch commands
        log.debug("Received: {}", sb);
        channel.write(ByteBuffer.wrap(sb.toString().getBytes(StandardCharsets.UTF_8)));
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel channel = serverSocketChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        // Todo send welcome message
        log.debug("Client connected ...");
    }

    public static void main(String[] args) {
        new NioServer();
    }
}
