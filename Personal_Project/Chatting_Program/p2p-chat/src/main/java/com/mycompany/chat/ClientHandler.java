package com.mycompany.chat; // 이 파일이 속한 패키지를 선언. 다른 클래스들과의 네임스페이스 역할.

import java.io.*; // 입출력 관련 클래스(Reader/Writer 등)를 사용하기 위해 임포트.
import java.net.Socket; // TCP 소켓 통신을 위한 Socket 클래스를 사용하기 위해 임포트.
import java.nio.charset.StandardCharsets; // 고정된 문자셋(UTF-8)을 지정해서 스트림을 감싸기 위해 임포트.

/** 각 클라이언트 소켓을 처리 */ // 이 클래스는 서버에 접속한 "각 클라이언트"를 담당하는 작업 스레드의 본체.
public class ClientHandler implements Runnable { // Runnable을 구현하여 스레드에서 실행될 수 있도록 함.

    private final Socket clientSocket; // 접속한 클라이언트와 통신할 소켓 인스턴스(불변).
    private BufferedReader in; // 클라이언트로부터 문자열(한 줄 단위)을 읽기 위한 입력 스트림 래퍼.
    private PrintWriter out; // 클라이언트로 문자열(한 줄 단위)을 보내기 위한 출력 스트림 래퍼.
    private String nickname; // 이 소켓(클라이언트)의 닉네임을 저장.

    public ClientHandler(Socket socket) { // 생성자: 서버가 accept()로 얻은 소켓을 주입.
        this.clientSocket = socket; // 전달받은 소켓을 필드에 보관.
    } // 생성자 끝.

    @Override
    public void run() { // 스레드가 시작되면 실행되는 메서드(핵심 루프 포함).
        try { // 네트워크/IO 처리 중 발생하는 예외를 포착.
            in  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8)); // 소켓 입력 스트림을 UTF-8로 디코딩하는 문자 스트림으로 감싼 뒤, 라인 단위로 읽기 위해 BufferedReader로 래핑.
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true); // 소켓 출력 스트림을 UTF-8로 인코딩하는 문자 스트림으로 감싸고, autoFlush=true로 설정한 PrintWriter 생성.

            // 1줄: 닉네임
            this.nickname = in.readLine(); // 클라이언트가 접속 직후 가장 먼저 보낸 닉네임을 한 줄 읽음(프로토콜 규약).
            ChatServer.addClient(nickname, this); // 서버의 전역 레지스트리에 이 클라이언트를 등록하고 초기 상태/방(로비 등)을 세팅.

            String message; // 클라이언트가 보낸 각 메시지를 담을 임시 변수.
            while ((message = in.readLine()) != null) { // 클라이언트가 연결을 끊거나 오류가 나기 전까지 한 줄씩 읽어 처리.
                if ("/quit".equals(message)) break; // 클라이언트가 /quit를 보내면 루프 탈출(연결 종료 의사).

                if (message.startsWith("status:")) { // 상태 변경 프로토콜: "status:활동 중" 또는 "status:자리 비움" 등.
                    ChatServer.updateStatus(nickname, message.substring("status:".length()).trim()); // 접두어를 제거하고 상태 문자열만 추출하여 서버에 반영.

                } else if (message.startsWith("room:create:")) { // 방 생성 프로토콜: "room:create:방이름".
                    String room = message.substring("room:create:".length()).trim(); // 방 이름만 추출.
                    if (!room.isEmpty()) { // 공백/빈 문자열이 아니면
                        ChatServer.createRoom(room); // 서버 전역 방 목록에 방을 생성하고 전체에 목록 갱신 브로드캐스트.
                        // UI에서 즉시 참가+스위치 호출할 것. (원하면 여기서도 join 가능)
                    } // if 끝.

                } else if (message.startsWith("room:join:")) { // 방 참가 프로토콜: "room:join:방이름".
                    String room = message.substring("room:join:".length()).trim(); // 방 이름만 추출.
                    if (!room.isEmpty()) ChatServer.joinRoom(nickname, room); // 유효하면 서버에 참가 처리(멤버십 추가, 활성 방 전환, 알림 전파 등).

                } else if (message.startsWith("room:leave:")) { // 방 나가기 프로토콜: "room:leave:방이름".
                    String room = message.substring("room:leave:".length()).trim(); // 방 이름만 추출.
                    if (!room.isEmpty()) ChatServer.leaveRoom(nickname, room); // 유효하면 서버에 나가기 처리(멤버십 삭제, 알림 전파 등).

                } else if (message.startsWith("room:switch:")) { // 활성 방 전환 프로토콜: "room:switch:방이름".
                    String room = message.substring("room:switch:".length()).trim(); // 방 이름만 추출.
                    if (!room.isEmpty()) ChatServer.switchRoom(nickname, room); // 유효하면 활성 방만 변경(가입 안 되어 있으면 자동 가입 후 전환).

                } else if ("room:list".equals(message)) { // 클라이언트가 초기 스냅샷(목록들)을 요청할 때: "room:list".
                    // 스냅샷 4종
                    sendMessage(ChatServer.getRoomListPayload()); // 현재 존재하는 방 목록을 이 클라이언트에게 전송("roomlist:...").
                    sendMessage(ChatServer.buildAllUsersPayload()); // 전체 접속자/상태 목록 전송("allusers:...").
                    sendMessage(ChatServer.buildMyRoomsPayload(nickname)); // 내가 가입한 방 목록 전송("myrooms:...").
                    sendMessage(ChatServer.buildActiveRoomPayload(nickname)); // 내 활성 방 정보 전송("roomactive:...").
                    sendMessage(ChatServer.buildUserListPayloadForRoom(ChatServer.getActiveRoomOrDefault(nickname))); // 활성 방 참가자 목록 전송("userlist:<room>:...").

                } else { // 위의 어떤 프로토콜에도 해당하지 않으면
                    // 일반 채팅(활성 방으로 전송)
                    ChatServer.broadcastMessage(nickname, message); // 텍스트를 활성 방의 모든 구성원에게 브로드캐스트하고 DB에 저장 시도.
                } // if-else 사다리 끝.
            } // while 끝.
        } catch (IOException e) { // 네트워크/스트림 처리 중 예외 발생 시
            System.err.println(nickname + "와의 연결이 끊겼습니다."); // 해당 사용자와의 연결 끊김을 서버 콘솔에 알림.
        } finally { // 정상 종료든 예외든 마지막에 반드시 실행되는 블록
            if (nickname != null) ChatServer.removeClient(nickname); // 서버 전역 레지스트리에서 이 클라이언트를 제거하고 각 방에 퇴장 알림.
            try { clientSocket.close(); } catch (IOException ignore) {} // 소켓 자원 정리(예외는 무시).
        } // try-catch-finally 끝.
    } // run() 끝.

    /** 서버→클라이언트 전송 */ // 서버 측에서 이 핸들러를 통해 해당 클라이언트에게 한 줄을 보낼 때 사용.
    public void sendMessage(String message) { // 한 줄 메시지를 소켓으로 전송하는 헬퍼 메서드.
        out.println(message); // PrintWriter로 개행 포함 출력(자동 flush 설정으로 즉시 전송됨).
    } // sendMessage 끝.
} // ClientHandler 클래스 끝.
