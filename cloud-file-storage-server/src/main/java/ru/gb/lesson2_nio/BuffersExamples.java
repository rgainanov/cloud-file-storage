package ru.gb.lesson2_nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class BuffersExamples {
    public static void main(String[] args) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(5);

        byte ch = 'a';
        for (int i = 0; i < 3; i++) {
            buffer.put((byte) (ch + i));
        }

        buffer.flip();

        while (buffer.hasRemaining()) {
            System.out.println((char) buffer.get());
        }

        buffer.clear();

        Path text = Paths.get("cloud-file-storage-server", "server", "root", "test.txt");
        SeekableByteChannel seekableByteChannel = Files.newByteChannel(text);
        System.out.println(seekableByteChannel);

        byte[] result = new byte[(int) seekableByteChannel.size()];
        int pos = 0;
        while (true) {
            int read = seekableByteChannel.read(buffer);
            if (read <= 0) {
                break;
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                result[pos] = buffer.get();
                pos++;
            }
            buffer.clear();
        }
        System.out.println(new String(result, StandardCharsets.UTF_8));

        Path copy = Paths.get("cloud-file-storage-server", "server", "root", "test-copy.txt");
        SeekableByteChannel copySeekableByteChannel = Files.newByteChannel(copy, StandardOpenOption.WRITE);

        buffer.clear();

        byte[] bytes = "Приветствуем вас в нашей системе!".getBytes(StandardCharsets.UTF_8);

        for (int i = 0; i < bytes.length; i++) {
            buffer.put(bytes[i]);
            if (i % 5 == 0) {
                buffer.flip();
                copySeekableByteChannel.write(buffer);
                buffer.clear();
            }
            if (i == bytes.length - 1 && (i + 1) % 5 != 0) {
                buffer.flip();
                copySeekableByteChannel.write(buffer);
            }
        }

//        for (byte b : bytes) {
//            buffer.put(b);
//            if (buffer.position() == 5) {
//                buffer.flip();
//                copySeekableByteChannel.write(buffer);
//                buffer.clear();
//            }
//        }

    }
}
