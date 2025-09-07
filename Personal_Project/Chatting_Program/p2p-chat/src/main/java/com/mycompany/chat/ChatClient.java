package com.mycompany.chat;

import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** 클라이언트 ↔ 서버 네트워크 통신 담당 */
public class ChatClient {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final ChatMainController controller;
    private final String nickname;

    private final Thread listenerThread;

    public ChatClient(String serverAddress, int serverPort,
                      ChatMainController controller, String nickname) throws IOException {
        this.socket = new Socket(serverAddress, serverPort);
        this.in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        this.controller = controller;
        this.nickname = nickname;

        // 최초 1줄: 닉네임 전송
        out.println(nickname);

        // 서버 수신 스레드 시작
        this.listenerThread = new Thread(this::listenForMessages, "chat-client-listener");
        this.listenerThread.setDaemon(true);
        this.listenerThread.start();
    }

    /* ===== 송신 유틸 ===== */

    /** 현재 활성 방으로 일반 채팅 전송 */
    public void sendMessage(String message) {
        if (message == null) return;
        out.println(message);
    }

    /** 상태 변경 전송 ("활동 중" / "자리 비움") */
    public void sendStatus(String status) {
        if (status == null) return;
        out.println("status:" + status.trim());
    }

    /** 임의의 프로토콜 라인 전송 (예: room:list) */
    public void sendRaw(String line) {
        if (line == null) return;
        out.println(line);
    }

    /** 방 참가(+활성화) */
    public void sendJoinRoom(String room) {
        if (room == null || room.isBlank()) return;
        out.println("room:join:" + room.trim());
    }

    /** 방 나가기 */
    public void sendLeaveRoom(String room) {
        if (room == null || room.isBlank()) return;
        out.println("room:leave:" + room.trim());
    }

    /** 활성 방 전환(가입되어 있지 않다면 서버가 자동 가입) */
    public void sendSwitchRoom(String room) {
        if (room == null || room.isBlank()) return;
        out.println("room:switch:" + room.trim());
    }

    /** 새 방 생성 */
    public void sendCreateRoom(String room) {
        if (room == null || room.isBlank()) return;
        out.println("room:create:" + room.trim());
    }

    /* ===== 수신 루프 ===== */

    private void listenForMessages() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                final String line = msg;
                Platform.runLater(() -> controller.displayMessage(line));
            }
        } catch (IOException e) {
            System.err.println("서버와의 연결이 끊어졌습니다: " + e.getMessage());
            Platform.runLater(() -> controller.appendSystem("연결이 끊어졌습니다."));
        } finally {
            try { socket.close(); } catch (IOException ignore) {}
        }
    }

    /** 명시적으로 종료하고 싶을 때 호출 */
    public void close() {
        try { socket.close(); } catch (IOException ignore) {}
        if (listenerThread != null && listenerThread.isAlive()) {
            listenerThread.interrupt();
        }
    }

    public String getNickname() {
        return nickname;
    }
}
