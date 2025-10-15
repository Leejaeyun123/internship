// TCP↔UDP 브리지를 블로킹 I/O(입력 없으면 대기)와 두 스레드(동시에 실행)로 구현. 각 줄은 네트워킹 기본 동작(리스닝/수락/수신/송신), 문자 인코딩/디코딩, 스레드 간 상태 공유(volatile/스냅샷), 라인 기반 메시징을 성립시키기 위해 필수 역할을 담당
// TCP는 메인 스레드(프로세스 시작 시 만들어지는 첫 실행 흐름), UDP는 보조 스레드(추가로 만든 실행 흐름)
// 하나의 스레드는 하나의 블로킹 호출(accept() or receive())만 잡을 수 있으므로, 두 곳을 동시에 기다리기 위해서 스레드를 두 개 만듦 

package com.example;

import java.io.BufferedReader; // 문자 입력을 내부 버퍼(임시 저장소)에 모아 효율적으로 읽고, readLine()을 제공
import java.io.InputStreamReader; // InputStreamReader(어댑터: 서로 다른 타입 연결 변환기 / 디코딩: 바이트→문자 해석)가 있어야 TCP 바이트 스트림을 문자 스트림으로 바꿔 readLine()을 쓸 수 있음
import java.io.PrintWriter; // PrintWriter(문자 출력 Writer / println() 제공 / autoFlush(자동 플러시: 특정 시점에 버퍼 자동 비움))로 라인 단위 즉시 전송을 하려면 필요
import java.net.*; // ServerSocket(리스닝 소켓: TCP 연결 요청 대기), Socket(세션: 수락된 개별 TCP 연결), DatagramSocket(UDP 소켓), DatagramPacket(UDP 패킷 컨테이너), SocketAddress(소켓 주소 표현) 를 쓰기 위해 필요. 이 중 하나라도 없으면 네트워킹 X
import java.nio.charset.StandardCharsets; // StandardCharsets.UTF_8(문자셋 상수: 인코딩/디코딩 규칙)을 명시해 플랫폼에 따른 기본 인코딩 차이를 제거

public class RelayServer {
    private volatile PrintWriter currentTcpOut; // UDP→TCP 중계의 출구. volatile(가시성: 여러 스레드가 최신 값을 즉시 보게 보장)를 붙이지 않으면 UDP 수신 스레드가 낡은 참조를 볼 수 있음
    private volatile SocketAddress lastUdpPeer; // TCP→UDP 중계의 목적지. UDP는 연결(세션) 개념이 없기 때문에 “어디로 보낼지”를 매번 주소로 지정(volatile 사용)

    public static void main(String[] args) throws Exception {
        if (args.length != 2) return;   // 조기 종료 조건. 포트 2개(tcpPort, udpPort)가 없으면 소켓을 만들지 않도록 막음
        RelayServer server = new RelayServer(); // run은 인스턴스 메서드(객체에 귀속). static(객체 없이 호출)이 아니므로 객체 생성이 필요
        int tcpPort = Integer.parseInt(args[0]);
        int udpPort = Integer.parseInt(args[1]);
        server.run(tcpPort, udpPort); // 인자 문자열을 정수로 변환(파싱, 문자 -> 숫자) 후 run 메서드로 넘겨 서버 시작
    }

