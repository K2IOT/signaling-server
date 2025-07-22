package io.rsocket.signaling_server;

import io.rsocket.signaling_server.dto.ChatRequest;
import io.rsocket.signaling_server.dto.ChatResponse;
import io.rsocket.signaling_server.dto.Message;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@ActiveProfiles("test")
class RSocketIntegrationTest {

    private RSocketStrategies strategies;

    @BeforeEach
    void setup() {
        strategies = RSocketStrategies.builder()
                .encoder(new Jackson2JsonEncoder())
                .decoder(new Jackson2JsonDecoder())
                .build();
                
        // Initialize requesters
        createAndInitTcpRequester();
        createAndInitWsRequester();
    }
    
    @AfterEach
    void cleanup() {
        if (tcpRequester != null) {
            tcpRequester.dispose();
        }
        if (wsRequester != null) {
            wsRequester.dispose();
        }
    }



    private RSocketRequester tcpRequester;
    private RSocketRequester wsRequester;
    
    private RSocketRequester createAndInitTcpRequester() {
        if (tcpRequester == null) {
            tcpRequester = RSocketRequester.builder()
                    .rsocketStrategies(strategies)
                    .rsocketConnector(connector -> connector
                        .keepAlive(Duration.ofSeconds(60), Duration.ofSeconds(30)))
                    .transport(TcpClientTransport.create("localhost", 7000));
        }
        return tcpRequester;
    }

    private RSocketRequester createAndInitWsRequester() {
        if (wsRequester == null) {
            wsRequester = RSocketRequester.builder()
                    .rsocketStrategies(strategies)
                    .rsocketConnector(connector -> connector
                        .keepAlive(Duration.ofSeconds(60), Duration.ofSeconds(30))  // Add keepalive
                        .reconnect(Retry.fixedDelay(3, Duration.ofSeconds(2))))  // Add reconnection strategy
                    .transport(WebsocketClientTransport.create(URI.create("ws://localhost:9000/rsocket"))); // Use the correct path
        }
        return wsRequester;
    }


