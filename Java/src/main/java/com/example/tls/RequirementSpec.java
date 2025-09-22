package com.example.tls;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RequirementSpec {
    @JsonProperty("Title")
    private String title;
    @JsonProperty("NodeNum")
    private int nodeNum;
    @JsonProperty("NodeId")
    private List<String> nodeId;
    @JsonProperty("MessageSequence")
    private List<Message> messageSequence;

    public String getTitle() {
        return title;
    }

    public int getNodeNum() {
        return nodeNum;
    }

    public List<String> getNodeId() {
        return nodeId;
    }

    public List<Message> getMessageSequence() {
        return messageSequence;
    }
}
