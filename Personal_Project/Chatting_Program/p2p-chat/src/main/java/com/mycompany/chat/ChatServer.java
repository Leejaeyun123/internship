package com.mycompany.chat; // ChatServer 클래스가 속한 패키지 선언

import java.io.IOException; // 입출력(예: 소켓) 예외 처리에 사용
import java.net.ServerSocket; // 서버 소켓(클라이언트 접속 대기)
import java.net.Socket; // 접속된 클라이언트 소켓 표현
import java.sql.*; // JDBC 사용을 위한 패키지 전체 임포트
import java.util.*; // List, Set, Map 등 컬렉션 사용
import java.util.concurrent.ConcurrentHashMap; // 스레드 안전한 맵(동시성 고려)

public class ChatServer { // 채팅 서버 전체 로직을 담는 클래스 시작

    private static final int PORT = 8000; // 서버가 바인드할 포트 번호
    private static final String DEFAULT_ROOM = "Lobby"; // 기본(초기) 채팅방 이름

    // 닉네임 -> 핸들러 // 각 사용자의 네트워크 연결을 관리하는 핸들러 매핑
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>(); // 동시 접근 안전한 맵
    // 닉네임 -> 상태 // 유저의 현재 상태(활동 중/자리 비움) 저장
    private static final Map<String, String> clientStatuses = new ConcurrentHashMap<>(); // 상태 문자열 보관
    // 방이름 -> 닉네임 세트 // 방마다 누가 있는지 관리
    private static final Map<String, Set<String>> rooms = new ConcurrentHashMap<>(); // 방 구성원 집합
    // 닉네임 -> 가입한 방들(여러개 가능) // 한 사용자가 가입한 모든 방 목록
    private static final Map<String, Set<String>> userRooms = new ConcurrentHashMap<>(); // 멤버십 관리
    // 닉네임 -> 활성(채팅 송신 대상) 방 // 사용자가 현재 채팅을 보낼 기본 대상 방
    private static final Map<String, String> activeRoom = new ConcurrentHashMap<>(); // 활성 방 매핑

    private static final Set<String> ALLOWED_STATUSES = Set.of("활동 중", "자리 비움"); // 허용되는 상태값 정의

    // DB // 데이터베이스 접속 정보 상수
    private static final String DB_URL = "jdbc:mysql://localhost:3306/chat_app?serverTimezone=UTC"; // JDBC URL
    private static final String DB_USER = "root"; // DB 사용자
    private static final String DB_PASSWORD = "ljy"; // DB 비밀번호

    public static void main(String[] args) { // 서버 진입점(메인 함수)
        ensureRoom(DEFAULT_ROOM); // 기본 방(Lobby)을 미리 생성(존재하지 않으면)
        System.out.println("서버가 " + PORT + "번 포트에서 시작되었습니다."); // 서버 시작 로그 출력
        try (ServerSocket serverSocket = new ServerSocket(PORT)) { // 지정 포트로 서버소켓 생성(try-with-resources로 자동 close)
            while (true) { // 서버는 무한 루프로 접속을 처리
                Socket clientSocket = serverSocket.accept(); // 클라이언트 접속 수락(블로킹)
                new Thread(new ClientHandler(clientSocket)).start(); // 접속당 새 스레드로 ClientHandler 실행
            } // while 끝
        } catch (IOException e) { // 소켓 생성/accept 중 예외 처리
            e.printStackTrace(); // 스택트레이스 출력
        } // try-catch 끝
    } // main 끝

    /* ---------------- 유틸 ---------------- */ // 공용 유틸리티 메서드 구역

    private static void ensureRoom(String room) { // 방이 존재하지 않으면 생성하는 헬퍼
        rooms.computeIfAbsent(room, r -> ConcurrentHashMap.newKeySet()); // 방 이름 키로 빈 동시성 Set 생성
    } // ensureRoom 끝

    private static Set<String> membership(String nick) { // 닉네임의 멤버십 Set 확보(없으면 생성)
        return userRooms.computeIfAbsent(nick, k -> ConcurrentHashMap.newKeySet()); // 사용자별 가입 방 집합 반환
    } // membership 끝