    @Test
    @DisplayName("Setup requesters and test health endpoints")
    void setupRequesters() {
        RSocketRequester tcpRequester = createAndInitTcpRequester();
        RSocketRequester wsRequester = createAndInitWsRequester();

        // Test TCP health endpoint
        Mono<String> tcpHealthResult = tcpRequester
                .route("health")
                .retrieveMono(String.class);

        StepVerifier.create(tcpHealthResult)
                .assertNext(response -> {
                    assertThat(response).contains("RSocket Server is healthy");
                })
                .verifyComplete();

        // Test WebSocket health endpoint
        Mono<String> wsHealthResult = wsRequester
                .route("health")
                .retrieveMono(String.class);

        StepVerifier.create(wsHealthResult)
                .assertNext(response -> {
                    assertThat(response).contains("RSocket Server is healthy");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Test request-response pattern with TCP")
    void testRequestResponseWithTcp() {
        ChatRequest request = new ChatRequest("Hello from TCP test", "TestUser");

        Mono<ChatResponse> result = tcpRequester
                .route("request-response")
                .data(request)
                .retrieveMono(ChatResponse.class);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getResponse()).isEqualTo("Echo: Hello from TCP test");
                    assertThat(response.getServerInfo()).isEqualTo("TCP/WebSocket Server - Request-Response");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Test request-response pattern with WebSocket")
    void testRequestResponseWithWebSocket() {
        ChatRequest request = new ChatRequest("Hello from WebSocket test", "TestUser");

        Mono<ChatResponse> result = wsRequester
                .route("request-response")
                .data(request)
                .retrieveMono(ChatResponse.class);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getResponse()).isEqualTo("Echo: Hello from WebSocket test");
                    assertThat(response.getServerInfo()).isEqualTo("TCP/WebSocket Server - Request-Response");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Test fire-and-forget pattern with TCP")
    void testFireAndForgetWithTcp() {
        RSocketRequester tcpRequester = createAndInitTcpRequester();

        if (tcpRequester == null) {
            setupRequesters();
        }

        Message message = new Message("Test fire-and-forget message", "TCP Client");

        Mono<Void> result = tcpRequester
                .route("fire-and-forget")
                .data(message)
                .retrieveMono(Void.class);

        StepVerifier.create(result)
                .verifyComplete(); // Simply verify that it completes without error
    }

    @Test
    @DisplayName("Test fire-and-forget pattern with WebSocket")
    void testFireAndForgetWithWebSocket() {

        RSocketRequester wsRequester = createAndInitWsRequester();
        if (wsRequester == null) {
            setupRequesters();
        }

        Message message = new Message("Test fire-and-forget message", "WebSocket Client");

        Mono<Void> result = wsRequester
                .route("fire-and-forget")
                .data(message)
                .retrieveMono(Void.class);

        StepVerifier.create(result)
                .verifyComplete(); // Simply verify that it completes without error
    }

    @Test
    @DisplayName("Test request-stream pattern with TCP")
    void testRequestStreamWithTcp() {

        RSocketRequester tcpRequester = createAndInitTcpRequester();

        if (tcpRequester == null) {
            setupRequesters();
        }

        ChatRequest request = new ChatRequest("Stream request", "TCP Client");

        Flux<Message> result = tcpRequester
                .route("request-stream")
                .data(request)
                .retrieveFlux(Message.class);

        StepVerifier.create(result.take(5)) // Just take first 5 messages
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    @DisplayName("Test request-stream pattern with WebSocket")
    void testRequestStreamWithWebSocket() {

        RSocketRequester wsRequester = createAndInitWsRequester();

        if (wsRequester == null) {
            setupRequesters();
        }

        ChatRequest request = new ChatRequest("Stream request", "WebSocket Client");

        Flux<Message> result = wsRequester
                .route("request-stream")
                .data(request)
                .retrieveFlux(Message.class);

        // Verify that we receive 10 messages
        StepVerifier.create(result)
                .expectNextCount(10)
                .verifyComplete();
    }

    @Test
    @DisplayName("Test channel pattern with TCP")
    void testChannelWithTcp() {

        RSocketRequester tcpRequester = createAndInitTcpRequester();

        if (tcpRequester == null) {
            setupRequesters();
        }

        AtomicInteger counter = new AtomicInteger();

        // Create flux of requests
        Flux<ChatRequest> requests = Flux.interval(Duration.ofMillis(300))
                .take(5)
                .map(i -> new ChatRequest(
                        "Channel message " + i,
                        "TCP Client"
                ));

        Flux<ChatResponse> responses = tcpRequester
                .route("channel")
                .data(requests)
                .retrieveFlux(ChatResponse.class);

        StepVerifier.create(responses)
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    @DisplayName("Test channel pattern with WebSocket")
    void testChannelWithWebSocket() {

        RSocketRequester wsRequester = createAndInitWsRequester();

        if (wsRequester == null) {
            setupRequesters();
        }

        // Create flux of requests
        Flux<ChatRequest> requests = Flux.interval(Duration.ofMillis(300))
                .take(5)
                .map(i -> new ChatRequest(
                        "Channel message " + i,
                        "WebSocket Client"
                ));

        Flux<ChatResponse> responses = wsRequester
                .route("channel")
                .data(requests)
                .retrieveFlux(ChatResponse.class);

        List<ChatResponse> responseList = responses.collectList().block(Duration.ofSeconds(10));

        assertThat(responseList).hasSize(5);

        // Verify the response content format
        for (int i = 0; i < responseList.size(); i++) {
            ChatResponse response = responseList.get(i);
            assertThat(response.getResponse()).contains("Channel response to: Channel message " + i);
            assertThat(response.getServerInfo()).isEqualTo("Bidirectional Channel");
        }
    }

    @Test
    @DisplayName("Test connection metadata endpoint")
    void testConnectionInfo() {

        RSocketRequester tcpRequester = createAndInitTcpRequester();

        if (tcpRequester == null) {
            setupRequesters();
        }

        Mono<String> tcpResult = tcpRequester
                .route("connection-info")
                .retrieveMono(String.class);

        StepVerifier.create(tcpResult)
                .assertNext(response -> {
                    assertThat(response).contains("Connected to RSocket Server");
                    assertThat(response).contains("Supports TCP and WebSocket");
                })
                .verifyComplete();
    }
}