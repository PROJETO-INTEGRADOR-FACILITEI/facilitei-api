package psg.facilitei.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.config.ChannelRegistration;
import psg.facilitei.Security.ChatAuthorizationInterceptor;
import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final ChatAuthorizationInterceptor authorizationInterceptor;
    private final String allowedOrigins;

    public WebSocketConfig(ChatAuthorizationInterceptor authorizationInterceptor,
                           @Value("${myapp.cors.allowed-origins:http://localhost:5173,http://localhost:8080}")
                           String allowedOrigins) {
        this.authorizationInterceptor = authorizationInterceptor;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topics");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Configuração para WebSocket Nativo (ws://)
        // Permite conexão direta do React sem fallback de SockJS
        registry.addEndpoint("/buildrun-livechat-websocket")
                .setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim).filter(value -> !value.isBlank()).toArray(String[]::new));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authorizationInterceptor);
    }
}