    private static void sendTo(String nick, String line) { // 특정 사용자에게 한 줄 메세지 전송
        ClientHandler h = clients.get(nick); // 닉네임으로 핸들러 조회
        if (h != null) h.sendMessage(line); // 핸들러가 있으면 송신
    } // sendTo 끝

    public static String getActiveRoomOrDefault(String nick) { // 활성 방이 없을 경우 기본 방 반환
        return activeRoom.getOrDefault(nick, DEFAULT_ROOM); // 맵 조회 실패 시 Lobby로
    } // getActiveRoomOrDefault 끝

    private static <T> List<T> snapshot(Collection<T> c) { // 동시성 컬렉션을 안전히 순회하기 위한 스냅샷 리스트 생성
        return (c == null) ? List.of() : new ArrayList<>(c); // null이면 빈 리스트, 아니면 복사본 반환
    } // snapshot 끝

    /* ---------------- payload 생성 ---------------- */ // 클라이언트로 보낼 프로토콜 문자열 생성기

    public static String getRoomListPayload() { // 방 목록 페이로드 생성
        return "roomlist:" + String.join(",", new TreeSet<>(rooms.keySet())); // 정렬된 방 이름들을 CSV로 연결
    } // getRoomListPayload 끝

    public static String buildAllUsersPayload() { // 전체 접속자 + 상태 페이로드
        StringBuilder sb = new StringBuilder("allusers:"); // 접두어 작성
        for (String nick : clients.keySet()) { // 현재 접속 중인 모든 사용자 순회
            String st = clientStatuses.getOrDefault(nick, "활동 중"); // 상태 조회(기본값: 활동 중)
            sb.append(nick).append("|").append(st).append(","); // "닉네임|상태," 형태로 추가
        } // for 끝
        if (sb.length() > "allusers:".length()) sb.setLength(sb.length() - 1); // 마지막 콤마 제거(항목이 있을 때만)
        return sb.toString(); // 완성 문자열 반환
    } // buildAllUsersPayload 끝

    public static String buildMyRoomsPayload(String nick) { // 특정 사용자의 가입 방 목록 페이로드
        Set<String> set = userRooms.get(nick); // 사용자 멤버십 집합 조회
        if (set == null || set.isEmpty()) return "myrooms:"; // 없으면 빈 페이로드 리턴
        List<String> list = new ArrayList<>(set); // 정렬을 위해 리스트로 복사
        Collections.sort(list); // 사전순 정렬
        return "myrooms:" + String.join(",", list); // "myrooms:방1,방2" 형태
    } // buildMyRoomsPayload 끝

    public static String buildActiveRoomPayload(String nick) { // 활성 방 알림 페이로드
        return "roomactive:" + getActiveRoomOrDefault(nick); // "roomactive:방"
    } // buildActiveRoomPayload 끝

    public static String buildUserListPayloadForRoom(String room) { // 특정 방의 참가자 목록 페이로드
        StringBuilder sb = new StringBuilder("userlist:"); // 접두어
        sb.append(room).append(":"); // 방 이름과 구분자 ':' 추가 → "userlist:방:"
        Set<String> set = rooms.get(room); // 방의 멤버 집합 조회
        if (set != null) { // 멤버가 존재하면
            for (String nick : set) { // 각 닉네임에 대해
                String st = clientStatuses.getOrDefault(nick, "활동 중"); // 상태 조회
                sb.append(nick).append("|").append(st).append(","); // "닉|상," 추가
            } // for 끝
        } // if 끝
        if (sb.length() > ("userlist:" + room + ":").length()) sb.setLength(sb.length() - 1); // 항목이 있으면 꼬리 콤마 제거
        return sb.toString(); // 완성 문자열 반환
    } // buildUserListPayloadForRoom 끝

    /* ---------------- 브로드캐스트 ---------------- */ // 여러 사용자에게 일괄 전송

    private static void broadcastRoomListToAll() { // 모든 클라이언트에게 방 목록 전파
        String payload = getRoomListPayload(); // 페이로드 생성
        for (ClientHandler c : clients.values()) c.sendMessage(payload); // 전 클라이언트로 전송
    } // broadcastRoomListToAll 끝

