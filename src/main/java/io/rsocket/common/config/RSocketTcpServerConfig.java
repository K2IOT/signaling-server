package io.rsocket.common.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;

import io.rsocket.SocketAcceptor;
import io.rsocket.common.config.condition.RSocketTcpServerCondition;
import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import jakarta.annotation.PreDestroy;
import reactor.netty.tcp.TcpServer;

@Conditional({ RSocketTcpServerCondition.class })
@Configuration
public class RSocketTcpServerConfig {

    private static final Logger logger = LoggerFactory.getLogger(RSocketTcpServerConfig.class);

    @Value("${rsocket.tcp.host:localhost}")
    private String host;

    @Value("${rsocket.tcp.port:7000}")
    private int port;

    @Value("${rsocket.tcp.max-frame-size:16777215}")
    private int maxFrameSize;

    @Value("${rsocket.tcp.setup-timeout:30000}") // 30 seconds
    private long setupTimeout;

    private CloseableChannel tcpServerChannel;

    @Bean
    public RSocketStrategies tcpRSocketStrategies() {
        return RSocketStrategies.builder()
                .encoder(new Jackson2JsonEncoder())
                .decoder(new Jackson2JsonDecoder())
                .build();
    }

    @Bean
    public SocketAcceptor tcpSocketAcceptor(RSocketMessageHandler tcpRSocketMessageHandler) {
        return tcpRSocketMessageHandler.responder();
    }

    @Bean
    public RSocketMessageHandler tcpRSocketMessageHandler(RSocketStrategies tcpRSocketStrategies) {
        RSocketMessageHandler messageHandler = new RSocketMessageHandler();
        messageHandler.setRSocketStrategies(tcpRSocketStrategies);
        return messageHandler;
    }

    @Bean
    public CloseableChannel tcpServerChannel(SocketAcceptor tcpSocketAcceptor) {
        logger.info("Starting RSocket TCP server on {}:{}", host, port);
        TcpServer tcpServer = TcpServer.create()
                .host(host)
                .port(port)
                .doOnConnection(connection -> {
                    logger.info("TCP Client connected: {}", connection.channel().remoteAddress());
                })
                .doOnChannelInit((observer, channel, remoteAddress) -> {
                    logger.debug("TCP Channel initialized for: {}", remoteAddress);
                });
        this.tcpServerChannel = RSocketServer.create(tcpSocketAcceptor)
                .payloadDecoder(PayloadDecoder.ZERO_COPY)
                .maxInboundPayloadSize(maxFrameSize)
                .bind(TcpServerTransport.create(tcpServer))
                .doOnSuccess(closeableChannel -> {
                    logger.info("RSocket TCP Server started successfully on {}:{}", host, port);
                })
                .doOnError(throwable -> {
                    logger.error("Failed to start RSocket TCP Server", throwable);
                })
                .block(Duration.ofMillis(setupTimeout));
        return this.tcpServerChannel;
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down RSocket TCP servers...");
        if (tcpServerChannel != null && !tcpServerChannel.isDisposed()) {
            tcpServerChannel.dispose();
            logger.info("TCP Server shut down");
        } else {
            logger.warn("TCP Server was not running, nothing to shut down.");
        }
        logger.info("RSocket TCP Server shutdown completed.");
    }

}
