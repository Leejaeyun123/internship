package com.example;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class RelayServer {
    private volatile PrintWriter currentTcpOut;     // 최근 TCP 클라이언트에게 쓰기
    private volatile SocketAddress lastUdpPeer;     // 최근 UDP 보낸 쪽 주소

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: java com.example.RelayServer <tcpPort> <udpPort>");
            return;
        }
        int tcpPort = Integer.parseInt(args[0]); // args[0] = launch.json의 args의 첫 번째 인자 = "5000"
        int udpPort = Integer.parseInt(args[1]); // args[1] = launch.json의 args의 두 번째 인자 = "6000"

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
