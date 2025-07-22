package io.rsocket.signaling_server.exception;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RSocketExceptionHandler {

    @MessageExceptionHandler
    public Mono<String> handleException(Exception ex) {
        System.err.println("RSocket Exception: " + ex.getMessage());
        return Mono.just("Error: " + ex.getMessage());
    }

    @MessageExceptionHandler(IllegalArgumentException.class)
    public Mono<String> handleIllegalArgument(IllegalArgumentException ex) {
        return Mono.just("Invalid argument: " + ex.getMessage());
    }
}