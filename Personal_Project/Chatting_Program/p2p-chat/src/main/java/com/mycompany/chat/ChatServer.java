package com.mycompany.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ChatServer {

    private static final int PORT = 8000;
    private static final Map<String, ClientHandler> clients = Collections.synchronizedMap(new HashMap<>());
    private static final String DB_URL = "jdbc:mysql://localhost:3306/chat_app?serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "ljy";

    public static void main(String[] args) {
        System.out.println("서버가 " + PORT + "번 포트에서 시작되었습니다.");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("새로운 클라이언트 연결: " + clientSocket);
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcastMessage(String senderNickname, String message) {
        saveMessageToDb(senderNickname, message);
        for (ClientHandler client : clients.values()) {
            client.sendMessage("chat:" + senderNickname + ": " + message);
        }
    }

    public static void broadcastSystemMessage(String message) {
        for (ClientHandler client : clients.values()) {
            client.sendMessage("system:" + message);
        }
        broadcastUserList();
    }

    public static void addClient(String nickname, ClientHandler handler) {
        clients.put(nickname, handler);
        System.out.println(nickname + " 클라이언트 추가됨. 현재 접속자 수: " + clients.size());
        broadcastSystemMessage(nickname + "님이 채팅방에 입장했습니다.");
    }

    public static void removeClient(String nickname) {
        clients.remove(nickname);
        System.out.println(nickname + " 클라이언트 제거됨. 현재 접속자 수: " + clients.size());
        broadcastSystemMessage(nickname + "님이 채팅방을 나갔습니다.");
    }

    private static void broadcastUserList() {
        StringBuilder userList = new StringBuilder("userlist:");
        for (String nickname : clients.keySet()) {
            userList.append(nickname).append(",");
        }
        if (userList.length() > "userlist:".length()) {
            userList.setLength(userList.length() - 1);
        }
        for (ClientHandler client : clients.values()) {
            client.sendMessage(userList.toString());
        }
    }

    private static void saveMessageToDb(String nickname, String message) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "INSERT INTO chat_logs (id, nickname, message) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            String id = fetchIdFromDatabase(nickname);
            if (id != null) {
                pstmt.setString(1, id);
                pstmt.setString(2, nickname);
                pstmt.setString(3, message);
                pstmt.executeUpdate();
                System.out.println("메시지가 데이터베이스에 저장되었습니다.");
            } else {
                System.err.println("메시지를 보낸 닉네임(" + nickname + ")에 해당하는 아이디를 찾을 수 없습니다.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String fetchIdFromDatabase(String nickname) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT id FROM users WHERE nickname = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, nickname);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("id");
            }
        } catch (SQLException e) {
            System.err.println("DB에서 아이디 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}