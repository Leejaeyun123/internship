//  각 클라이언트의 소켓 통신을 담당

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private int senderId; // DB에 저장하기 위한 발신자 ID

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        try {
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            // TODO: 클라이언트로부터 로그인 정보를 받아 senderId 설정
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        String message;
        try {
            while ((message = in.readLine()) != null) {
                System.out.println("클라이언트로부터 받은 메시지: " + message);
                // 서버의 broadcastMessage 메소드 호출
                ChatServer.broadcastMessage(message, this.senderId); 
            }
        } catch (IOException e) {
            System.err.println("클라이언트 연결이 종료되었습니다.");
        } finally {
            try {
                if (clientSocket != null) clientSocket.close();
                // TODO: 클라이언트 연결 종료 시 clients 리스트에서 제거
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void sendMessage(String message) {
        out.println(message);
    }
}