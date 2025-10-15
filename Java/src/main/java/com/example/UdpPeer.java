package com.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class UdpPeer {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) return;   // 조기 종료 조건. host, port가 없으면 잘못된 상태로 진행하지 않도록 즉시 종료
        InetAddress host = InetAddress.getByName(args[0]);  // 문자열 호스트명을 주소 객체로 해석(이름 -> IP)
        int port = Integer.parseInt(args[1]);   // 인자 문자열을 정수로 변환(파싱, 문자 -> 숫자)

        try (DatagramSocket sock = new DatagramSocket();    // UDP 소켓 생성. 바인딩(포트 등록)을 안 하면 OS가 자동으로 사용 가능한 포트를 골라 바인딩
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
