package com.mycompany.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * 이 클래스는 각 클라이언트의 연결을 처리하는 Runnable입니다.
 * 클라이언트로부터 메시지를 수신하고, 메시지 형식을 파싱하여 적절하게 처리합니다.
 */
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
            // 클라이언트 소켓으로부터 입력 및 출력 스트림을 설정합니다.
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // 클라이언트로부터 닉네임을 읽고 서버에 추가합니다.
            this.nickname = in.readLine();
            ChatServer.addClient(nickname, this);
            
            String message;
            // 클라이언트로부터 메시지를 지속적으로 읽습니다.
            while ((message = in.readLine()) != null) {
                // "/quit" 메시지를 받으면 루프를 종료하여 클라이언트 연결을 끊습니다.
                if (message.equals("/quit")) {
                    break;
                }
                // 받은 메시지를 모든 다른 클라이언트에게 브로드캐스트합니다.
                ChatServer.broadcastMessage(nickname, message);
            }
        } catch (IOException e) {
            // 연결이 끊겼을 때 오류를 처리합니다.
            System.err.println(nickname + "와의 연결이 끊겼습니다.");
        } finally {
            // 클라이언트 연결이 종료되면 서버 맵에서 클라이언트를 제거하고 소켓을 닫습니다.
            if (nickname != null) {
                ChatServer.removeClient(nickname);
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 클라이언트에게 메시지를 보냅니다.
     * @param message 보낼 메시지
     */
    public void sendMessage(String message) {
        out.println(message);
    }
}
