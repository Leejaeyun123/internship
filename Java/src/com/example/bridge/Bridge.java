package com.example.bridge;

import java.net.InetSocketAddress; /* JDK에 포함된 표준 라이브러리. IP+포트를 함께 담는 불변 주소 객체를 가져옴  */
import java.util.concurrent.BlockingQueue; /* //. Thread 안전 Queue의 인터페이스를 가져옴 */
import java.util.concurrent.LinkedBlockingQueue; /* //. 블로킹 Queue 구현을 가져옴 */

/* UDP, TCP는 서로를 모르고 오직 Queue 인터페이스만 봄 */
public class Bridge { /*
                       * 클래스 정의. UDP <-> TCP 중계자. 줄서기 상자(Queue)와 Adapter(UdpServer, TcpServer)를 만들고
                       * 시작/종료만
                       * 지휘
                       */

    /*
     * UDP/TCP에서 들어온 메시지가 TCP/UDP로 흘러가는 통로. Queue = 줄서기 상자(FIFO: 먼저 넣은 게 먼저 나감).
     * Blocking = 비어 있으면 기다림
     */
    private final BlockingQueue<Message> udpToTcp = new LinkedBlockingQueue<>(); /* 변수 선언과 초기화. 객체 생성 */
    private final BlockingQueue<Message> tcpToUdp = new LinkedBlockingQueue<>(); /* // */

    /*
     * 두 필드 선언. Bridge가 UDP/TCP용 Adapter(UdpServer, TcpServer) 객체를 가리키는 참조를 보관.
     * Adapter는 Socket
     * I/O와 Queue 사이를 번역. 참조는 객체의 주소를 담는 변수
     */
    private final UdpServer udpServer; /* 변수 선언. 타입(UdpServer)의 객체( new UdpServer(...) )를 담을 참조를 갖겠다는 선언 */
    private final TcpServer tcpServer; /* //. 타입(TcpServer)의 객체( new TcpServer(...) )를 담을 참조를 갖겠다는 선언 */

    /*
     * Bridge를 만들 때 필요한 환경(포트, TCP<->UDP 송신 대상)을 외부에서 주입받음. 문 여는 번호(포트)와, 편지 보낼 기본
     * 주소(udpSendTarget)를 초기 설정으로 받음
     */
    public Bridge(int udpPort, int tcpPort, InetSocketAddress udpSendTarget) { /* 메서드 정의 : 생성자 */
        this.udpServer = new UdpServer(udpPort, udpToTcp, tcpToUdp, udpSendTarget); /*
                                                                                     * 객체 생성+필드 초기화. udpSendTarget은 UDP는
                                                                                     * 무연결이라 TCP에서 보내는 메시지를 받을 목적지가 필요
                                                                                     */
        this.tcpServer = new TcpServer(tcpPort, udpToTcp, tcpToUdp); /* 객체 생성+필드 초기화 */
    }

    /* 시작 지휘. 실제 네트워크 입출력과 Queue put/take는 Adapter(UdpServer, TcpServer)가 수행 */
    public void start() {
        udpServer.start(); /*
                            * 호출. UDP 서버가 포트에 바인딩("이 IP:포트는 내가 받겠다"를 OS(커널)에 등록")한 뒤, 수신용 Thread와 송신용
                            * Thread를 따로
                            * 돌림.
                            */
        tcpServer.start(); /*
                            * 호출. TCP 서버에서 Listen(TCP에서 연결 대기 상태) 하며 Accept 전용 Thread(동시에 돌아가는 작업 줄기)로 새
                            * 연결을
                            * 받고,
                            * 브로드캐스트 전용 Thread로 UDP에서 온
                            * 메시지를 모든 TCP 클라이언트에게 보냄
                            */
    }

    /*
     * 종료 지휘. Socket을 닫아 I/O 대기를 깨우고, thread.interrupt()로 Queue 대기를 깨운 뒤, Thread가
     * running=false를 확인하고 루프를 종료
     */
    public void stop() {
        udpServer.stop(); /* 호출 */
        tcpServer.stop(); /* 호출 */
    }
}
