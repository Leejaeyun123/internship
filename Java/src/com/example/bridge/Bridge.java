package com.example.bridge;

import java.net.InetSocketAddress; /* JDK에 포함된 표준 라이브러리. IP+포트를 함께 담는 불변 주소 객체를 가져옴  */
import java.util.concurrent.BlockingQueue; /* //. 스레드 안전 큐의 인터페이스를 가져옴 */
import java.util.concurrent.LinkedBlockingQueue; /* //. 블로킹 큐 구현을 가져옴 */

/* UDP, TCP는 서로를 모르고 오직 Queue 인터페이스만 봄 */
public class Bridge { /* 클래스 정의. UDP <-> TCP 중계자. */

    /* UDP/TCP에서 들어온 메시지가 TCP/UDP로 흘러가는 통로 */
    private final BlockingQueue<Message> udpToTcp = new LinkedBlockingQueue<>(); /* 변수 선언과 초기화. 객체 생성 */
    private final BlockingQueue<Message> tcpToUdp = new LinkedBlockingQueue<>(); /* // */

    /*
     * 두 필드 선언. Bridge가 UDP/TCP용 어댑터(UdpServer, TcpServer) 객체를 가리키는 참조를 보관. 어댑터는 소켓
     * I/O와 Queue 사이를 번역
     */
    private final UdpServer udpServer; /* 변수 선언. 타입(UdpServer)의 객체( new UdpServer(...) )를 담을 자리(참조)를 갖겠다는 선언 */
    private final TcpServer tcpServer; /* //. 타입(TcpServer)의 객체( new TcpServer(...) )를 담을 자리(참조)를 갖겠다는 선언 */

    /* Bridge를 만들 때 필요한 환경(포트, TCP<->UDP 송신 대상)을 외부에서 주입받음. */
    public Bridge(int udpPort, int tcpPort, InetSocketAddress udpSendTarget) { /* 메서드 정의 : 생성자 */
        this.udpServer = new UdpServer(udpPort, udpToTcp, tcpToUdp, udpSendTarget); /*
                                                                                     * 객체 생성+필드 초기화. udpSendTarget은 UDP는
                                                                                     * 무연결이라 TCP에서 보내는 메시지를 받을 목적지가 필요
                                                                                     */
        this.tcpServer = new TcpServer(tcpPort, udpToTcp, tcpToUdp); /* 객체 생성+필드 초기화 */
    }

    public void start() { /* 메서드 정의 */
        udpServer.start(); /* 호출. UDP 서버가 포트에 바인딩한 뒤, 수신용 스레드와 송신용 스레드를 따로 돌림. */
        tcpServer.start(); /*
                            * 호출. TCP 서버에서 리슨(listen) 하며 accept 전용 스레드로 새 연결을 받고, 브로드캐스트 전용 스레드로 UDP에서 온
                            * 메시지를 모든 TCP 클라이언트에게 보냄
                            */
    }

    public void stop() {
        udpServer.stop();
        tcpServer.stop();
    }
}
