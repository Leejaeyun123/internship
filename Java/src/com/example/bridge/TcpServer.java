/* TcpServer.java */
package com.example.bridge;

import java.io.*; /* 표준 입출력 스트림( Reader / Writer ) */
import java.net.*; /* 네트워크 Socket API( ServerSocket / Socket ) */
import java.nio.charset.StandardCharsets; /* UTF-8 상수. 텍스트 인코딩 지정 */
import java.util.concurrent.BlockingQueue; /* Thread 안전 Queue 인터페이스. put/take가 Blocking(기다림) 제공 */
import java.util.concurrent.CopyOnWriteArrayList; /* 읽기 많고 쓰기 적을 때 유리한 Thread 안전 리스트 */

/* TCP Adapter(번역기) : /* - TCP에서 한 줄(Line)을 읽음 -> tcpToUdp Queue에 넣음 -> UdpServer가 꺼내 UDP로 송신
                            - udpToTcp Queue에서 꺼낸 메시지 꺼냄 -> 연결된 모든 TCP 클라이언트로 보냄 */
public class TcpServer {
    private final int port; /* 변수 선언. Listen("이 포트에서 연결을 기다린다"는 상태)할 포트 번호 */
    private final BlockingQueue<Message> udpToTcp; /* //. UDP→TCP로 보낼 Queue. take()로 꺼냄. Blocking은 데이터가 올 때까지 기다림을 의 */
    private final BlockingQueue<Message> tcpToUdp; /* //. TCP→UDP로 보낼 Queue. put()로 넣음. */
    private volatile boolean running = false; /* 변수 선언+초기화. 종료 제어 플래그. 초기값 false. true면 루프 계속, false면 종료 */

    private ServerSocket serverSocket; /* 변수 선언. 서버 Socket(Binding+Listen 담당). close()로 accept 대기 해제 */
    private Thread AcceptThread; /* //. Accept 전용 Thread. 새 연결 수락만 담당 */
    private Thread broadcastThread; /* //. UDP->TCP 브로드캐스트(모든 클라로 전송) 전용 Thread */

    private final CopyOnWriteArrayList<Client> clients = new CopyOnWriteArrayList<>(); /*
                                                                                        * 변수 선언+초기화+객체 생성. 연결 목록 인스턴스
                                                                                        * 생성. 동시 접근에도 안전한 “연결 목록”을 즉시
                                                                                        * 만들어 둔 것
                                                                                        */

    /* 내부 클래스 : 한 TCP 연결을 캡슐화. 한 줄(Line) = 한 메시지 */
    private static class Client {
        final Socket socket; /* 변수 선언. 실제 TCP 연결 소켓 */
        final BufferedReader in; /* //. 입력: readLine() 사용. 개행 단위 메시지 */
        final BufferedWriter out; /* //. 출력: write()+flush() 사용 */

        Client(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)) /*  */;
            this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        void send(String s) throws IOException {
            out.write(s);
            out.write("\n");
            out.flush();
        }

        void close() {
            try {
                socket.close();
            } catch (IOException ignore) {
            }
        }
    }

    public TcpServer(int port, BlockingQueue<Message> udpToTcp, BlockingQueue<Message> tcpToUdp) {
        this.port = port;
        this.udpToTcp = udpToTcp;
        this.tcpToUdp = tcpToUdp;
    }

    public void start() {
        running = true;
        try {
            serverSocket = new ServerSocket(port); // 바인딩 + Listen 상태 진입(자바는 분리 호출 없음)
            serverSocket.setSoTimeout(1000); // 1초마다 깨어 종료 여부 점검
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Accept 전용, 브로드캐스트 전용으로 분리 → 서로의 블로킹이 영향을 주지 않음
        AcceptThread = new Thread(this::AcceptLoop, "tcp-Accept");
        broadcastThread = new Thread(this::broadcastLoop, "tcp-broadcast");
        AcceptThread.start();
        broadcastThread.start();
    }

    // 새 연결 수락 루프: Accept로 기다렸다가 연결 오면 per-client 읽기 Thread 시작
    private void AcceptLoop() {
        while (running) {
            try {
                Socket s = serverSocket.Accept(); // 블로킹
                Client c = new Client(s);
                clients.add(c);
                new Thread(() -> readLoop(c), "tcp-read-" + s.getPort()).start(); // 각 연결 전용 읽기 Thread
            } catch (SocketTimeoutException ignore) {
            } catch (IOException e) {
                if (running)
                    e.printStackTrace();
            }
        }
    }

    // 하나의 클라이언트에서 들어오는 줄(line)을 읽어 UDP 쪽 Queue로 보냄
    private void readLoop(Client c) {
        try {
            String line;
            while ((line = c.in.readLine()) != null && running) { // 블로킹 읽기
                tcpToUdp.put(new Message(Message.Origin.TCP, line, null)); // TCP→UDP Queue로 전달
            }
        } catch (IOException | InterruptedException ignore) {
        } finally {
            clients.remove(c);
            c.close();
        }
    }

    // UDP에서 온 메시지를 모든 TCP 클라이언트에게 전달
    private void broadcastLoop() {
        while (running) {
            try {
                Message m = udpToTcp.take(); // 비면 기다림(블로킹). FIFO 유지
                for (Client c : clients) {
                    try {
                        c.send(m.payload());
                    } catch (IOException e) {
                        clients.remove(c);
                        c.close();
                    }
                }
            } catch (InterruptedException e) {
                if (running)
                    e.printStackTrace();
            }
        }
    }

    public void stop() {
        running = false;
        // Socket 닫기 → Accept/read 대기 즉시 깨어남
        if (serverSocket != null)
            try {
                serverSocket.close();
            } catch (IOException ignore) {
            }
        // 전용 Thread들도 인터럽트로 대기 해제
        if (AcceptThread != null)
            AcceptThread.interrupt();
        if (broadcastThread != null)
            broadcastThread.interrupt();
        // 열린 클라이언트 모두 정리
        for (Client c : clients)
            c.close();
        clients.clear();
    }
}