    private static void broadcastAllUsers() { // 전체 접속자/상태 목록 전파
        String payload = buildAllUsersPayload(); // 페이로드 생성
        for (ClientHandler c : clients.values()) c.sendMessage(payload); // 전 클라이언트로 전송
    } // broadcastAllUsers 끝

    private static void broadcastSystemToRoom(String room, String msg) { // 특정 방에 시스템 메시지 발송
        for (String nick : snapshot(rooms.get(room))) { // 방 멤버 스냅샷 순회
            ClientHandler c = clients.get(nick); // 핸들러 조회
            if (c != null) c.sendMessage("system:" + msg); // "system:..." 형태로 전송
        } // for 끝
    } // broadcastSystemToRoom 끝

    private static void broadcastUserList(String room) { // 특정 방의 참가자 목록을 방 멤버들에게 갱신 전파
        String payload = buildUserListPayloadForRoom(room); // 페이로드 생성
        for (String nick : snapshot(rooms.get(room))) { // 방 멤버 순회
            ClientHandler c = clients.get(nick); // 핸들러 조회
            if (c != null) c.sendMessage(payload); // 참가자 목록 전송
        } // for 끝
    } // broadcastUserList 끝

    /* ---------------- 방/멤버십 관리 ---------------- */ // 방 생성/입장/나가기/스위치

    public static void createRoom(String room) { // 새 방 생성(이미 있으면 유지)
        ensureRoom(room); // 방 존재 보장
        broadcastRoomListToAll(); // 전체에 방 목록 갱신 통지
    } // createRoom 끝

    public static void joinRoom(String nickname, String room) { // 사용자를 방에 가입시키고 활성화
        ensureRoom(room); // 방 생성 보장
        // 멤버십 추가 // 사용자-방 양쪽에 기록
        membership(nickname).add(room); // 사용자 가입 방 집합에 추가
        rooms.get(room).add(nickname); // 방 멤버 집합에 사용자 추가
        // 활성 방을 새 방으로 전환 // 이 사용자에게 기본 송신 방을 지정
        activeRoom.put(nickname, room); // 활성 방 갱신

        // 알림 // 가입자와 관련 클라이언트들에게 상태 전파
        sendTo(nickname, buildMyRoomsPayload(nickname)); // 내 가입 방 목록 갱신 전송
        sendTo(nickname, buildActiveRoomPayload(nickname)); // 내 활성 방 갱신 전송
        broadcastUserList(room); // 해당 방의 참가자 목록을 모든 멤버에게 갱신
        broadcastSystemToRoom(room, nickname + "님이 채팅방에 입장했습니다."); // 시스템 입장 메시지 브로드캐스트
        broadcastRoomListToAll(); // 방 목록 갱신 전파
        broadcastAllUsers(); // 전체 접속자/상태 갱신 전파
    } // joinRoom 끝

    public static void leaveRoom(String nickname, String room) { // 사용자가 특정 방을 나가기
        Set<String> mem = membership(nickname); // 사용자의 멤버십 집합 조회
        if (!mem.contains(room)) return; // 해당 방에 없으면 아무 작업도 하지 않음

        mem.remove(room); // 사용자 멤버십에서 해당 방 제거
        Set<String> rs = rooms.get(room); // 방 멤버 집합 조회
        if (rs != null) rs.remove(nickname); // 방 멤버에서 사용자 제거

        // 활성 방을 나갔으면 다른 방으로 스위치(없으면 Lobby에 자동 합류)
        if (room.equals(activeRoom.get(nickname))) { // 현재 활성 방을 나간 경우
            String next = mem.stream().findFirst().orElse(null); // 가입된 다른 방 중 하나 선택(있으면)
            if (next == null) { // 더 이상 가입된 방이 없다면
                // 로비로 되돌림 // 기본 방으로 자동 입장
                joinRoom(nickname, DEFAULT_ROOM); // 로비에 조인(이 과정에서 활성화/알림 처리됨)
            } else { // 다른 가입 방이 존재할 경우
                activeRoom.put(nickname, next); // 활성 방을 그 방으로 변경
                sendTo(nickname, buildActiveRoomPayload(nickname)); // 활성 방 변경 알림 전송
                broadcastUserList(next); // 새 활성 방의 참가자 목록 갱신 전파
            } // if-else 끝
        } // if 끝

        sendTo(nickname, buildMyRoomsPayload(nickname)); // 내 가입 방 목록 갱신 전송
        broadcastUserList(room); // 나간 방의 참가자 목록 갱신 전파
        broadcastSystemToRoom(room, nickname + "님이 채팅방을 나갔습니다."); // 시스템 퇴장 메시지
        broadcastRoomListToAll(); // 방 목록 갱신 전파
        broadcastAllUsers(); // 전체 접속자/상태 갱신 전파
    } // leaveRoom 끝

