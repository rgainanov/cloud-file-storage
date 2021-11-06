package ru.gb.lesson2_nio;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;

import static java.nio.file.StandardWatchEventKinds.*;

public class NioExamples {
    public static void main(String[] args) throws IOException {
        // Path
        Path parent = Paths.get("");
        Path path = Paths.get("cloud-file-storage-server", "server", "root");
        for (Path p : path) {
            Path cur = parent.resolve(p);
            System.out.println(cur);
            if (!Files.exists(cur)) {
                Files.createDirectory(cur);
            }
            parent = cur;
        }
        System.out.println(path.getParent());
        System.out.println(path.toAbsolutePath());
        System.out.println(path.toAbsolutePath().getParent());

        System.out.println(path.getFileName().toString());
        System.out.println(path.getFileSystem());

//        WatchService watchService = path.getFileSystem().newWatchService();
//        WatchService watchService = FileSystems.getDefault().newWatchService();
//        path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
//        runAsync(watchService);


//        abcd
//        gt -> gtcd
        Path testTxt = path.resolve("test.txt");
//        Files.write(
//                testTxt,
//                "gt".getBytes(StandardCharsets.UTF_8),
//                StandardOpenOption.WRITE
//        );
//        Files.copy(
//                testTxt,
//                path.resolve("test-copy.txt"),
//                StandardCopyOption.REPLACE_EXISTING
//        );

        Path root = Paths.get("");
//        Files.walkFileTree(root, new HashSet<>(), 2, new SimpleFileVisitor<Path>() {
//            @Override
//            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                System.out.println(file);
//                return super.visitFile(file, attrs);
//            }
//        });
//        Files.walk(root, 2).forEach(System.out::println);

    }

    private static void runAsync(WatchService watchService) {
        new Thread(() -> {
            System.out.println("WatchService started listening ...");
            try {
                while (true) {
                    WatchKey watchKey = watchService.take();
                    List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
                    for (WatchEvent<?> watchEvent : watchEvents) {
                        System.out.println(watchEvent.kind() + " " + watchEvent.context());
                    }
                    watchKey.reset();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).run();
    }
}
