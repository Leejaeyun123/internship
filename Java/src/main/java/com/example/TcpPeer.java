package com.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TcpPeer {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) return;
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try (Socket s = new Socket(host, port);
             BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8)) {

            Thread recv = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) System.out.println(line); // 서버→TCP 출력
                } catch (Exception ignored) {}
            });
            recv.setDaemon(true);
            recv.start();

            String line;
            while ((line = stdin.readLine()) != null) out.println(line);            // TCP → 서버
        }
    }
}