    public static void switchRoom(String nickname, String room) { // 활성 방만 전환(가입이 안 되어 있으면 자동 가입)
        if (!membership(nickname).contains(room)) { // 해당 방에 가입되어 있지 않다면
            // 아직 가입 안 한 방이면 자동 가입 + 스위치
            joinRoom(nickname, room); // joinRoom이 활성화까지 처리하므로 그대로 리턴
            return; // 조기 종료
        } // if 끝
        activeRoom.put(nickname, room); // 이미 가입된 방이면 활성 방만 바꿈
        sendTo(nickname, buildActiveRoomPayload(nickname)); // 활성 방 변경 알림 전송
        broadcastUserList(room); // 새 활성 방의 참가자 목록 갱신 전파
    } // switchRoom 끝

    public static String getRoom(String nickname) { // 외부에서 활성 방을 조회하려고 할 때 사용
        return getActiveRoomOrDefault(nickname); // 활성 방 없으면 기본 방 반환
    } // getRoom 끝

    /* ---------------- 수명주기 ---------------- */ // 접속/해제 시 처리

    public static void addClient(String nickname, ClientHandler handler) { // 새 사용자가 접속했을 때 호출
        clients.put(nickname, handler); // 클라이언트 핸들러 등록
        clientStatuses.put(nickname, "활동 중"); // 기본 상태를 "활동 중"으로 설정
        // 기본으로 로비에 가입 + 활성화
        joinRoom(nickname, DEFAULT_ROOM); // 로비에 입장(활성화 및 각종 알림 포함)
        System.out.println(nickname + " 접속. 현재 접속자 수: " + clients.size()); // 접속 로그 출력

        // 스냅샷 // 접속자 본인에게 현재 상태를 한 번에 내려줌
        handler.sendMessage(getRoomListPayload()); // 방 목록 전달
        handler.sendMessage(buildAllUsersPayload()); // 전체 접속자/상태 전달
        handler.sendMessage(buildMyRoomsPayload(nickname)); // 내 가입 방 목록 전달
        handler.sendMessage(buildActiveRoomPayload(nickname)); // 내 활성 방 전달
        handler.sendMessage(buildUserListPayloadForRoom(getActiveRoomOrDefault(nickname))); // 활성 방 참가자 목록 전달
    } // addClient 끝

    public static void removeClient(String nickname) { // 사용자가 접속 종료할 때 호출
        clients.remove(nickname); // 클라이언트 핸들러 제거
        clientStatuses.remove(nickname); // 상태 정보 제거

        Set<String> mem = userRooms.remove(nickname); // 사용자의 멤버십 집합 제거
        activeRoom.remove(nickname); // 활성 방 정보 제거

        if (mem != null) { // 사용자가 가입했던 방들이 있다면
            for (String r : snapshot(mem)) { // 각 방에 대해
                Set<String> rs = rooms.get(r); // 방 멤버 집합 조회
                if (rs != null) rs.remove(nickname); // 방 멤버에서 제거
                broadcastUserList(r); // 해당 방 참가자 목록 갱신 전파
                broadcastSystemToRoom(r, nickname + "님이 채팅방을 나갔습니다."); // 퇴장 시스템 메시지
            } // for 끝
        } // if 끝
        broadcastRoomListToAll(); // 방 목록 갱신 전파
        broadcastAllUsers(); // 전체 접속자/상태 갱신 전파
        System.out.println(nickname + " 종료. 현재 접속자 수: " + clients.size()); // 종료 로그 출력
    } // removeClient 끝

