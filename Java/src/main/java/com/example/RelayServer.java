package com.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * 사용법: java com.example.RelayServer <tcpPort> <udpPort>
 * 예시:  java com.example.RelayServer 5000 6000
 *
 * 단순화 가정:
 *  - TCP는 "가장 최근 접속 1명"만 상대합니다.
 *  - UDP는 "가장 최근에 서버로 패킷을 보낸 주소 1곳"만 상대합니다.
 *  - TCP→UDP 전달은 UDP 피어가 한 번 이상 보낸 뒤(등록된 뒤)에 가능합니다.
 */
public class RelayServer {
    private volatile PrintWriter currentTcpOut;     // 최근 TCP 클라이언트에게 쓰기
    private volatile SocketAddress lastUdpPeer;     // 최근 UDP 보낸 쪽 주소

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: java com.example.RelayServer <tcpPort> <udpPort>");
            return;
        }
        int tcpPort = Integer.parseInt(args[0]);
        int udpPort = Integer.parseInt(args[1]);

        new RelayServer().run(tcpPort, udpPort);
    }

    private void run(int tcpPort, int udpPort) throws Exception {
        ServerSocket tcpServer = new ServerSocket(tcpPort);
        DatagramSocket udpSock = new DatagramSocket(udpPort);
        System.out.println("[Relay] TCP " + tcpPort + ", UDP " + udpPort + " 대기");

        // UDP 수신 스레드: 들어오는 UDP를 최근 TCP 클라이언트로 전달
        Thread udpThread = new Thread(() -> {
            byte[] buf = new byte[4096];
            DatagramPacket p = new DatagramPacket(buf, buf.length);
            try {
                while (true) {
                    udpSock.receive(p); // 신호 없으면 대기
                    lastUdpPeer = p.getSocketAddress(); // 최근 UDP 피어 등록/갱신
                    String s = new String(p.getData(), p.getOffset(), p.getLength(), StandardCharsets.UTF_8);
                    PrintWriter out = currentTcpOut;
                    if (out != null) {
                        out.println("[UDP→TCP] " + s);
                    }
                }
            } catch (Exception e) {
                System.err.println("[Relay] UDP error: " + e.getMessage());
            }
        });
        udpThread.start();

        // TCP accept 루프: 최근 접속 1명만 상대, 그 클라에서 오는 라인은 최근 UDP 피어로 전달
        while (true) {
            try (Socket s = tcpServer.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8)) {

                currentTcpOut = out;
                System.out.println("[Relay] TCP 연결: " + s.getRemoteSocketAddress());

                String line;
                while ((line = in.readLine()) != null) {
                    // TCP → UDP
                    if (lastUdpPeer != null) {
                        byte[] data = line.getBytes(StandardCharsets.UTF_8);
                        DatagramPacket toUdp = new DatagramPacket(data, data.length, lastUdpPeer);
                        udpSock.send(toUdp);
                    }
                    // (선택) TCP 쪽 에코
                    out.println("[TCP→UDP] " + line);
                }
            } catch (Exception e) {
                System.err.println("[Relay] TCP error: " + e.getMessage());
            } finally {
                currentTcpOut = null;
            }
        }
    }
}
