package ru.gb;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j
public class MainController implements Initializable {

    public ListView<String> clientView;
    public ListView<String> serverView;
    public TextField input;
    private Path clientDir;
    private DataInputStream is;
    private DataOutputStream os;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {

            clientDir = Paths.get("cloud-file-storage-client", "client-storage");
            if (!Files.exists(clientDir)) {
                Files.createDirectory(clientDir);
            }

            clientView.getItems().clear();
            clientView.getItems().addAll(getFiles(clientDir));

            clientView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    String item = clientView.getSelectionModel().getSelectedItem();
                    input.setText(item);
                }
            });

            Socket socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());

            Thread readThread = new Thread(this::read);
            readThread.setDaemon(true);
            readThread.start();

        } catch (Exception e) {
            log.error("", e);
        }
    }

    private List<String> getFiles(Path path) throws IOException {
        return Files.list(path).map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
    }

    private void read() {
        try {
            while (true) {
                String msg = is.readUTF();
                log.debug("msg received: {}", msg);
                Platform.runLater(() -> clientView.getItems().add(msg));
            }
        } catch (Exception e) {
            log.error("", e);
        }

    }

    public void sendMessage(ActionEvent actionEvent) throws IOException {
        String text = input.getText();
//        os.writeUTF(text);
        os.write(getFileObject(text));
        os.flush();
        input.clear();
    }

    private byte[] getFileObject(String fileName) throws IOException {
        FileObject file = new FileObject(
                fileName, new File(String.valueOf(Paths.get(String.valueOf(clientDir), fileName)))
        );
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput outputObj = new ObjectOutputStream(bos);
        outputObj.writeObject(file);
        outputObj.flush();
        outputObj.close();
        return bos.toByteArray();
    }

}