    private void run(int tcpPort, int udpPort) throws Exception {   // 서버 구동 로직과 두 소켓 준비를 수행하는 메서드
        ServerSocket tcpServer = new ServerSocket(tcpPort); // TCP 리스닝(listening: 연결 요청 대기) 소켓을 바인딩(binding: OS에 포트 등록). 이게 없으면 TCP 클라이언트를 수락(accept) X
        DatagramSocket udpSock = new DatagramSocket(udpPort);   // UDP 소켓을 바인딩. 이게 없으면 UDP 패킷 수신/송신 X

        // UDP 수신 전담 스레드. 별도 스레드를 쓰는 이유는 TCP 처리와 UDP 수신을 동시에 처리하기 위함
        Thread udpRecv = new Thread(() -> { // UDP 수신 전담 스레드 
            byte[] buf = new byte[4096];    // 수신 버퍼(임시 메모리 공간). 크기는 4096바이트
            DatagramPacket pkt = new DatagramPacket(buf, buf.length); // 수신 컨테이너(데이터+길이+주소). receive가 호출되면 이 객체에 데이터/길이/발신자 주소가 채워짐(버퍼 재사용)
            try {   // 수신 루프 내에서 발생하는 예외를 잡음
                while (true) {  // 서버가 살아 있는 동안 무한 루프로 수신
                    udpSock.receive(pkt);   // 수신 블로킹 호출. “신호 없으면 대기”의 핵심. 이 한 줄이 없으면 패킷을 받지 못합
                    lastUdpPeer = pkt.getSocketAddress();   // 가장 최근 UDP 발신자 주소(IP:포트)를 기록. TCP -> UDP로 보낼 때 목적지로 사용. 가장 최근 한 곳만 유지하는 구조
                    String msg = new String(pkt.getData(), pkt.getOffset(), pkt.getLength(), StandardCharsets.UTF_8);   // 수신 바이트를 문자열로 디코딩(바이트→문자 해석)
                    PrintWriter out = currentTcpOut;    // 현재 등록된 TCP 출력 스트림(현재 접속 클라이언트로 보내는 통로)을 스냅샷(특정 시점의 상태를 그대로 복사)으로 가져옴
                    if (out != null)     
                        out.println(msg);   //  현재 TCP 클라이언트가 있을 때만 UDP->TCP 전달을 수행. 없으면 드롭. 이 한 줄이 없으면 UDP→TCP 브리지(두 프로토콜 사이 중계 경로)가 사라짐(TCP→UDP는 계속 동작하지만, UDP→TCP는 더 이상 전송되지 않음)
                }
            } catch (Exception ignored) {
            }   // 예외 처리에 관한 문장이 없으므로 try-catch를 빠져 나와 다음 문장을 실행. UDP 수신 스레드만 죽고, TCP 처리 루프(메인 스레드)는 계속 살아 있음 
        });
        udpRecv.setDaemon(true);    // 아직 이해 불가
        udpRecv.start();    // new Thread(...)까지는 스레드 객체만 만든 상태라 start()가 있어야 실제로 실행

        //  TCP 처리 루프(메인 스레드. JVM이 프로그램 시작 시 자동으로 만들어 둔 기본 스레드)
        while (true) {  // 여러 TCP 클라이언트를 순차적으로 처리하려면 필요
            try (Socket sock = tcpServer.accept(); // accept: 연결 요청 수락. 신호 없으면 대기
                    BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));  // TCP 입력을 문자 스트림으로 변환(어댑터/디코딩)하고 라인 단위로 읽기 위해 필요
                    PrintWriter out = new PrintWriter(sock.getOutputStream(), true, StandardCharsets.UTF_8)) {  // 라인 단위 전송 + autoFlush 를 위해 필요

                currentTcpOut = out;    // UDP 수신 스레드가 쓸 “현재 TCP 출구” 등록. 이게 없으면 UDP→TCP 전달 X

                String line;
                while ((line = in.readLine()) != null) {    // 클라이언트가 보낸 한 줄을 읽어 메시지로 처리. 없으면 TCP→UDP 전달 X
                    if (lastUdpPeer != null) {  // 목적지 검증. 등록된 UDP 피어가 없다면 보낼 곳 X.
                        byte[] data = line.getBytes(StandardCharsets.UTF_8);    // 문자열을 인코딩(문자→바이트 변환) 해 UDP로 보낼 수 있게 함
                        udpSock.send(new DatagramPacket(data, data.length, lastUdpPeer)); // TCP→UDP 송신. 이 한 줄이 없으면 TCP→UDP 브리지가 사라짐
                    }
                }
            } finally {
                currentTcpOut = null;   // 연결 종료 시 출구 해제를 해야, UDP 스레드가 닫힌 스트림으로 쓰려다 예외를 내지 않음. 오래된 출구로 오발송을 막음
            }
        }
    }
}
