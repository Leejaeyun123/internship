package com.mycompany.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

    private static final int PORT = 8000;
    private static final String DEFAULT_ROOM = "Lobby";

    // 닉네임 -> 핸들러
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    // 닉네임 -> 상태
    private static final Map<String, String> clientStatuses = new ConcurrentHashMap<>();
    // 방이름 -> 닉네임 세트
    private static final Map<String, Set<String>> rooms = new ConcurrentHashMap<>();
    // 닉네임 -> 현재 방
    private static final Map<String, String> userRoom = new ConcurrentHashMap<>();

    private static final Set<String> ALLOWED_STATUSES = Set.of("활동 중", "자리 비움");

    // DB 설정
    private static final String DB_URL = "jdbc:mysql://localhost:3306/chat_app?serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "ljy";

    public static void main(String[] args) {
        ensureRoom(DEFAULT_ROOM);
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

    /* ---------- 방 관리 ---------- */

    private static void ensureRoom(String room) {
        rooms.computeIfAbsent(room, r -> ConcurrentHashMap.newKeySet());
    }

    public static void createRoom(String room) {
        ensureRoom(room);
        broadcastRoomListToAll();
    }

    public static void joinRoom(String nickname, String newRoom) {
        ensureRoom(newRoom);
        String old = userRoom.get(nickname);

        // ✅ 이미 같은 방이면 아무 것도 하지 않음 (루프 방지)
        if (old != null && old.equals(newRoom)) {
            ClientHandler same = clients.get(nickname);
            if (same != null) same.sendMessage("roomchanged:" + newRoom); // 동기화만
            broadcastUserList(newRoom);
            return;
        }

        if (old != null && rooms.containsKey(old)) {
            rooms.get(old).remove(nickname);
            broadcastUserList(old); // 나간 방 갱신
            broadcastSystemToRoom(old, nickname + "님이 채팅방을 나갔습니다.");
        }

        rooms.get(newRoom).add(nickname);
        userRoom.put(nickname, newRoom);

    ClientHandler h = clients.get(nickname);
        if (h != null) h.sendMessage("roomchanged:" + newRoom);

        broadcastUserList(newRoom); // 새 방 갱신
        broadcastSystemToRoom(newRoom, nickname + "님이 채팅방에 입장했습니다.");
        broadcastRoomListToAll();   // 전체 방 목록 갱신
    }

    public static String getRoom(String nickname) {
        return userRoom.getOrDefault(nickname, DEFAULT_ROOM);
    }

    private static void broadcastRoomListToAll() {
        String payload = getRoomListPayload();
        for (ClientHandler c : clients.values()) c.sendMessage(payload);
    }

    /** ClientHandler에서 사용: 현재 방 목록 payload 반환 */
    public static String getRoomListPayload() {
        return "roomlist:" + String.join(",", new TreeSet<>(rooms.keySet()));
    }

    /* ---------- 클라이언트 수명주기 ---------- */

    public static void addClient(String nickname, ClientHandler handler) {
        clients.put(nickname, handler);
        clientStatuses.put(nickname, "활동 중");
        joinRoom(nickname, DEFAULT_ROOM); // 기본 방 입장
        System.out.println(nickname + " 클라이언트 추가됨. 현재 접속자 수: " + clients.size());
    }

    public static void removeClient(String nickname) {
        String room = userRoom.remove(nickname);
        clients.remove(nickname);
        clientStatuses.remove(nickname);
        if (room != null && rooms.containsKey(room)) {
            rooms.get(room).remove(nickname);
            broadcastUserList(room);
            broadcastSystemToRoom(room, nickname + "님이 채팅방을 나갔습니다.");
        }
        System.out.println(nickname + " 클라이언트 제거됨. 현재 접속자 수: " + clients.size());
        broadcastRoomListToAll();
    }

    /* ---------- 방송/상태/메시지 ---------- */

    public static void updateStatus(String nickname, String status) {
        if (!ALLOWED_STATUSES.contains(status)) {
            System.out.println("무시: 허용되지 않은 상태값 (" + status + ")");
            return;
        }
        clientStatuses.put(nickname, status);
        broadcastUserList(getRoom(nickname));
    }

    public static void broadcastMessage(String senderNickname, String message) {
        String room = getRoom(senderNickname);
        saveMessageToDb(senderNickname, message, room);
        for (String nick : snapshot(rooms.get(room))) {
            ClientHandler c = clients.get(nick);
            if (c != null) c.sendMessage("chat:" + senderNickname + ": " + message);
        }
    }

    private static void broadcastSystemToRoom(String room, String msg) {
        for (String nick : snapshot(rooms.get(room))) {
            ClientHandler c = clients.get(nick);
            if (c != null) c.sendMessage("system:" + msg);
        }
    }

    private static void broadcastUserList(String room) {
        Set<String> set = rooms.get(room);
        if (set == null) return;
        StringBuilder sb = new StringBuilder("userlist:");
        for (String nick : snapshot(set)) {
            String st = clientStatuses.getOrDefault(nick, "활동 중");
            sb.append(nick).append("|").append(st).append(",");
        }
        if (sb.length() > "userlist:".length()) sb.setLength(sb.length() - 1);
        String payload = sb.toString();
        for (String nick : snapshot(set)) {
            ClientHandler c = clients.get(nick);
            if (c != null) c.sendMessage(payload);
        }
    }

    private static <T> List<T> snapshot(Collection<T> c) {
        return (c == null) ? List.of() : new ArrayList<>(c);
    }

    /* ---------- DB ---------- */

    // 방까지 함께 저장하도록 room 컬럼 추가 권장
    // ALTER TABLE chat_logs ADD COLUMN room VARCHAR(50) NOT NULL DEFAULT 'Lobby';
    private static void saveMessageToDb(String nickname, String message, String room) {
        String userId = fetchIdFromDatabase(nickname);
        if (userId == null) {
            System.err.println("메시지를 보낸 닉네임(" + nickname + ")에 해당하는 아이디를 찾을 수 없습니다.");
            return;
        }

        // room 컬럼 없으면 아래에서 room 제거하고 사용
        String sql = "INSERT INTO chat_logs (id, nickname, message) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, nickname);
            pstmt.setString(3, message);
            pstmt.executeUpdate();
            System.out.println("메시지가 데이터베이스에 저장되었습니다. (room=" + room + ")");
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
