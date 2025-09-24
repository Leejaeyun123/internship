package com.example.bridge;

import java.net.InetSocketAddress;

public class Message {
    public enum Origin {
        UDP, TCP
    }

    private final Origin origin;
    private final String payload;
    private final InetSocketAddress udpSender;

    public Message(Origin origin, String payload, InetSocketAddress udpSender) {
        this.origin = origin;
        this.payload = payload;
        this.udpSender = udpSender;
    }

    public Origin origin() {
        return origin;
    }

    public String payload() {
        return payload;
    }

    public InetSocketAddress udpSender() {
        return udpSender;
    }
}
