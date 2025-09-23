package com.example.bridge;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class Bridge {
    private final BlockingQeque<Message> udpToTcp = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message> tcpToUdp = new LinkedBlockingQueue<>();

    private final UdpServer udpServer;
    private final TcpServer tcpServer;

    public Bridge(int udpPort, int tcpPort, InetSocketAddress udpSendTarget) {
        this.udpServer = new UdpServer(udpPort, udpToTcp, tcpToUdp, udpSendTarget);
        this.tcpServer = new TcpServer(tcpPort, udpToTcp, tcpToUdp);
    }
}
