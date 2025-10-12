package com.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 사용법: java com.example.TcpPeer <host> <tcpPort>
 * 예시:  java com.example.TcpPeer localhost 5000
 */
public class TcpPeer {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: java com.example.TcpPeer <host> <tcpPort>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try (Socket s = new Socket(host, port);
             BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8)) {

            System.out.println("[TCP] 연결됨: " + host + ":" + port);

            // 수신 스레드
            Thread recv = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (Exception ignored) {}
            });
            recv.setDaemon(true);
            recv.start();

            // 송신 루프 (표준입력 → 서버)
            String line;
            while ((line = stdin.readLine()) != null) {
                out.println(line);
            }
        }
    }
}
