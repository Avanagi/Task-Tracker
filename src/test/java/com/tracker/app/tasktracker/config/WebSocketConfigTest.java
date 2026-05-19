package com.tracker.app.tasktracker.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @InjectMocks
    private WebSocketConfig webSocketConfig;

    @Mock
    private MessageBrokerRegistry messageBrokerRegistry;

    @Mock
    private StompEndpointRegistry stompEndpointRegistry;

    @Mock
    private StompWebSocketEndpointRegistration endpointRegistration;


    @Test
    void configureMessageBroker_Positive_SuccessConfig() {
        when(messageBrokerRegistry.enableSimpleBroker("/topic", "/queue")).thenReturn(null);
        when(messageBrokerRegistry.setApplicationDestinationPrefixes("/app")).thenReturn(messageBrokerRegistry);
        when(messageBrokerRegistry.setUserDestinationPrefix("/user")).thenReturn(messageBrokerRegistry);

        webSocketConfig.configureMessageBroker(messageBrokerRegistry);

        verify(messageBrokerRegistry, times(1)).enableSimpleBroker("/topic", "/queue");
        verify(messageBrokerRegistry, times(1)).setApplicationDestinationPrefixes("/app");
        verify(messageBrokerRegistry, times(1)).setUserDestinationPrefix("/user");
    }

    @Test
    void configureMessageBroker_Negative_NullRegistry() {
        assertThrows(NullPointerException.class, () -> webSocketConfig.configureMessageBroker(null));
    }

    @Test
    void configureMessageBroker_Boundary_RegistryThrowsException() {
        when(messageBrokerRegistry.enableSimpleBroker(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Invalid prefixes"));

        assertThrows(IllegalArgumentException.class, () -> webSocketConfig.configureMessageBroker(messageBrokerRegistry));
    }

    @Test
    void configureMessageBroker_Boundary_SecurityException() {
        when(messageBrokerRegistry.setApplicationDestinationPrefixes(anyString()))
                .thenThrow(new SecurityException("Access denied to config"));

        assertThrows(SecurityException.class, () -> webSocketConfig.configureMessageBroker(messageBrokerRegistry));
    }


    @Test
    void registerStompEndpoints_Positive_SuccessConfig() {
        when(stompEndpointRegistry.addEndpoint("/ws")).thenReturn(endpointRegistration);
        when(endpointRegistration.setAllowedOrigins("http://localhost:3000", "http://localhost:5173")).thenReturn(endpointRegistration);

        webSocketConfig.registerStompEndpoints(stompEndpointRegistry);

        verify(stompEndpointRegistry, times(1)).addEndpoint("/ws");
        verify(endpointRegistration, times(1)).setAllowedOrigins("http://localhost:3000", "http://localhost:5173");
        verify(endpointRegistration, times(1)).withSockJS();
    }

    @Test
    void registerStompEndpoints_Negative_NullRegistry() {
        assertThrows(NullPointerException.class, () -> webSocketConfig.registerStompEndpoints(null));
    }

    @Test
    void registerStompEndpoints_Boundary_EndpointAddFails() {
        when(stompEndpointRegistry.addEndpoint("/ws")).thenThrow(new IllegalStateException("Path already mapped"));

        assertThrows(IllegalStateException.class, () -> webSocketConfig.registerStompEndpoints(stompEndpointRegistry));

        verify(endpointRegistration, never()).setAllowedOrigins(any());
        verify(endpointRegistration, never()).withSockJS();
    }

    @Test
    void registerStompEndpoints_Boundary_CorsConfigFails() {
        when(stompEndpointRegistry.addEndpoint("/ws")).thenReturn(endpointRegistration);
        when(endpointRegistration.setAllowedOrigins(any(), any()))
                .thenThrow(new IllegalArgumentException("Invalid origin format"));

        assertThrows(IllegalArgumentException.class, () -> webSocketConfig.registerStompEndpoints(stompEndpointRegistry));

        verify(endpointRegistration, never()).withSockJS();
    }
}