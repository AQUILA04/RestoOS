package com.resto.core.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * STOMP over SockJS at {@code /ws}.
 *
 * <p>Redis backplane is preferred for multi-instance deployments
 * ({@code restoos.redis.broker-enabled=true}). Until then we use
 * {@code enableSimpleBroker} so local / single-node golden path works.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtDecoder jwtDecoder;

    @Value("${restoos.redis.broker-enabled:false}")
    private boolean redisBrokerEnabled;

    public WebSocketConfig(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Prefer Redis STOMP relay in production multi-instance setups.
        // When restoos.redis.broker-enabled=true, wire a StompBrokerRelay to Redis/Rabbit.
        // For now enableSimpleBroker keeps single-node and test profiles working without Redis.
        if (redisBrokerEnabled) {
            // Placeholder for future: config.enableStompBrokerRelay("/topic").setRelayHost(...)
            config.enableSimpleBroker("/topic");
        } else {
            config.enableSimpleBroker("/topic");
        }
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    return message;
                }
                if (StompCommand.CONNECT.equals(accessor.getCommand())
                        || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        throw new IllegalArgumentException("WebSocket requires Authorization: Bearer <jwt>");
                    }
                    String token = authHeader.substring(7);
                    Jwt jwt = jwtDecoder.decode(token);
                    List<SimpleGrantedAuthority> authorities = extractRoles(jwt).stream()
                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                            .collect(Collectors.toList());
                    JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities);
                    accessor.setUser(authentication);

                    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                        String destination = accessor.getDestination();
                        String storeIdClaim = jwt.getClaimAsString("store_id");
                        if (destination != null && destination.contains("/store/") && storeIdClaim != null) {
                            // OWNER/ADMIN may omit store_id; store-scoped roles must match topic store
                            if (!destination.contains("/store/" + storeIdClaim + "/")
                                    && !hasOrgRole(jwt)) {
                                throw new IllegalArgumentException("SUBSCRIBE denied: store membership mismatch");
                            }
                        }
                    }
                }
                return message;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        Object roles = jwt.getClaim("roles");
        if (roles instanceof List<?> list) {
            return list.stream().map(Object::toString).collect(Collectors.toList());
        }
        if (roles instanceof String s) {
            return List.of(s);
        }
        return List.of();
    }

    private boolean hasOrgRole(Jwt jwt) {
        List<String> roles = extractRoles(jwt);
        return roles.contains("OWNER") || roles.contains("ADMIN");
    }
}
