package io.rsocket.common.config.condition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ResourceCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import lombok.Generated;

public class RSocketTcpServerCondition extends ResourceCondition {

    @Generated
    private static final Logger logger = LoggerFactory.getLogger(RSocketTcpServerCondition.class);

    static final String RSOCKET_TCP_SERVER_PROPERTY = "rsocket.tcp.server.enabled";

    protected RSocketTcpServerCondition(String name, String property, String[] resourceLocations) {
        super("RSocket-Tcp-Server-Condition", RSOCKET_TCP_SERVER_PROPERTY, new String[0]);
    }

    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata){
        if (context.getEnvironment().containsProperty(RSOCKET_TCP_SERVER_PROPERTY)) {
            boolean isEnabled = context.getEnvironment().getProperty(RSOCKET_TCP_SERVER_PROPERTY, Boolean.class, true);
            if (isEnabled) {
                logger.info("RSocket TCP Server is enabled");
                return ConditionOutcome.match("RSocket TCP Server is enabled");
            } else {
                logger.info("RSocket TCP Server is disabled");
                return ConditionOutcome.noMatch("RSocket TCP Server is disabled");
            }
        }
        logger.warn("RSocket TCP Server property '{}' not found, defaulting to disabled", RSOCKET_TCP_SERVER_PROPERTY);
        return ConditionOutcome.noMatch("RSocket TCP Server property not found, defaulting to disabled");
    }
    
}
