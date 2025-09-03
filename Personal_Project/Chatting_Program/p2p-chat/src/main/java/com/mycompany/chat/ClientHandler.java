// 개별 클라이언트의 연결을 처리하는 스레드
package com.mycompany.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private String nickname; // ✅ 닉네임 변수 추가

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        try {
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            // ✅ 1. 클라이언트로부터 첫 번째 메시지로 닉네임을 받음
            this.nickname = in.readLine();
            System.out.println("사용자 접속: " + nickname);

            // ✅ 2. 메시지 수신 루프
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("[" + nickname + "]로부터 받은 메시지: " + message);
                ChatServer.broadcastMessage(this.nickname, message); // ✅ 닉네임을 전달
            }
        } catch (IOException e) {
            System.err.println("클라이언트 연결 오류: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null) clientSocket.close();
                // ✅ 3. 연결 종료 시 리스트에서 제거
                ChatServer.removeClient(this);
                System.out.println(nickname + " 클라이언트 연결 종료됨.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}