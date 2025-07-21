package io.rsocket.common.config.condition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ResourceCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import lombok.Generated;


public class RSocketWebSocketServerCondition extends ResourceCondition {


    @Generated
    private static final Logger logger = LoggerFactory.getLogger(RSocketWebSocketServerCondition.class);
   
    static final String RSOCKET_WEBSOCKET_SERVER_PROPERTY = "rsocket.websocket.server.enabled";
    
    public RSocketWebSocketServerCondition() {
        super("RSocket-WebSocket-Server-Condition", RSOCKET_WEBSOCKET_SERVER_PROPERTY, new String[0]);
    }

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        if (context.getEnvironment().containsProperty(RSOCKET_WEBSOCKET_SERVER_PROPERTY)) {
            boolean isEnabled = context.getEnvironment().getProperty(RSOCKET_WEBSOCKET_SERVER_PROPERTY, Boolean.class, true);
            if (isEnabled) {
              //  logger.info("RSocket WebSocket Server is enabled");
                return ConditionOutcome.match("RSocket WebSocket Server is enabled");
            } else {
              //  logger.info("RSocket WebSocket Server is disabled");
                return ConditionOutcome.noMatch("RSocket WebSocket Server is disabled");
            }
        }
        logger.warn("RSocket WebSocket Server property '{}' not found, defaulting to disabled", RSOCKET_WEBSOCKET_SERVER_PROPERTY);
        return ConditionOutcome.noMatch("RSocket WebSocket Server property not found, defaulting to disabled");
    }
    
}
