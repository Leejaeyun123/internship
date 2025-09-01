// 이 클래스는 서버와의 연결을 관리하고, 메시지를 보내며, 별도의 스레드에서 서버로부터 메시지를 수신하는 역할을 함

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
    private ChatRoomController controller; // UI 컨트롤러에 대한 참조

    // 생성자: 서버와 연결하고 UI 컨트롤러를 연결
    public ChatClient(String serverAddress, int serverPort, ChatRoomController controller) throws IOException {
        this.socket = new Socket(serverAddress, serverPort);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.controller = controller;

        // 서버로부터 들어오는 메시지를 수신하기 위한 새 스레드 시작
        new Thread(this::listenForMessages).start();
    }

    // 서버로 메시지를 보내는 메소드
    public void sendMessage(String message) {
        out.println(message);
    }

    // 서버로부터 메시지를 수신하는 메소드
    private void listenForMessages() {
        try {
            String messageFromServer;
            while ((messageFromServer = in.readLine()) != null) {
                final String finalMessage = messageFromServer;
                // JavaFX 애플리케이션 스레드에서 UI를 업데이트
                Platform.runLater(() -> {
                    controller.displayMessage(finalMessage);
                });
            }
        } catch (IOException e) {
            System.err.println("서버로부터 연결이 끊어졌습니다.");
            e.printStackTrace();
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}