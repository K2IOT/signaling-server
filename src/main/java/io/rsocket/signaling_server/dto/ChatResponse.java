package io.rsocket.signaling_server.dto;


import java.time.LocalDateTime;

public class ChatResponse {
    private String response;
    private String timestamp;
    private String serverInfo;

    // Constructors
    public ChatResponse() {
        this.timestamp = LocalDateTime.now().toString();
    }

    public ChatResponse(String response, String serverInfo) {
        this();
        this.response = response;
        this.serverInfo = serverInfo;
    }

    // Getters and Setters
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getServerInfo() { return serverInfo; }
    public void setServerInfo(String serverInfo) { this.serverInfo = serverInfo; }

    @Override
    public String toString() {
        return "ChatResponse{response='" + response +
                "', timestamp='" + timestamp +
                "', serverInfo='" + serverInfo + "'}";
    }
}