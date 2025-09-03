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
    private int senderId; // DB에 저장하기 위한 발신자 ID
    private String username;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/chat_app?serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "ljy";

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
            // ✅ 1. 클라이언트로부터 첫 번째 메시지로 username을 받음
            this.username = in.readLine();
            System.out.println("사용자 로그인됨: " + username);

            // ✅ 2. DB에서 senderId 조회
            this.senderId = fetchSenderIdFromDatabase(username);
            if (senderId == -1) {
                out.println("서버 오류: 사용자 ID를 찾을 수 없습니다.");
                clientSocket.close();
                return;
            }

            // ✅ 3. 메시지 수신 루프
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("[" + username + "]로부터 받은 메시지: " + message);
                ChatServer.broadcastMessage(username + ": " + message, this.senderId);
            }

        } catch (IOException e) {
            System.err.println("클라이언트 연결 오류: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null) clientSocket.close();
                // ✅ 4. 연결 종료 시 리스트에서 제거
                ChatServer.removeClient(this);
                System.out.println(username + " 클라이언트 연결 종료됨.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    private int fetchSenderIdFromDatabase(String username) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT id FROM users WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            System.err.println("DB에서 senderId 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        return -1; // 실패 시
    }
}
