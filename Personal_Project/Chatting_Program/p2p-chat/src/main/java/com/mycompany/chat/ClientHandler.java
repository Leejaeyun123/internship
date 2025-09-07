package com.mycompany.chat;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** 각 클라이언트 소켓을 처리 */
public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private String nickname;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);

            // 1줄: 닉네임
            this.nickname = in.readLine();
            ChatServer.addClient(nickname, this);

            String message;
            while ((message = in.readLine()) != null) {
                if ("/quit".equals(message)) break;

                if (message.startsWith("status:")) {
                    ChatServer.updateStatus(nickname, message.substring("status:".length()).trim());

                } else if (message.startsWith("room:create:")) {
                    String room = message.substring("room:create:".length()).trim();
                    if (!room.isEmpty()) {
                        ChatServer.createRoom(room);
                        // UI에서 즉시 참가+스위치 호출할 것. (원하면 여기서도 join 가능)
                    }

                } else if (message.startsWith("room:join:")) {
                    String room = message.substring("room:join:".length()).trim();
                    if (!room.isEmpty()) ChatServer.joinRoom(nickname, room);

                } else if (message.startsWith("room:leave:")) {
                    String room = message.substring("room:leave:".length()).trim();
                    if (!room.isEmpty()) ChatServer.leaveRoom(nickname, room);

                } else if (message.startsWith("room:switch:")) {
                    String room = message.substring("room:switch:".length()).trim();
                    if (!room.isEmpty()) ChatServer.switchRoom(nickname, room);

                } else if ("room:list".equals(message)) {
                    // 스냅샷 4종
                    sendMessage(ChatServer.getRoomListPayload());
                    sendMessage(ChatServer.buildAllUsersPayload());
                    sendMessage(ChatServer.buildMyRoomsPayload(nickname));
                    sendMessage(ChatServer.buildActiveRoomPayload(nickname));
                    sendMessage(ChatServer.buildUserListPayloadForRoom(ChatServer.getActiveRoomOrDefault(nickname)));

                } else {
                    // 일반 채팅(활성 방으로 전송)
                    ChatServer.broadcastMessage(nickname, message);
                }
            }
        } catch (IOException e) {
            System.err.println(nickname + "와의 연결이 끊겼습니다.");
        } finally {
            if (nickname != null) ChatServer.removeClient(nickname);
            try { clientSocket.close(); } catch (IOException ignore) {}
        }
    }

    /** 서버→클라이언트 전송 */
    public void sendMessage(String message) {
        out.println(message);
    }
}
