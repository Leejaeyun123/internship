package com.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class UdpPeer {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) return;
        InetAddress host = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);

        try (DatagramSocket sock = new DatagramSocket();
             BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            byte[] hello = "UDP 주소 등록".getBytes(StandardCharsets.UTF_8);    // 등록용 1회 전송
            sock.send(new DatagramPacket(hello, hello.length, host, port));

            Thread recv = new Thread(() -> {
                byte[] buf = new byte[4096];
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                try {
                    while (true) {
                        sock.receive(p);
                        String s = new String(p.getData(), p.getOffset(), p.getLength(), StandardCharsets.UTF_8);
                        System.out.println(s);                           // 서버→UDP 출력
                    }
                } catch (Exception ignored) {}
            });
            recv.setDaemon(true);
            recv.start();

            String line;
            while ((line = stdin.readLine()) != null) {
                byte[] data = line.getBytes(StandardCharsets.UTF_8);
                sock.send(new DatagramPacket(data, data.length, host, port)); // UDP → 서버
            }
        }
    }
}
