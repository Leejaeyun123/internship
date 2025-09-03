// ChatRoomController.java (수정)
// ...
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import java.io.IOException;

public class ChatRoomController {

    @FXML private TextArea chatArea;
    @FXML private TextField messageField;
    @FXML private ListView<String> userList; // ✅ 접속자 목록 UI
    
    private ChatClient client;
    private ObservableList<String> usersObservableList;

    // ✅ 초기화 메서드 (FXML 로드 시 자동 호출)
    @FXML
    public void initialize() {
        usersObservableList = FXCollections.observableArrayList();
        userList.setItems(usersObservableList);
    }

    // ChatClient에서 호출되어 UI에 메시지를 표시하는 메서드
    public void displayMessage(String message) {
        // ✅ 메시지 유형에 따라 분기 처리
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

    // ✅ 접속자 목록 업데이트 메서드
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