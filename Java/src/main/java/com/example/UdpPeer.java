package com.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * 사용법: java com.example.UdpPeer <serverHost> <serverUdpPort>
 * 예시:  java com.example.UdpPeer localhost 6000
 *
 * 최초에 아무 문자 한 줄 보내면 서버가 이 소켓 주소를 기억하고,
 * 이후 TCP→UDP 메시지를 이 주소로 보내줍니다.
 */
public class UdpPeer {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: java com.example.UdpPeer <serverHost> <serverUdpPort>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        InetAddress addr = InetAddress.getByName(host);

        try (DatagramSocket sock = new DatagramSocket();
             BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            System.out.println("[UDP] 로컬 " + sock.getLocalAddress() + ":" + sock.getLocalPort());

            // 수신 스레드
            Thread recv = new Thread(() -> {
                byte[] buf = new byte[4096];
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                try {
                    while (true) {
                        sock.receive(p);
                        String s = new String(p.getData(), p.getOffset(), p.getLength(), StandardCharsets.UTF_8);
                        System.out.println("[UDP RECV] " + s);
                    }
                } catch (Exception ignored) {}
            });
            recv.setDaemon(true);
            recv.start();

            // 송신 루프 (표준입력 → 서버)
            String line;
            while ((line = stdin.readLine()) != null) {
                byte[] data = line.getBytes(StandardCharsets.UTF_8);
                DatagramPacket p = new DatagramPacket(data, data.length, addr, port);
                sock.send(p);
            }
        }
    }
}
