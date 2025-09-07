package com.mycompany.chat;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;

import java.util.*;

public class ChatMainController {

    // 상단
    @FXML private ComboBox<String> statusCombo;
    @FXML private Label myNickLabel;

    // 좌측: 방 목록 + 새 방 만들기
    @FXML private ListView<String> roomListView;
    @FXML private TextField newRoomField;

    // 하단: 전체 접속자
    @FXML private ListView<String> allUsersListView;
    @FXML private Label allUsersTitle;

    // 중앙: 채팅 영역 + 입력창
    @FXML private TextArea chatArea;
    @FXML private TextField messageField;
    @FXML private Button sendBtn;

    // 우측: 현재(활성) 방 참가자
    @FXML private ListView<String> roomUsersListView;
    @FXML private Label roomUsersTitle;

    /** 방 “원본 이름” 목록 */
    private final ObservableList<String> roomItems = FXCollections.observableArrayList();
    private final ObservableList<String> allUserItems = FXCollections.observableArrayList();
    private final ObservableList<String> roomUserItems = FXCollections.observableArrayList();

    /** 내가 가입한 방들 */
    private final Set<String> myRooms = new HashSet<>();
    /** 현재 활성 방(메시지 전송 대상) */
    private String activeRoom = null;
    /** 활성 방에서 타이핑 중인지 */
    private boolean typingActive = false;

    private ChatClient client;
    private String myNickname;

    /* ---------------- 초기화/주입 ---------------- */

