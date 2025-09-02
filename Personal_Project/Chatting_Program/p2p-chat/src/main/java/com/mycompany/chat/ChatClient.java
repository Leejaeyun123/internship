package com.mycompany.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javafx.application.Platform;

public class ChatClient {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ChatRoomController controller; // UI 컨트롤러
    private String username;

    // ✅ username 인자를 추가한 생성자
    public ChatClient(String serverAddress, int serverPort, ChatRoomController controller, String username) throws IOException {
        this.socket = new Socket(serverAddress, serverPort);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.controller = controller;
        this.username = username;

        // ✅ 서버에 최초로 username 전송
        out.println(username);

        // 메시지 수신 쓰레드 시작
        new Thread(this::listenForMessages).start();
    }

    // 서버에 메시지 전송
    public void sendMessage(String message) {
        out.println(message);
    }

    // 서버로부터 메시지를 수신
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
