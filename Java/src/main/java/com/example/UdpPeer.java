package com.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;

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

            // UDP 서버 주소 등록. UDP는 TCP와 다르게 "연결"이 없고, 목적지(IP:PORT)를 매 패킷마다 지정해야 함. 서버는 상대가 먼저 보낸 패킷의 송신자 주소를 보고 "여기로 보내면 된다"를 알 수 있음
            byte[] AddressRegister = "UDP 주소 등록".getBytes(StandardCharsets.UTF_8);
            sock.send(new DatagramPacket(AddressRegister, AddressRegister.length, addr, port));
            
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
