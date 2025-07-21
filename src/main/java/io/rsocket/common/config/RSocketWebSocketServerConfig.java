package io.rsocket.common.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;

import io.rsocket.SocketAcceptor;
import io.rsocket.common.config.condition.RSocketWebSocketServerCondition;
import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import reactor.netty.http.server.HttpServer;

@Conditional({ RSocketWebSocketServerCondition.class })
@Configuration
public class RSocketWebSocketServerConfig implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger logger = LoggerFactory.getLogger(RSocketWebSocketServerConfig.class);

    @Value("${rsocket.websocket.host:localhost}")
    private String host;

    @Value("${rsocket.websocket.port:9000}")
    private int port;

    @Value("${rsocket.websocket.max-frame-size:16777215}")
    private int maxFrameSize;

    @Value("${rsocket.websocket.setup-timeout:30000}") // 30 seconds
    private long setupTimeout;

    @Value("${rsocket.websocket.path:/rsocket}")
    private String websocketPath;


    private final ApplicationContext applicationContext;

    private CloseableChannel webSocketServerChannel;

    @Bean
    public RSocketStrategies webSocketRSocketStrategies() {
        return RSocketStrategies.builder()
                .encoder(new Jackson2JsonEncoder())
                .decoder(new Jackson2JsonDecoder())
                .build();
    }

    @Bean
    public SocketAcceptor webSocketSocketAcceptor(RSocketMessageHandler webSocketRSocketMessageHandler) {
        return webSocketRSocketMessageHandler.responder();
    }

    @Bean
    public RSocketMessageHandler webSocketRSocketMessageHandler(RSocketStrategies webSocketRSocketStrategies) {
        RSocketMessageHandler messageHandler = new RSocketMessageHandler();
        messageHandler.setRSocketStrategies(webSocketRSocketStrategies);
        return messageHandler;
    }

    private void startWebSocketServer(SocketAcceptor socketAcceptor) {

        // TODO: Config prefix for /rsocket path

        logger.info("Starting RSocket WebSocket Server on {}:{}{}", host, port, websocketPath);

        HttpServer httpServer = HttpServer.create()
                .host(host)
                .port(port)
                .doOnConnection(connection -> {
                    logger.info("WebSocket Client connected: {}", connection.channel().remoteAddress());
                });

        webSocketServerChannel = RSocketServer.create(socketAcceptor)
                .payloadDecoder(PayloadDecoder.ZERO_COPY)
                .maxInboundPayloadSize(maxFrameSize)
                .bind(
                        WebsocketServerTransport.create(httpServer))
                .doOnSuccess(closeableChannel -> {
                    logger.info("RSocket WebSocket Server started successfully on {}:{}{}",
                            host, port, websocketPath);
                })
                .doOnError(throwable -> {
                    logger.error("Failed to start RSocket WebSocket Server", throwable);
                })
                .block(Duration.ofMillis(setupTimeout));
    }

    public void shutdown() {
        logger.info("Shutting down RSocket WebSocket Server...");
        if (webSocketServerChannel != null && !webSocketServerChannel.isDisposed()) {
            webSocketServerChannel.dispose();
            logger.info("RSocket WebSocket Server shutdown successfully.");
        } else {
            logger.warn("RSocket WebSocket Server was not running, nothing to shutdown.");
        }
        logger.info("RSocket WebSocket Server shutdown completed.");
    }

    public RSocketWebSocketServerConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (event.getApplicationContext() == this.applicationContext) {
            SocketAcceptor socketAcceptor = applicationContext.getBean("webSocketSocketAcceptor", SocketAcceptor.class);
            startWebSocketServer(socketAcceptor);
        } else {
            logger.warn("Application context mismatch, RSocket WebSocket Server will not start.");
        }
    }

}