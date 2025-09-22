package com.example.tls;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    private int seqNum;
    private String label;
    private String from;
    private String to;
    private Content content;

    public int getSeqNum() {
        return seqNum;
    }

    public String getLabel() {
        return label;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public Content getContent() {
        return content;
    }
}
