// 여러 클라이언트의 접속을 관리하고, 메시지를 중계하며, 채팅 기록을 데이터베이스에 저장하는 핵심적인 기능을 수행

package com.mycompany.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatServer {

    private static final int PORT = 8000;
    // 모든 클라이언트 핸들러를 저장하는 동기화된 리스트
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    
    // 데이터베이스 연결 정보
    private static final String DB_URL = "jdbc:mysql://localhost:3306/chat_app?serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "ljy";

    public static void main(String[] args) {
        System.out.println("서버가 " + PORT + "번 포트에서 시작되었습니다.");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("새로운 클라이언트 연결: " + clientSocket);

                // 새로운 클라이언트를 처리할 ClientHandler 스레드 생성 및 시작
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler); // 리스트에 추가하여 관리
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 모든 클라이언트에게 메시지 브로드캐스트
    public static void broadcastMessage(String message, int senderId) {
        // 먼저 메시지를 DB에 저장
        saveMessageToDb(senderId, message);
        
        // 연결된 모든 클라이언트에게 메시지 전송
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
    
    // 데이터베이스에 메시지 저장
    private static void saveMessageToDb(int senderId, String message) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "INSERT INTO chat_logs (sender_id, message) VALUES (?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, senderId);
            pstmt.setString(2, message);
            pstmt.executeUpdate();
            System.out.println("메시지가 데이터베이스에 저장되었습니다.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static void removeClient(ClientHandler clientHandler) {
    clients.remove(clientHandler);
    System.out.println("클라이언트 제거됨. 현재 접속자 수: " + clients.size());
}
}