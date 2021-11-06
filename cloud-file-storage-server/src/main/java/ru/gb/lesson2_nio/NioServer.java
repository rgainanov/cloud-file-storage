package ru.gb.lesson2_nio;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class NioServer {

    private ByteBuffer buf;
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private Path path = Paths.get("");
    private Path previousPath = Paths.get("");

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
        StringBuilder sb = new StringBuilder();
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
        handleCommands(sb.toString().trim(), channel);
        log.debug("Received: {}", sb.toString().trim());
        String pwd = path.toString().equals("") ? "" : "/" + path;
        channel.write(ByteBuffer.wrap((" ~" + pwd + " > ").getBytes(StandardCharsets.UTF_8)));
    }

    private void handleCommands(String command, SocketChannel channel) throws IOException {
        String[] parsedCommand = command.split("\\s");
        String cmd = parsedCommand[0];
        String arg = "";
        String name;
        switch (cmd) {
            case "ls":
                getDirectoryList(channel);
                log.debug("Directory listed ...");
                break;
            case "cd":
                name = parsedCommand[1];
                changeDirectory(name);
                log.debug("Directory changed ...");
                break;
            case "cat":
                arg = parsedCommand[1];
                catCommand(arg, channel);
                log.debug("File displayed ...");
                break;
            case "mkdir":
                if (parsedCommand.length == 3) {
                    arg = parsedCommand[1];
                    name = parsedCommand[2];
                } else {
                    name = parsedCommand[1];
                }
                makeDirectory(name, arg, channel);
                log.debug("Directory created ...");
                break;
            case "touch":
                name = parsedCommand[1];
                createFile(name, channel);
                log.debug("File created ...");
                break;
            case "rm":
                if (parsedCommand.length == 3) {
                    arg = parsedCommand[1];
                    name = parsedCommand[2];
                } else {
                    name = parsedCommand[1];
                }
                remove(name, arg, channel);
                log.debug("Directory/file removed ...");
                break;
            case "":
                break;
            default:
                channel.write(ByteBuffer.wrap(("shell: command not found: " + cmd + "\n").getBytes(StandardCharsets.UTF_8)));
        }

    }

    private void remove(String name, String arg, SocketChannel channel) throws IOException {
        if (arg.equals("-rf")) {
            Files.walk(path.resolve(name))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } else {
            try {
                if (!Files.deleteIfExists(path.resolve(name))) {
                    channel.write(ByteBuffer.wrap(("shell: failed to delete\n").getBytes(StandardCharsets.UTF_8)));
                }
            } catch (DirectoryNotEmptyException e) {
                channel.write(ByteBuffer.wrap(("shell: directory is not empty\n").getBytes(StandardCharsets.UTF_8)));
            }
        }
    }

    private void createFile(String arg, SocketChannel channel) throws IOException {
        if (Files.exists(path.resolve(arg))) {
            channel.write(ByteBuffer.wrap(("shell: file already exists\n").getBytes(StandardCharsets.UTF_8)));
        } else {
            Files.createFile(path.resolve(arg));
        }
    }

    private void makeDirectory(String fileName, String arg, SocketChannel channel) throws IOException {
        Path newDir = path.resolve(fileName);
        if (fileName.contains(".")) {
            channel.write(ByteBuffer.wrap(("shell: invalid directory\n").getBytes(StandardCharsets.UTF_8)));
        } else if (arg.equals("-p")) {
            if (!Files.exists(newDir)) {
                Files.createDirectories(newDir);
            }
        } else {
            try {
                if (!Files.exists(newDir)) {
                    Files.createDirectory(newDir);
                }
            } catch (NoSuchFileException e) {
                channel.write(ByteBuffer.wrap(("shell: no such file or directory\n").getBytes(StandardCharsets.UTF_8)));
            }
        }
    }

    private void catCommand(String arg, SocketChannel channel) throws IOException {
        if (Files.isDirectory(path.resolve(arg))) {
            channel.write(ByteBuffer.wrap("shell: cannot read directory, please select file\n".getBytes(StandardCharsets.UTF_8)));
        } else if (!Files.exists(path.resolve(arg))) {
            channel.write(ByteBuffer.wrap("shell: file doesn't exists\n".getBytes(StandardCharsets.UTF_8)));
        } else {
            channel.write(ByteBuffer.wrap((readFile(path.resolve(arg)) + "\n").getBytes(StandardCharsets.UTF_8)));
        }
    }

    private void changeDirectory(String name) {
        if (!name.equals(".")) {
            if (name.equals("..")) {
                if (path.getParent() == null) {
                    path = Paths.get("");
                } else if (!path.toString().equals("")) {
                    path = path.getParent();
                }
            } else if (Files.exists(path.resolve(name))) {
                path = path.resolve(name);
            }
        }
    }

    private void getDirectoryList(SocketChannel channel) throws IOException {
        Files.walkFileTree(path, new HashSet<>(), 1, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                BasicFileAttributeView basicView =
                        Files.getFileAttributeView(file, BasicFileAttributeView.class);
                BasicFileAttributes basicAttribs = basicView.readAttributes();
                String fileType = Files.isDirectory(file) ? "directory" : "file";
                String f = String.format(" %-12s  %-10s  %-20s  %-50s  %n", fileType, basicAttribs.size() + "B", basicAttribs.lastModifiedTime(), file.getFileName());
                channel.write(ByteBuffer.wrap(f.getBytes(StandardCharsets.UTF_8)));
                return super.visitFile(file, attrs);
            }
        });
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel channel = serverSocketChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        Path banner = Paths.get("cloud-file-storage-server", "server", "root", "banner.txt");
        channel.write(ByteBuffer.wrap(readFile(banner).getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap(" ~ > ".getBytes(StandardCharsets.UTF_8)));
        log.debug("Client connected ...");
    }


    private String readFile(Path path) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        SeekableByteChannel seekableByteChannel = Files.newByteChannel(path);

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
        return new String(result, StandardCharsets.UTF_8);
    }


    public static void main(String[] args) {
        new NioServer();
    }
}
