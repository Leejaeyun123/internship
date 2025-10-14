package com.example;

import java.io.BufferedReader; // 문자 입력을 내부 버퍼(임시 저장소)에 모아 효율적으로 읽고, readLine을 제공하는 리더
import java.io.InputStreamReader; // 바이트 입력을 지정 문자셋으로 디코딩해 문자로 바꿔주는 어댑터
import java.io.PrintWriter; // 문자 출력용 Writer, println/autoFlush 제공
import java.net.*; // java.net 패키지(ServerSocket, Socket, DatagramSocket, DatagramPacket, SocketAddress)의 여러 타입을 한 번에 가져옴
import java.nio.charset.StandardCharsets; // 바이트↔문자 변환 시 UTF-8 상수를 사용(플랫폼 독립, 오타 방지)

public class RelayServer {
    private volatile PrintWriter currentTcpOut; // 현재 접속한 TCP 클라이언트로 출력하는 통로(출력 스트림)를 보관. volatile로 선언하여, UDP 수신 스레드가 최신 스트림을 볼 수 있게 함
    private volatile SocketAddress lastUdpPeer; // 마지막으로 서버에 데이터를 보낸 UDP 송신자 주소를 저장. volatile로 선언하여, TCP 처리 스레드가 최신 주소를 즉시 볼 수 있게 함

    public static void main(String[] args) throws Exception {
        if (args.length != 2) return; // 프로그램 인자(실행 시 전달되는 문자열 배열)가 2개가 아니면 바로 종료. 기대 인자 : tcpPort(5000), udpPort(6000)
        RelayServer server = new RelayServer();
        int tcpPort = Integer.parseInt(args[0]);
        int udpPort = Integer.parseInt(args[1]);
        server.run(tcpPort, udpPort); // 인자 문자열을 정수로 변환(파싱, 문자 -> 숫자) 후 run 메서드로 넘겨 서버 시작
    }

    private void run(int tcpPort, int udpPort) throws Exception {   // 실제 서버 구동 로직을 수행하는 메서드
        ServerSocket tcpServer = new ServerSocket(tcpPort); // ServerSocket(클라이언트의 요청을 받기 위한 준비)을 생성해 tcpPort에 바인딩(OS에 ‘이 포트를 내가 쓰겠다’ 등록). accept(새 TCP 연결을 수락하는 블로킹 호출—입력(연결)이 올 때까지 멈춤) 를 호출할 수 있음
        DatagramSocket udpSock = new DatagramSocket(udpPort);   // DatagramSocket을 생성해 udpPort에 바인딩

        Thread udpRecv = new Thread(() -> { // UDP 수신 전담 스레드를 만듦. 별도 스레드를 쓰는 이유는 TCP 처리와 UDP 수신을 동시에 처리하기 위함 
            byte[] buf = new byte[4096];    // 수신 데이터 임시 저장 버퍼(임시 메모리 공간). 크기는 4096바이트
            DatagramPacket pkt = new DatagramPacket(buf, buf.length); // 수신용 DatagramPacket(UDP 패킷 데이터+길이+주소를 담는 객체). receive가 호출되면 이 객체에 데이터 내용/길이/발신자 주소가 채워짐(버퍼 재사용)
            try {   // 수신 루프 내에서 발생하는 예외를 잡음
                while (true) {  // 서버가 살아 있는 동안 무한 루프로 수신
                    udpSock.receive(pkt);   // UDP 소켓이 해당 포트로 들어오는 패킷을 받을 때까지 대기(블로킹 : 입력이 올 때까지 멈춤). 패킷이 오면 커널이 그 내용을 buf[0]부터 buf[pkt.getLength()-1]까지 덮어씀 ex) "hello"(5byte)가 왔다면 pkt.getLength() == 5, buf[0..4]가 h e l l o로 바뀌고 buf[5..4095]는 변함 없음
                    lastUdpPeer = pkt.getSocketAddress();   // 가장 최근 UDP 발신자 주소(IP:포트)를 기록. TCP -> UDP로 보낼 때 목적지로 사용. 가장 최근 한 곳만 유지하는 구조
                    String msg = new String(pkt.getData(), pkt.getOffset(), pkt.getLength(), StandardCharsets.UTF_8);   // pkt 버퍼에서 실제 수신 길이만큼 꺼내 UTF-8(유니코드 인코딩 표준)로 디코딩(바이트 → 문자 해석) 하여 문자열을 만듦. 
                    PrintWriter out = currentTcpOut;    // 현재 등록된 TCP 출력 스트림(현재 접속 클라이언트로 보내는 통로)을 스냅샷(특정 시점의 상태를 그대로 복사)으로 가져옴
                    if (out != null)    //  TCP 클라이언트가 연결되어 있으면 한 줄 출력. 
                        out.println(msg); // UDP → TCP
                }
            } catch (Exception ignored) {
            }
        });
        udpRecv.setDaemon(true);
        udpRecv.start();

        while (true) {
            try (Socket sock = tcpServer.accept(); // 신호 없으면 대기
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
                    PrintWriter out = new PrintWriter(sock.getOutputStream(), true, StandardCharsets.UTF_8)) {

                currentTcpOut = out;

                String line;
                while ((line = in.readLine()) != null) {
                    if (lastUdpPeer != null) {
                        byte[] data = line.getBytes(StandardCharsets.UTF_8);
                        udpSock.send(new DatagramPacket(data, data.length, lastUdpPeer)); // TCP → UDP
                    }
                }
            } finally {
                currentTcpOut = null;
            }
        }
    }
}
