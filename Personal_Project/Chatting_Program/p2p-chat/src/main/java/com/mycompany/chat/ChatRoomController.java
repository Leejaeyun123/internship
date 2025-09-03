package com.mycompany.chat;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import java.io.IOException;

public class ChatRoomController {

    @FXML private TextArea chatArea;
    @FXML private TextField messageField;
    @FXML private ListView<String> userList;
    @FXML private Label userLabel;

    private ChatClient client;
    private ObservableList<String> usersObservableList;

    @FXML
    public void initialize() {
        usersObservableList = FXCollections.observableArrayList();
        userList.setItems(usersObservableList);
    }

    public void displayMessage(String message) {
        if (message.startsWith("chat:")) {
            chatArea.appendText(message.substring("chat:".length()) + "\n");
        } else if (message.startsWith("system:")) {
            chatArea.appendText("== " + message.substring("system:".length()) + " ==\n");
        } else if (message.startsWith("userlist:")) {
            updateUserList(message.substring("userlist:".length()));
        } else {
            chatArea.appendText(message + "\n");
        }
    }

    private void updateUserList(String userListString) {
        usersObservableList.clear();
        String[] users = userListString.split(",");
        for (String user : users) {
            usersObservableList.add(user);
        }
    }

    @FXML
    private void sendMessage() {
        String message = messageField.getText();
        if (!message.isEmpty()) {
            client.sendMessage(message);
            messageField.clear();
        }
    }

    public void setClient(ChatClient client) {
        this.client = client;
    }
}