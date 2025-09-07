package com.mycompany.chat;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** 각 클라이언트 연결을 처리 */
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

            // 첫 줄: 닉네임
            this.nickname = in.readLine();
            ChatServer.addClient(nickname, this);

            String message;
            while ((message = in.readLine()) != null) {
                if ("/quit".equals(message)) break;

                try {
                    if (message.startsWith("status:")) {
                        ChatServer.updateStatus(nickname, message.substring("status:".length()).trim());
                    } else if (message.startsWith("room:create:")) {
                        String room = message.substring("room:create:".length()).trim();
                        if (!room.isEmpty()) {
                            ChatServer.createRoom(room);
                            ChatServer.joinRoom(nickname, room);
                        }
                    } else if (message.startsWith("room:join:")) {
                        String room = message.substring("room:join:".length()).trim();
                        if (!room.isEmpty()) ChatServer.joinRoom(nickname, room);
                    } else if (message.equals("room:list")) {
                        // 내게만 방 목록 보내기 (ChatServer의 공개 메서드 사용)
                        out.println(ChatServer.getRoomListPayload());
                    } else {
                        // 일반 채팅
                        ChatServer.broadcastMessage(nickname, message);
                    }
                } catch (Exception ex) {
                    System.err.println("메시지 처리 오류(" + nickname + "): " + ex);
                    ex.printStackTrace();
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
