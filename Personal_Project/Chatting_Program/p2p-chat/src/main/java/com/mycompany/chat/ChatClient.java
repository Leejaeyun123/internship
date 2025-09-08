package com.mycompany.chat; // 패키지 선언: 이 클래스가 속한 네임스페이스(디렉터리 구조와 매핑)

import javafx.application.Platform; // JavaFX UI 스레드에 작업을 위임할 때 사용하는 유틸(Platform.runLater)

import java.io.*; // 입출력 관련 클래스(BufferedReader, PrintWriter 등) 사용
import java.net.Socket; // TCP 소켓 통신을 위한 Socket 클래스
import java.nio.charset.StandardCharsets; // 문자 인코딩(UTF-8) 상수 제공

/** 클라이언트 ↔ 서버 네트워크 통신 담당 */ // 클래스 역할에 대한 문서화 주석
public class ChatClient { // ChatClient 클래스 선언 시작

    private final Socket socket; // 서버와 TCP 연결을 유지하는 소켓
    private final BufferedReader in; // 서버로부터 문자열 라인을 읽는 입력 스트림(버퍼링)
    private final PrintWriter out; // 서버로 문자열 라인을 보내는 출력 스트림(자동 flush)
    private final ChatMainController controller; // 수신한 메시지를 UI에 반영할 컨트롤러 참조
    private final String nickname; // 로그인한 사용자의 닉네임(서버에 최초 1줄로 전송)

    private final Thread listenerThread; // 서버 수신 전용 백그라운드 스레드

    public ChatClient(String serverAddress, int serverPort, // 생성자: 서버 주소/포트, UI 컨트롤러, 닉네임을 전달받음
                      ChatMainController controller, String nickname) throws IOException { // 네트워크 오류가 나면 IOException 발생
        this.socket = new Socket(serverAddress, serverPort); // 서버로 TCP 연결 시도(성공 시 소켓 생성)
        this.in = new BufferedReader( // 입력 스트림 초기화(문자 스트림 + 버퍼)
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)); // 소켓 입력 스트림을 UTF-8로 읽음
        this.out = new PrintWriter( // 출력 스트림 초기화(문자 스트림 + 자동 flush)
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true); // 소켓 출력 스트림을 UTF-8로 쓰고 autoFlush=true
        this.controller = controller; // UI 갱신을 요청할 컨트롤러 보관
        this.nickname = nickname; // 닉네임 보관

        // 최초 1줄: 닉네임 전송
        out.println(nickname); // 서버 프로토콜 규약: 연결 직후 첫 줄로 클라이언트 닉네임을 보냄

