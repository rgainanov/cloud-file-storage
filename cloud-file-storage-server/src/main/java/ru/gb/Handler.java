package ru.gb;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
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
                String msg = is.readUTF();
                log.debug("received: {}", msg);
                String response = String.format("%s %s: %s", getDate(), name, msg);
                log.debug("response msg generated: {}", response);
                os.writeUTF(response);
                log.debug("msg sent...");
                os.flush();
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
