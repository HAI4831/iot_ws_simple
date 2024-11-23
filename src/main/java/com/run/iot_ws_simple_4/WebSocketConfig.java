package com.run.iot_ws_simple_4;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import lombok.AllArgsConstructor;
import lombok.Setter;

@Setter
@Configuration
@AllArgsConstructor
public class EventWebSocketConfiguration {

    private final EventSocketHandler eventSocketHandler;

    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        final Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/chat", eventSocketHandler);

        final SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setOrder(1);
        handlerMapping.setUrlMap(map);
        return handlerMapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
