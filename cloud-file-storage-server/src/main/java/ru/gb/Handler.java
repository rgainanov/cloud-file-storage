package ru.gb;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Slf4j
public class Handler implements Runnable {

    private static int counter = 0;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DataInputStream is;
    private final DataOutputStream os;

    private final String name;

    public Handler(Socket socket) throws IOException {
        this.is = new DataInputStream(socket.getInputStream());
        this.os = new DataOutputStream(socket.getOutputStream());
        counter++;
        name = "User#" + counter;
        log.debug("Set nick: {} for new client", name);
    }

    private String getDate() {
        return formatter.format(LocalDateTime.now());
    }

    @Override
    public void run() {
        try {
            while (true) {
//                String msg = is.readUTF();
//                log.debug("received: {}", msg);
//                String response = String.format("%s %s: %s", getDate(), name, msg);
//                log.debug("response msg generated: {}", response);
//                os.writeUTF(response);
//                log.debug("msg sent...");
//                os.flush();
                byte[] receiveBuffer = new byte[1024];
                is.read(receiveBuffer);

                FileObject receivedObject;
                ByteArrayInputStream bis = new ByteArrayInputStream(receiveBuffer);
                ObjectInput oin = new ObjectInputStream(bis);
                receivedObject = (FileObject) oin.readObject();
                Path serverDir = Paths.get(
                        "cloud-file-storage-server", "server-storage", receivedObject.getFileName()
                );
                int res = saveFile(receivedObject, serverDir);
                log.debug("file saved: {}", res == 1 ? "success" : "fail");
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

    private int saveFile(FileObject fb, Path savePath) throws IOException {

        if (!Files.exists(savePath)) {
            Files.createFile(savePath);
        }
        File destFile = new File(String.valueOf(savePath));
        File srcFile = fb.getFile();

        FileReader ins = null;
        FileWriter outs = null;

        try {
            ins = new FileReader(srcFile);
            outs = new FileWriter(destFile);
            int ch;
            while ((ch = ins.read()) != -1) {
                outs.write(ch);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            try {
                ins.close();
                outs.close();
            } catch (IOException e) {
            }
        }
        return 1;
    }
}
