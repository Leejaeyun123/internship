package com.mycompany.chat;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;

public class ChatRoomController {

    @FXML private TextArea chatArea;
    @FXML private TextField messageField;
    @FXML private ListView<String> userList;
    @FXML private Label userLabel;
    @FXML private ComboBox<String> statusCombo;

    private ChatClient client;
    private ObservableList<String> usersObservableList;

    @FXML
    public void initialize() {
        usersObservableList = FXCollections.observableArrayList();
        userList.setItems(usersObservableList);

        // 상태 옵션 세팅
        if (statusCombo != null) {
            statusCombo.setItems(FXCollections.observableArrayList("활동 중", "자리 비움"));
            statusCombo.getSelectionModel().select("활동 중"); // 기본값
        }
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
        if (userListString.isBlank()) return;
        String[] entries = userListString.split(",");
        for (String entry : entries) {
            String[] parts = entry.split("\\|", 2); // "닉네임|상태"
            String nick = parts[0];
            String status = (parts.length > 1 && !parts[1].isBlank()) ? parts[1] : "활동 중";
            usersObservableList.add(nick + " (" + status + ")");
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

    // 콤보박스에서 상태 변경 시 호출
    @FXML
    private void changeStatus() {
        if (client == null || statusCombo == null) return;
        String status = statusCombo.getValue();
        if (status != null && !status.isBlank()) {
            client.sendStatus(status);
        }
    }

    public void setClient(ChatClient client) {
        this.client = client;
        // 초기 상태도 서버에 알려주고 싶으면 주석 해제
        // if (statusCombo != null) client.sendStatus(statusCombo.getValue());
    }
}
