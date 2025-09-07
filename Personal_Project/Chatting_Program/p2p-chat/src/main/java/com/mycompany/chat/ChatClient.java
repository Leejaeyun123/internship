package com.mycompany.chat;

import javafx.application.Platform;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ChatClient {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final ChatRoomController controller;
    private final String nickname;

    public ChatClient(String serverAddress, int serverPort,
                      ChatRoomController controller, String nickname) throws IOException {
        this.socket = new Socket(serverAddress, serverPort);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        this.controller = controller;
        this.nickname = nickname;

        // 닉네임 1줄 전송
        out.println(nickname);

        // 수신 스레드
        new Thread(this::listenForMessages, "chat-client-listener").start();
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    // 상태 전송
    public void sendStatus(String status) {
        out.println("status:" + status.trim());
    }

    private void listenForMessages() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                final String m = msg;
                Platform.runLater(() -> controller.displayMessage(m));
            }
        } catch (IOException e) {
            System.err.println("서버와의 연결이 끊어졌습니다.");
            Platform.runLater(() -> controller.displayMessage("⚠ 서버와 연결이 끊어졌습니다."));
        } finally {
            try { socket.close(); } catch (IOException ignore) {}
        }
    }
}