    /* ---------------- 상태/메시지 ---------------- */ // 상태 변경과 메시지 브로드캐스트

    public static void updateStatus(String nickname, String status) { // 사용자의 상태 변경 처리
        if (!ALLOWED_STATUSES.contains(status)) { // 허용되지 않은 상태면
            System.out.println("무시: 허용되지 않은 상태값 (" + status + ")"); // 로그만 남기고
            return; // 종료
        } // if 끝
        clientStatuses.put(nickname, status); // 상태값 저장

        // 본인이 속한 모든 방의 참가자 목록 갱신
        for (String room : snapshot(userRooms.get(nickname))) { // 유저가 가입한 모든 방 순회
            broadcastUserList(room); // 각 방의 참가자 목록 갱신 전파
        } // for 끝
        broadcastAllUsers(); // 전체 접속자/상태 목록도 갱신 전파
    } // updateStatus 끝

    public static void broadcastMessage(String senderNickname, String message) { // 채팅 메시지 브로드캐스트
        String room = getActiveRoomOrDefault(senderNickname); // 발신자의 현재 활성 방 결정
        saveMessageToDb(senderNickname, message, room); // DB에 로그 저장 시도
        for (String nick : snapshot(rooms.get(room))) { // 활성 방의 모든 멤버에게
            ClientHandler c = clients.get(nick); // 핸들러 조회
            if (c != null) c.sendMessage("chat:" + senderNickname + ": " + message); // "chat:닉: 내용" 송신
        } // for 끝
    } // broadcastMessage 끝

    /* ---------------- DB ---------------- */ // 채팅 로그 영속화 관련

    private static void saveMessageToDb(String nickname, String message, String room) { // 메시지를 DB에 저장
        String userId = fetchIdFromDatabase(nickname); // 닉네임으로 사용자 id 조회
        if (userId == null) { // 사용자가 users 테이블에 없으면
            System.err.println("메시지 보낸 닉네임을 users에서 찾지 못함: " + nickname); // 경고 출력
            return; // 저장하지 않고 종료
        } // if 끝
        String sql = "INSERT INTO chat_logs (id, nickname, message, room) VALUES (?, ?, ?, ?)"; // INSERT SQL
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD); // 커넥션 열기
             PreparedStatement pstmt = conn.prepareStatement(sql)) { // 프리페어드 스테이트먼트 준비
            pstmt.setString(1, userId); // 1번 파라미터: id
            pstmt.setString(2, nickname); // 2번 파라미터: nickname
            pstmt.setString(3, message); // 3번 파라미터: message
            pstmt.setString(4, room); // 4번 파라미터: room
            pstmt.executeUpdate(); // INSERT 실행
        } catch (SQLException e) { // SQL 예외 처리
            e.printStackTrace(); // 스택트레이스 출력
        } // try-catch 끝
    } // saveMessageToDb 끝

    private static String fetchIdFromDatabase(String nickname) { // 닉네임으로 users 테이블에서 id 조회
        String sql = "SELECT id FROM users WHERE nickname = ?"; // 조회용 SQL
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD); // 커넥션
             PreparedStatement pstmt = conn.prepareStatement(sql)) { // 스테이트먼트
            pstmt.setString(1, nickname); // 1번 파라미터 바인딩
            try (ResultSet rs = pstmt.executeQuery()) { // 쿼리 실행 및 결과셋 획득
                if (rs.next()) return rs.getString("id"); // 결과가 있으면 id 컬럼 반환
            } // try-with-resources(ResultSet) 끝
        } catch (SQLException e) { // SQL 예외 처리
            System.err.println("DB 조회 실패: " + e.getMessage()); // 에러 로그 간단 출력
            e.printStackTrace(); // 자세한 스택트레이스
        } // try-catch 끝
        return null; // 못 찾았으면 null 반환
    } // fetchIdFromDatabase 끝
} // ChatServer 클래스 끝
