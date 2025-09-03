// 클라이언트의 네트워크 통신을 관리
package com.mycompany.chat;

import javafx.application.Platform;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ChatRoomController controller;
    private String nickname; // ✅ username 대신 nickname 사용

    public ChatClient(String serverAddress, int serverPort, ChatRoomController controller, String nickname) throws IOException {
        this.socket = new Socket(serverAddress, serverPort);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.controller = controller;
        this.nickname = nickname;

        // ✅ 서버에 최초로 닉네임 전송
        out.println(nickname);

        // 메시지 수신 쓰레드 시작
        new Thread(this::listenForMessages).start();
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    private void listenForMessages() {
        try {
            String messageFromServer;
            while ((messageFromServer = in.readLine()) != null) {
                final String finalMessage = messageFromServer;
                Platform.runLater(() -> {
                    controller.displayMessage(finalMessage);
                });
            }
        } catch (IOException e) {
            System.err.println("서버와의 연결이 끊어졌습니다.");
            Platform.runLater(() -> controller.displayMessage("⚠ 서버와 연결이 끊어졌습니다."));
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}