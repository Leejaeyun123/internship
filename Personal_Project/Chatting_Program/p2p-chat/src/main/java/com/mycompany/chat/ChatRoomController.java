package com.mycompany.chat;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;

import java.util.*;

public class ChatRoomController {

    @FXML private TextArea chatArea;
    @FXML private TextField messageField;
    @FXML private ListView<String> userList;
    @FXML private Label userLabel;

    // 상태 콤보
    @FXML private ComboBox<String> statusCombo;

    // 방 UI (옵션; FXML에 없으면 null로 들어옴)
    @FXML private ComboBox<String> roomCombo;
    @FXML private TextField newRoomField;

    private ChatClient client;
    private ObservableList<String> usersObservableList;
    private ObservableList<String> roomsObservableList;

    // ✅ 방별 로그 저장
    private final Map<String, StringBuilder> roomLogs = new HashMap<>();
    // ✅ 현재 방 & 루프 방지 플래그
    private String currentRoom = null;
    private boolean suppressRoomComboEvent = false;

    @FXML
    public void initialize() {
        if (chatArea != null) chatArea.setWrapText(true);

        usersObservableList = FXCollections.observableArrayList();
        if (userList != null) userList.setItems(usersObservableList);

        if (statusCombo != null) {
            statusCombo.setItems(FXCollections.observableArrayList("활동 중", "자리 비움"));
            statusCombo.getSelectionModel().select("활동 중");
        }

        if (roomCombo != null) {
            roomsObservableList = FXCollections.observableArrayList();
            roomCombo.setItems(roomsObservableList);

            // ✅ 사용자가 직접 선택했을 때만 서버에 join 전송
            roomCombo.setOnAction(e -> {
                if (suppressRoomComboEvent) return;
                String r = roomCombo.getValue();
                if (r == null || r.isBlank()) return;
                if (r.equals(currentRoom)) return;                 // 같은 방이면 전송 X
                if (client != null) client.sendMessage("room:join:" + r.trim());
            });
        }
    }

    /** 서버에서 오는 프로토콜 처리 */
    public void displayMessage(String message) {
        if (message.startsWith("chat:")) {
            String line = message.substring("chat:".length()) + "\n";
            appendToCurrentRoom(line);

        } else if (message.startsWith("system:")) {
            String line = "== " + message.substring("system:".length()) + " ==\n";
            appendToCurrentRoom(line);

        } else if (message.startsWith("userlist:")) {
            updateUserList(message.substring("userlist:".length()));

        } else if (message.startsWith("roomlist:")) {
            updateRoomList(message.substring("roomlist:".length()));

        } else if (message.startsWith("roomchanged:")) {
            String newRoom = message.substring("roomchanged:".length());
            // ✅ 기존 방 로그 저장
            if (currentRoom != null && chatArea != null) {
                roomLogs.put(currentRoom, new StringBuilder(chatArea.getText()));
            }
            currentRoom = newRoom;

            // ✅ 콤보 선택을 프로그램적으로 맞추되 onAction은 막기
            if (roomCombo != null) {
                if (!roomsObservableList.contains(newRoom)) roomsObservableList.add(newRoom);
                suppressRoomComboEvent = true;
                roomCombo.getSelectionModel().select(newRoom);
                suppressRoomComboEvent = false;
            }

            // ✅ 새 방 로그 복원 (없으면 비우기)
            StringBuilder buf = roomLogs.get(newRoom);
            if (chatArea != null) chatArea.setText(buf == null ? "" : buf.toString());

        } else {
            // 기타
            appendToCurrentRoom(message + "\n");
        }
    }

    /** 현재 방 TextArea와 버퍼에 함께 기록 */
    private void appendToCurrentRoom(String line) {
        if (chatArea != null) chatArea.appendText(line);
        if (currentRoom == null) return; // 아직 방 미정(최초 연결 직후)일 수 있음
        roomLogs.computeIfAbsent(currentRoom, k -> new StringBuilder()).append(line);
    }

    /** "닉네임|상태,닉네임|상태,..." -> 리스트 채우기 */
    private void updateUserList(String userListString) {
        usersObservableList.clear();
        if (userListString == null || userListString.isBlank()) return;
        String[] entries = userListString.split(",");
        for (String entry : entries) {
            String[] parts = entry.split("\\|", 2);
            String nick = parts[0];
            String status = (parts.length > 1 && !parts[1].isBlank()) ? parts[1] : "활동 중";
            usersObservableList.add(nick + " (" + status + ")");
        }
    }

    /** "방1,방2,방3" -> 콤보 갱신 (현재 선택 유지) */
    private void updateRoomList(String csv) {
        if (roomCombo == null) return;
        roomsObservableList.clear();
        if (csv != null && !csv.isBlank()) {
            roomsObservableList.addAll(Arrays.asList(csv.split(",")));
        }
        suppressRoomComboEvent = true;
        if (currentRoom != null && roomsObservableList.contains(currentRoom)) {
            roomCombo.getSelectionModel().select(currentRoom);
        } else if (!roomsObservableList.isEmpty()) {
            roomCombo.getSelectionModel().selectFirst();
        } else {
            roomCombo.getSelectionModel().clearSelection();
        }
        suppressRoomComboEvent = false;
    }

    @FXML
    private void sendMessage() {
        String message = messageField.getText();
        if (message != null && !message.isEmpty() && client != null) {
            client.sendMessage(message);
            messageField.clear();
        }
    }

    /** 상태 콤보 선택 시 호출 (FXML onAction="#changeStatus") */
    @FXML
    private void changeStatus() {
        if (client == null || statusCombo == null) return;
        String status = statusCombo.getValue();
        if (status != null && !status.isBlank()) client.sendStatus(status);
    }

    /** + 버튼: 새 방 생성 (FXML onAction="#createRoom") */
    @FXML
    private void createRoom() {
        if (client == null || newRoomField == null) return;
        String room = newRoomField.getText();
        if (room == null) return;
        room = room.trim();
        if (room.isEmpty()) return;
        client.sendMessage("room:create:" + room);
        newRoomField.clear();
    }

    /** 로그인/화면 전환 후 ChatClient 주입 */
    public void setClient(ChatClient client) {
        this.client = client;

        // 초기 상태 서버 반영
        if (statusCombo != null && statusCombo.getValue() != null) {
            client.sendStatus(statusCombo.getValue());
        }

        // 최초 방 목록 요청
        if (roomCombo != null) {
            client.sendMessage("room:list");
        }
    }
}
