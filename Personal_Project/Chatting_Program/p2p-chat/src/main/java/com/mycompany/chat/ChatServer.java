package com.mycompany.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

    private static final int PORT = 8000;

    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final Map<String, String> clientStatuses = new ConcurrentHashMap<>();
    private static final Set<String> ALLOWED_STATUSES = Set.of("활동 중", "자리 비움");

    private static final String DB_URL = "jdbc:mysql://localhost:3306/chat_app?serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "ljy";

    public static void main(String[] args) {
        System.out.println("서버가 " + PORT + "번 포트에서 시작되었습니다.");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("새로운 클라이언트 연결: " + clientSocket);
                new Thread(new ClientHandler(clientSocket)).start();
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
        clientStatuses.put(nickname, "활동 중");
        System.out.println(nickname + " 클라이언트 추가됨. 현재 접속자 수: " + clients.size());
        broadcastSystemMessage(nickname + "님이 채팅방에 입장했습니다.");
    }

    public static void removeClient(String nickname) {
        clients.remove(nickname);
        clientStatuses.remove(nickname);
        System.out.println(nickname + " 클라이언트 제거됨. 현재 접속자 수: " + clients.size());
        broadcastSystemMessage(nickname + "님이 채팅방을 나갔습니다.");
    }

    public static void updateStatus(String nickname, String status) {
        if (!ALLOWED_STATUSES.contains(status)) {
            System.out.println("무시: 허용되지 않은 상태값 (" + status + ")");
            return;
        }
        clientStatuses.put(nickname, status);
        broadcastUserList();
    }

    private static void broadcastUserList() {
        // 키 스냅샷으로 안전 순회
        List<String> nicks = new ArrayList<>(clients.keySet());
        StringBuilder sb = new StringBuilder("userlist:");
        for (String nick : nicks) {
            String st = clientStatuses.getOrDefault(nick, "활동 중");
            sb.append(nick).append("|").append(st).append(",");
        }
        if (sb.length() > "userlist:".length()) sb.setLength(sb.length() - 1);
        String payload = sb.toString();

        for (ClientHandler c : clients.values()) {
            c.sendMessage(payload);
        }
    }

    // ===== DB =====
    private static void saveMessageToDb(String nickname, String message) {
        String userId = fetchIdFromDatabase(nickname);
        if (userId == null) {
            System.err.println("메시지를 보낸 닉네임(" + nickname + ")에 해당하는 아이디를 찾을 수 없습니다.");
            return;
        }
        String sql = "INSERT INTO chat_logs (id, nickname, message) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, nickname);
            pstmt.setString(3, message);
            pstmt.executeUpdate();
            System.out.println("메시지가 데이터베이스에 저장되었습니다.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String fetchIdFromDatabase(String nickname) {
        String sql = "SELECT id FROM users WHERE nickname = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nickname);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("id");
            }
        } catch (SQLException e) {
            System.err.println("DB에서 아이디 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
