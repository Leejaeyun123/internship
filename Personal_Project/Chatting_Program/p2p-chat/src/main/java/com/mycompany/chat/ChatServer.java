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
    // 닉네임 -> 가입한 방들(여러개 가능)
    private static final Map<String, Set<String>> userRooms = new ConcurrentHashMap<>();
    // 닉네임 -> 활성(채팅 송신 대상) 방
    private static final Map<String, String> activeRoom = new ConcurrentHashMap<>();

    private static final Set<String> ALLOWED_STATUSES = Set.of("활동 중", "자리 비움");

    // DB
    private static final String DB_URL = "jdbc:mysql://localhost:3306/chat_app?serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "ljy";

    public static void main(String[] args) {
        ensureRoom(DEFAULT_ROOM);
        System.out.println("서버가 " + PORT + "번 포트에서 시작되었습니다.");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* ---------------- 유틸 ---------------- */

    private static void ensureRoom(String room) {
        rooms.computeIfAbsent(room, r -> ConcurrentHashMap.newKeySet());
    }

    private static Set<String> membership(String nick) {
        return userRooms.computeIfAbsent(nick, k -> ConcurrentHashMap.newKeySet());
    }

    private static void sendTo(String nick, String line) {
        ClientHandler h = clients.get(nick);
        if (h != null) h.sendMessage(line);
    }

    public static String getActiveRoomOrDefault(String nick) {
        return activeRoom.getOrDefault(nick, DEFAULT_ROOM);
    }

    private static <T> List<T> snapshot(Collection<T> c) {
        return (c == null) ? List.of() : new ArrayList<>(c);
    }

    /* ---------------- payload 생성 ---------------- */

    public static String getRoomListPayload() {
        return "roomlist:" + String.join(",", new TreeSet<>(rooms.keySet()));
    }

    public static String buildAllUsersPayload() {
        StringBuilder sb = new StringBuilder("allusers:");
        for (String nick : clients.keySet()) {
            String st = clientStatuses.getOrDefault(nick, "활동 중");
            sb.append(nick).append("|").append(st).append(",");
        }
        if (sb.length() > "allusers:".length()) sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    public static String buildMyRoomsPayload(String nick) {
        Set<String> set = userRooms.get(nick);
        if (set == null || set.isEmpty()) return "myrooms:";
        List<String> list = new ArrayList<>(set);
        Collections.sort(list);
        return "myrooms:" + String.join(",", list);
    }

    public static String buildActiveRoomPayload(String nick) {
        return "roomactive:" + getActiveRoomOrDefault(nick);
    }

    public static String buildUserListPayloadForRoom(String room) {
        StringBuilder sb = new StringBuilder("userlist:");
        sb.append(room).append(":");
        Set<String> set = rooms.get(room);
        if (set != null) {
            for (String nick : set) {
                String st = clientStatuses.getOrDefault(nick, "활동 중");
                sb.append(nick).append("|").append(st).append(",");
            }
        }
        if (sb.length() > ("userlist:" + room + ":").length()) sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    /* ---------------- 브로드캐스트 ---------------- */

    private static void broadcastRoomListToAll() {
        String payload = getRoomListPayload();
        for (ClientHandler c : clients.values()) c.sendMessage(payload);
    }

    private static void broadcastAllUsers() {
        String payload = buildAllUsersPayload();
        for (ClientHandler c : clients.values()) c.sendMessage(payload);
    }

    private static void broadcastSystemToRoom(String room, String msg) {
        for (String nick : snapshot(rooms.get(room))) {
            ClientHandler c = clients.get(nick);
            if (c != null) c.sendMessage("system:" + msg);
        }
    }

    private static void broadcastUserList(String room) {
        String payload = buildUserListPayloadForRoom(room);
        for (String nick : snapshot(rooms.get(room))) {
            ClientHandler c = clients.get(nick);
            if (c != null) c.sendMessage(payload);
        }
    }

    /* ---------------- 방/멤버십 관리 ---------------- */

    public static void createRoom(String room) {
        ensureRoom(room);
        broadcastRoomListToAll();
    }

    public static void joinRoom(String nickname, String room) {
        ensureRoom(room);
        // 멤버십 추가
        membership(nickname).add(room);
        rooms.get(room).add(nickname);
        // 활성 방을 새 방으로 전환
        activeRoom.put(nickname, room);

        // 알림
        sendTo(nickname, buildMyRoomsPayload(nickname));
        sendTo(nickname, buildActiveRoomPayload(nickname));
        broadcastUserList(room);
        broadcastSystemToRoom(room, nickname + "님이 채팅방에 입장했습니다.");
        broadcastRoomListToAll();
        broadcastAllUsers();
    }

    public static void leaveRoom(String nickname, String room) {
        Set<String> mem = membership(nickname);
        if (!mem.contains(room)) return;

        mem.remove(room);
        Set<String> rs = rooms.get(room);
        if (rs != null) rs.remove(nickname);

        // 활성 방을 나갔으면 다른 방으로 스위치(없으면 Lobby에 자동 합류)
        if (room.equals(activeRoom.get(nickname))) {
            String next = mem.stream().findFirst().orElse(null);
            if (next == null) {
                // 로비로 되돌림
                joinRoom(nickname, DEFAULT_ROOM);
            } else {
                activeRoom.put(nickname, next);
                sendTo(nickname, buildActiveRoomPayload(nickname));
                broadcastUserList(next);
            }
        }

        sendTo(nickname, buildMyRoomsPayload(nickname));
        broadcastUserList(room);
        broadcastSystemToRoom(room, nickname + "님이 채팅방을 나갔습니다.");
        broadcastRoomListToAll();
        broadcastAllUsers();
    }

    public static void switchRoom(String nickname, String room) {
        if (!membership(nickname).contains(room)) {
            // 아직 가입 안 한 방이면 자동 가입 + 스위치
            joinRoom(nickname, room);
            return;
        }
        activeRoom.put(nickname, room);
        sendTo(nickname, buildActiveRoomPayload(nickname));
        broadcastUserList(room);
    }

    public static String getRoom(String nickname) {
        return getActiveRoomOrDefault(nickname);
    }

    /* ---------------- 수명주기 ---------------- */

    public static void addClient(String nickname, ClientHandler handler) {
        clients.put(nickname, handler);
        clientStatuses.put(nickname, "활동 중");
        // 기본으로 로비에 가입 + 활성화
        joinRoom(nickname, DEFAULT_ROOM);
        System.out.println(nickname + " 접속. 현재 접속자 수: " + clients.size());

        // 스냅샷
        handler.sendMessage(getRoomListPayload());
        handler.sendMessage(buildAllUsersPayload());
        handler.sendMessage(buildMyRoomsPayload(nickname));
        handler.sendMessage(buildActiveRoomPayload(nickname));
        handler.sendMessage(buildUserListPayloadForRoom(getActiveRoomOrDefault(nickname)));
    }

    public static void removeClient(String nickname) {
        clients.remove(nickname);
        clientStatuses.remove(nickname);

        Set<String> mem = userRooms.remove(nickname);
        activeRoom.remove(nickname);

        if (mem != null) {
            for (String r : snapshot(mem)) {
                Set<String> rs = rooms.get(r);
                if (rs != null) rs.remove(nickname);
                broadcastUserList(r);
                broadcastSystemToRoom(r, nickname + "님이 채팅방을 나갔습니다.");
            }
        }
        broadcastRoomListToAll();
        broadcastAllUsers();
        System.out.println(nickname + " 종료. 현재 접속자 수: " + clients.size());
    }

    /* ---------------- 상태/메시지 ---------------- */

    public static void updateStatus(String nickname, String status) {
        if (!ALLOWED_STATUSES.contains(status)) {
            System.out.println("무시: 허용되지 않은 상태값 (" + status + ")");
            return;
        }
        clientStatuses.put(nickname, status);

        // 본인이 속한 모든 방의 참가자 목록 갱신
        for (String room : snapshot(userRooms.get(nickname))) {
            broadcastUserList(room);
        }
        broadcastAllUsers();
    }

    public static void broadcastMessage(String senderNickname, String message) {
        String room = getActiveRoomOrDefault(senderNickname);
        saveMessageToDb(senderNickname, message, room);
        for (String nick : snapshot(rooms.get(room))) {
            ClientHandler c = clients.get(nick);
            if (c != null) c.sendMessage("chat:" + senderNickname + ": " + message);
        }
    }

    /* ---------------- DB ---------------- */

    private static void saveMessageToDb(String nickname, String message, String room) {
        String userId = fetchIdFromDatabase(nickname);
        if (userId == null) {
            System.err.println("메시지 보낸 닉네임을 users에서 찾지 못함: " + nickname);
            return;
        }
        String sql = "INSERT INTO chat_logs (id, nickname, message, room) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, nickname);
            pstmt.setString(3, message);
            pstmt.setString(4, room);
            pstmt.executeUpdate();
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
            System.err.println("DB 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
