package com.example.tls;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.security.cert.Extension;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Content {
    private String contentType;
    private String version;
    private String recordLen;
    private String handshakeType;
    private String handshakeLen;
    private String protocol;
    private List<String> cipherSuites;
    private String cipherSuitesLen;
    private String random;
    private String sessionID;
    private String sessionIDLen;
    private String compression;
    private Extension extension;

    public String getContentType() {
        return contentType;
    }

    public String getVersion() {
        return version;
    }

    public String getRecordLen() {
        return recordLen;
    }

    public String getHandshakeType() {
        return handshakeType;
    }

    public String getHandshakeLen() {
        return handshakeLen;
    }

    public String getProtocol() {
        return protocol;
    }

    public List<String> getCipherSuites() {
        return cipherSuites;
    }

    public String getCipherSuitesLen() {
        return cipherSuitesLen;
    }

    public String getRandom() {
        return random;
    }

    public String getSessionID() {
        return sessionID;
    }

    public String getSessionIDLen() {
        return sessionIDLen;
    }

    public String getCompression() {
        return compression;
    }

    public Extension getExtension() {
        return extension;
    }

}
