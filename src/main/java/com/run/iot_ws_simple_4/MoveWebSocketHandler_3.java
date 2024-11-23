package com.run.iot_ws_simple_4;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
@Slf4j
public class MoveWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ActionMoveService actionMoveService;
    private final WebSocketSessionManager sessionManager; // Inject session manager

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // Add session to manager on connect
        sessionManager.addSession(session);

        Flux<WebSocketMessage> stringFlux = session
                .receive()
                .map(it -> toActionMoveRequest(it.getPayloadAsText()))
                .doOnNext(actionMoveRequest -> {
                    if (actionMoveRequest != null) {
                        log.info("Received: {}", actionMoveRequest);
                    } else {
                        log.warn("Received non-JSON message, skipping processing");
                    }
                })
                .flatMap(actionMoveRequest -> {
                    if (actionMoveRequest != null) {
                        return actionMoveService.saveActionMove(actionMoveRequest)
                                .map(response -> session.textMessage(toJson(response)));
                    } else {
                        return Mono.empty();
                    }
                });

        return session.send(stringFlux)
                .doFinally(signalType -> sessionManager.removeSession(session)); // Remove session on disconnect
    }

    // Convert JSON to ActionMoveRequest
    private ActionMoveRequest toActionMoveRequest(String json) {
        try {
            return objectMapper.readValue(json, ActionMoveRequest.class);
        } catch (JsonProcessingException e) {
            log.warn("Invalid JSON: {}", json, e);
            return null;
        }
    }

    // Convert ActionMoveResponse to JSON
    private String toJson(ActionMoveResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("Error converting ActionMoveResponse to JSON", e);
            throw new RuntimeException(e);
        }
    }
}