    public void init(String nickname, ChatClient client) {
        this.myNickname = nickname;
        this.client = client;

        if (myNickLabel != null) myNickLabel.setText(nickname);
        if (chatArea != null) chatArea.setWrapText(true);

        if (statusCombo != null) {
            statusCombo.setItems(FXCollections.observableArrayList("활동 중", "자리 비움"));
            statusCombo.getSelectionModel().select("활동 중");
        }

        if (roomListView != null) {
            roomListView.setItems(roomItems);
            // 셀에 "입장 중 / 채팅 중" 표시
            roomListView.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); return; }
                    String label = item;
                    if (myRooms.contains(item)) label += " - 입장 중";
                    if (typingActive && item.equals(activeRoom)) label += " - 채팅 중";
                    setText(label);
                }
            });
            // 더블 클릭 → 참가(+활성화)
            roomListView.setOnMouseClicked(ev -> {
                if (ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() == 2) {
                    joinRoomAction();
                }
            });
        }
        if (allUsersListView != null) allUsersListView.setItems(allUserItems);
        if (roomUsersListView != null) roomUsersListView.setItems(roomUserItems);

        // 메시지 입력 상태 → 타이핑 플래그 갱신
        if (messageField != null) {
            messageField.textProperty().addListener((obs, ov, nv) -> {
                boolean now = nv != null && !nv.isBlank();
                if (now != typingActive) {
                    typingActive = now;
                    if (roomListView != null) roomListView.refresh();
                }
            });
        }

        // 최초 목록 요청
        if (client != null) client.sendRaw("room:list");
    }

    /* ---------------- 서버 수신 처리 ---------------- */

    public void displayMessage(String line) {
        if (line == null) return;

        if (line.startsWith("chat:")) {
            appendChat(line.substring("chat:".length()));

        } else if (line.startsWith("system:")) {
            appendSystem(line.substring("system:".length()));

        } else if (line.startsWith("roomlist:")) {
            updateRoomList(line.substring("roomlist:".length()));

        } else if (line.startsWith("myrooms:")) {
            updateMyRooms(line.substring("myrooms:".length()));

        } else if (line.startsWith("roomactive:")) {
            activeRoom = line.substring("roomactive:".length());
            if (roomListView != null) roomListView.refresh();

        } else if (line.startsWith("userlist:")) {
            // 형식: "userlist:<room>:nick|status,nick|status"
            String body = line.substring("userlist:".length());
            String room = body;
            String csv = "";
            int idx = body.indexOf(':');
            if (idx >= 0) {
                room = body.substring(0, idx);
                csv  = body.substring(idx + 1);
            }
            // 우측 패널은 "활성 방"의 참가자만 표시
            if (room != null && room.equals(activeRoom)) {
                updateRoomUsers(csv);
            }

        } else if (line.startsWith("allusers:")) {
            updateAllUsers(line.substring("allusers:".length()));

        } else {
            if (chatArea != null) chatArea.appendText(line + "\n");
        }
    }

    private void appendChat(String body) {
        if (chatArea != null) chatArea.appendText(body + "\n");
    }
    public void appendSystem(String msg) {
        if (chatArea != null) chatArea.appendText("== " + msg + " ==\n");
    }

    private void updateRoomList(String csv) {
        roomItems.clear();
        if (csv != null && !csv.isBlank()) {
            roomItems.addAll(Arrays.asList(csv.split(",")));
        }
        if (roomListView != null) roomListView.refresh();
        // 선택이 없으면 첫 항목만 선택(참가 아님)
        if (!roomItems.isEmpty() && roomListView.getSelectionModel().isEmpty()) {
            roomListView.getSelectionModel().selectFirst();
        }
    }

    private void updateMyRooms(String csv) {
        myRooms.clear();
        if (csv != null && !csv.isBlank()) {
            myRooms.addAll(Arrays.asList(csv.split(",")));
        }
        if (roomListView != null) roomListView.refresh();
    }

    private void updateRoomUsers(String csv) {
        roomUserItems.clear();
        int count = 0;
        if (csv != null && !csv.isBlank()) {
            for (String token : csv.split(",")) {
                String[] parts = token.split("\\|", 2);
                String nick = parts[0];
                String st = (parts.length > 1 && !parts[1].isBlank()) ? parts[1] : "활동 중";
                roomUserItems.add(nick + " (" + st + ")");
                count++;
            }
        }
        if (roomUsersTitle != null) roomUsersTitle.setText("참가자 (" + count + "명)");
    }

    private void updateAllUsers(String csv) {
        allUserItems.clear();
        int count = 0;
        if (csv != null && !csv.isBlank()) {
            for (String token : csv.split(",")) {
                String[] parts = token.split("\\|", 2);
                String nick = parts[0];
                String st = (parts.length > 1 && !parts[1].isBlank()) ? parts[1] : "활동 중";
                allUserItems.add(nick + " (" + st + ")");
                count++;
            }
        }
        if (allUsersTitle != null) allUsersTitle.setText("전체 접속자 (" + count + "명)");
    }

    /* ---------------- FXML 이벤트 ---------------- */

    @FXML private void changeStatusAction() {
        if (client == null || statusCombo == null) return;
        String st = statusCombo.getValue();
        if (st != null && !st.isBlank()) client.sendStatus(st);
    }

    @FXML private void createRoomAction() {
        if (client == null || newRoomField == null) return;
        String room = safeTrim(newRoomField.getText());
        if (room.isEmpty()) return;
        client.sendCreateRoom(room);
        // 만들면 바로 참가+활성화
        client.sendJoinRoom(room);
        newRoomField.clear();
    }

    @FXML private void joinRoomAction() {
        if (client == null || roomListView == null) return;
        String room = roomListView.getSelectionModel().getSelectedItem();
        if (room == null || room.isBlank()) return;
        client.sendJoinRoom(room);   // 가입 + 활성화
    }

    @FXML private void leaveRoomAction() {
        if (client == null || roomListView == null) return;
        String room = roomListView.getSelectionModel().getSelectedItem();
        if (room == null || room.isBlank()) return;
        client.sendLeaveRoom(room);
    }

    @FXML private void sendMessageAction() {
        if (client == null || messageField == null) return;
        String msg = safeTrim(messageField.getText());
        if (msg.isEmpty()) return;
        client.sendMessage(msg);
        messageField.clear(); // → typingActive=false 로 바뀌며 목록 갱신됨
    }

    /* ---------------- 유틸 ---------------- */

    private static String safeTrim(String s) {
        return (s == null) ? "" : s.trim();
    }
}