        // 서버 수신 스레드 시작
        this.listenerThread = new Thread(this::listenForMessages, "chat-client-listener"); // 수신 루프를 실행할 데몬 스레드 생성(람다 대신 메서드 레퍼런스)
        this.listenerThread.setDaemon(true); // 데몬 스레드 설정: 애플리케이션 종료 시 자동으로 함께 종료됨
        this.listenerThread.start(); // 수신 스레드 실행 시작
    } // 생성자 끝

    /* ===== 송신 유틸 ===== */ // 서버로 메시지를 보낼 때 사용하는 헬퍼 메서드 섹션

    /** 현재 활성 방으로 일반 채팅 전송 */ // 사용자가 입력한 채팅 텍스트를 서버로 전송
    public void sendMessage(String message) { // 일반 채팅 전송 메서드
        if (message == null) return; // null 안전 처리(아무것도 전송하지 않음)
        out.println(message); // 서버에 한 줄 전송(활성 방 기준으로 방송은 서버가 처리)
    } // sendMessage 끝

    /** 상태 변경 전송 ("활동 중" / "자리 비움") */ // 상태 변경을 서버에 알림
    public void sendStatus(String status) { // 상태 전송 메서드
        if (status == null) return; // null이면 무시
        out.println("status:" + status.trim()); // 프로토콜: "status:<값>" 형식으로 서버에 전달
    } // sendStatus 끝

    /** 임의의 프로토콜 라인 전송 (예: room:list) */ // 필요한 커맨드를 문자열로 직접 보낼 수 있게 함
    public void sendRaw(String line) { // 임의 라인 전송 메서드
        if (line == null) return; // null이면 무시
        out.println(line); // 전달받은 문자열을 그대로 서버로 전송
    } // sendRaw 끝

    /** 방 참가(+활성화) */ // 특정 방에 가입하고 그 방을 활성 방으로 설정하도록 요청
    public void sendJoinRoom(String room) { // 방 참가 요청 메서드
        if (room == null || room.isBlank()) return; // 빈 문자열/공백만 있으면 무시
        out.println("room:join:" + room.trim()); // 프로토콜: "room:join:<방이름>"
    } // sendJoinRoom 끝

    /** 방 나가기 */ // 특정 방에서 탈퇴 요청
    public void sendLeaveRoom(String room) { // 방 나가기 요청 메서드
        if (room == null || room.isBlank()) return; // 유효성 검사
        out.println("room:leave:" + room.trim()); // 프로토콜: "room:leave:<방이름>"
    } // sendLeaveRoom 끝

    /** 활성 방 전환(가입되어 있지 않다면 서버가 자동 가입) */ // 현재 채팅 전송 대상 방을 전환
    public void sendSwitchRoom(String room) { // 활성 방 전환 요청 메서드
        if (room == null || room.isBlank()) return; // 유효성 검사
        out.println("room:switch:" + room.trim()); // 프로토콜: "room:switch:<방이름>"
    } // sendSwitchRoom 끝

    /** 새 방 생성 */ // 서버에 새 채팅방 생성을 요청
    public void sendCreateRoom(String room) { // 방 생성 요청 메서드
        if (room == null || room.isBlank()) return; // 유효성 검사
        out.println("room:create:" + room.trim()); // 프로토콜: "room:create:<방이름>"
    } // sendCreateRoom 끝

    /* ===== 수신 루프 ===== */ // 서버에서 오는 메시지를 계속 읽어 UI로 전달하는 부분

    private void listenForMessages() { // 백그라운드에서 실행될 수신 루프
        try { // IO 예외 처리 블록 시작
            String msg; // 한 줄씩 읽어들일 버퍼 변수
            while ((msg = in.readLine()) != null) { // 서버로부터 라인이 들어올 때까지 블로킹, EOF일 경우 null
                final String line = msg; // 지역 변수 캡처: 람다에서 사용하기 위해 final로 별도 변수에 담음
                Platform.runLater(() -> controller.displayMessage(line)); // UI 스레드에서 컨트롤러로 전달하여 화면 갱신
            } // while 끝(서버가 연결을 끊으면 null이 되어 루프 종료)
        } catch (IOException e) { // 수신 중 예외 발생 시
            System.err.println("서버와의 연결이 끊어졌습니다: " + e.getMessage()); // 에러 로그 출력
            Platform.runLater(() -> controller.appendSystem("연결이 끊어졌습니다.")); // UI에 시스템 메시지로 표시
        } finally { // 정상/예외 상관없이 마지막에 실행
            try { socket.close(); } catch (IOException ignore) {} // 소켓 정리(이미 닫혔더라도 예외 무시)
        } // try-catch-finally 끝
    } // listenForMessages 끝

    /** 명시적으로 종료하고 싶을 때 호출 */ // 외부에서 클라이언트를 종료할 때 사용하는 메서드
    public void close() { // 자원 정리 메서드
        try { socket.close(); } catch (IOException ignore) {} // 소켓 닫기(예외 무시)
        if (listenerThread != null && listenerThread.isAlive()) { // 수신 스레드가 아직 살아있다면
            listenerThread.interrupt(); // 인터럽트로 종료 유도(블로킹 readLine에 바로 적용되지는 않을 수 있음)
        } // if 끝
    } // close 끝

    public String getNickname() { // 닉네임 getter
        return nickname; // 보관 중인 사용자 닉네임 반환
    } // getNickname 끝
} // ChatClient 클래스 끝
