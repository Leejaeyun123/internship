package com.example;

import java.io.BufferedReader; // 입력받은  
import java.io.InputStreamReader; // 입력받은 inputStream을 문자(character) 단위 데이터로 변환시키는 중간다리 역할. 입력한 문자 값을 그대로 출력하기 위해 사용
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

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
