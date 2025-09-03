// ChatServer.java (수정)
// ...
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ChatServer {

    private static final int PORT = 8000;
    // ✅ 닉네임으로 ClientHandler를 관리하는 Map
    private static final Map<String, ClientHandler> clients = Collections.synchronizedMap(new HashMap<>());
    private static final String DB_URL = "jdbc:mysql://localhost:3306/chat_app?serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "ljy";

    public static void main(String[] args) {
        System.out.println("서버가 " + PORT + "번 포트에서 시작되었습니다.");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("새로운 클라이언트 연결: " + clientSocket);
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 모든 클라이언트에게 메시지 브로드캐스트 (새로운 메서드)
    public static void broadcastMessage(String senderNickname, String message) {
        saveMessageToDb(senderNickname, message);
        for (ClientHandler client : clients.values()) {
            client.sendMessage("chat:" + senderNickname + ": " + message);
        }
    }

    // ✅ 시스템 메시지 전송 및 접속자 목록 업데이트
    public static void broadcastSystemMessage(String message) {
        for (ClientHandler client : clients.values()) {
            client.sendMessage("system:" + message);
        }
        broadcastUserList(); // 접속자 목록 업데이트
    }

    // ✅ 새로운 클라이언트 추가 (ClientHandler에서 호출)
    public static void addClient(String nickname, ClientHandler handler) {
        clients.put(nickname, handler);
        System.out.println(nickname + " 클라이언트 추가됨. 현재 접속자 수: " + clients.size());
        broadcastSystemMessage(nickname + "님이 채팅방에 입장했습니다.");
    }

    // ✅ 클라이언트 제거 (ClientHandler에서 호출)
    public static void removeClient(String nickname) {
        clients.remove(nickname);
        System.out.println(nickname + " 클라이언트 제거됨. 현재 접속자 수: " + clients.size());
        broadcastSystemMessage(nickname + "님이 채팅방을 나갔습니다.");
    }

    // ✅ 현재 접속자 목록을 모든 클라이언트에게 브로드캐스트
    private static void broadcastUserList() {
        StringBuilder userList = new StringBuilder("userlist:");
        for (String nickname : clients.keySet()) {
            userList.append(nickname).append(",");
        }
        if (userList.length() > "userlist:".length()) {
            userList.setLength(userList.length() - 1); // 마지막 쉼표 제거
        }
        for (ClientHandler client : clients.values()) {
            client.sendMessage(userList.toString());
        }
    }

    // ... saveMessageToDb, fetchIdFromDatabase 메서드는 기존 코드 유지 ...
}