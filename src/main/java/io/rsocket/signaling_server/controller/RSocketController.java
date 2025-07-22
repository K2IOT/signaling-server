package io.rsocket.signaling_server.controller;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import io.rsocket.signaling_server.dto.ChatRequest;
import io.rsocket.signaling_server.dto.ChatResponse;
import io.rsocket.signaling_server.dto.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class RSocketController {

    private final AtomicInteger messageCounter = new AtomicInteger(0);

    // REQUEST-RESPONSE Pattern
    @MessageMapping("request-response")
    public Mono<ChatResponse> requestResponse(@Payload ChatRequest request) {
        System.out.println("Received request-response: " + request);

        return Mono.just(new ChatResponse(
                "Echo: " + request.getMessage(),
                "TCP/WebSocket Server - Request-Response"
        )).delayElement(Duration.ofMillis(100)); // Simulate processing time
    }

    // FIRE-AND-FORGET Pattern
    @MessageMapping("fire-and-forget")
    public Mono<Void> fireAndForget(@Payload Message message) {
        System.out.println("Received fire-and-forget: " + message);

        // Process message asynchronously (e.g., save to database, send notification)
        return Mono.fromRunnable(() -> {
            // Simulate processing
            System.out.println("Processing message: " + message.getContent());
        }).then();
    }

    // REQUEST-STREAM Pattern
    @MessageMapping("request-stream")
    public Flux<Message> requestStream(@Payload ChatRequest request) {
        System.out.println("Received request-stream: " + request);

        return Flux.interval(Duration.ofSeconds(1))
                .map(i -> new Message(
                        request.getMessage() + " - Stream item " + (i + 1),
                        "Server Stream"
                ))
                .doOnNext(message -> message.setId(UUID.randomUUID().toString()))
                .take(10); // Limit to 10 messages
    }

    // CHANNEL (Bidirectional) Pattern
    @MessageMapping("channel")
    public Flux<ChatResponse> channel(Flux<ChatRequest> requests) {
        System.out.println("Starting bidirectional channel");

        return requests
                .doOnNext(request -> System.out.println("Channel received: " + request))
                .map(request -> new ChatResponse(
                        "Channel response to: " + request.getMessage() +
                                " [" + messageCounter.incrementAndGet() + "]",
                        "Bidirectional Channel"
                ))
                .doOnComplete(() -> System.out.println("Channel completed"))
                .doOnError(error -> System.err.println("Channel error: " + error.getMessage()));
    }

    // Health check endpoint
    @MessageMapping("health")
    public Mono<String> health() {
        return Mono.just("RSocket Server is healthy");
    }

    // Connection metadata
    @MessageMapping("connection-info")
    public Mono<String> connectionInfo() {
        return Mono.just("Connected to RSocket Server - Supports TCP and WebSocket");
    }
}