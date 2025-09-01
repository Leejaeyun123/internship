// 채팅방 화면의 UI를 제어하고, 사용자의 입력에 따라 메시지를 보내는 역할을 함

package com.mycompany.chat;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import java.io.IOException;

public class ChatRoomController {

    @FXML
    private TextArea chatArea;
    @FXML
    private TextField messageField;

    private ChatClient client;

    // 로그인 성공 후 클라이언트를 초기화하기 위해 호출되는 메소드
    public void setClient(ChatClient client) {
        this.client = client;
    }

    // UI에 메시지를 표시하는 메소드 (ChatClient에서 호출됨)
    public void displayMessage(String message) {
        chatArea.appendText(message + "\n");
    }

    // 사용자가 "전송" 버튼을 클릭하면 호출되는 메소드
    @FXML
    private void sendMessage() {
        String message = messageField.getText();
        if (!message.isEmpty()) {
            client.sendMessage(message); // 메시지 전송을 ChatClient에 위임
            messageField.clear(); // 메시지 입력 필드 초기화
        }
    }
}