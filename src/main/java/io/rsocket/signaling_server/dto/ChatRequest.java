package io.rsocket.signaling_server.dto;


public class ChatRequest {
    private String message;
    private String user;

    // Constructors
    public ChatRequest() {}

    public ChatRequest(String message, String user) {
        this.message = message;
        this.user = user;
    }

    // Getters and Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    @Override
    public String toString() {
        return "ChatRequest{message='" + message + "', user='" + user + "'}";
    }
}