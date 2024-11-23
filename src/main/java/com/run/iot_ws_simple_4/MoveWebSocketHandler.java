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

    private final ObjectMapper objectMapper;  // Injected ObjectMapper for JSON processing
    private final ActionMoveService actionMoveService;  // Service to handle business logic
    private final WebSocketSessionManager sessionManager;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // Add session to manager on connect
        sessionManager.addSession(session);

        // Khi nhận tin nhắn từ client
        Flux<WebSocketMessage> incomingMessages = session
                .receive()
                .map(it -> toActionMoveRequest(it.getPayloadAsText())) // Chuyển JSON payload thành đối tượng
                .doOnNext(actionMoveRequest -> {
                    if (actionMoveRequest != null) {
                        log.info("Received: {}", actionMoveRequest);
                    } else {
                        log.warn("Received non-JSON message, skipping processing");
                    }
                })
                .flatMap(actionMoveRequest -> {
                    if (actionMoveRequest != null) {
                        // Serialize the ActionMoveRequest to JSON before broadcasting
                        String messageJson = toJson(actionMoveRequest);
                        broadcastMessageToAllSessionsOther(session, messageJson);
                        return actionMoveService.saveActionMove(actionMoveRequest)
                                .map(response -> session.textMessage(toJson(response)));
                    } else {
                        return Mono.empty();
                    }
                });

        // Process incoming messages and ensure the session is removed when done
        return session.send(incomingMessages)
                .doFinally(signalType -> sessionManager.removeSession(session)); // Remove session when disconnected
    }

    private void broadcastMessageToAllSessions(String messageJson) {
        sessionManager.getSessions().forEach(session -> {
            if (session.isOpen()) {
                session.send(Mono.just(session.textMessage(messageJson)))
                        .subscribe(
                                null,
                                error -> log.error("Error sending message to session {}: {}", session.getId(), error)
                        );
            }
        });
    }

    private void broadcastMessageToAllSessionsOther(WebSocketSession currentSession, String messageJson) {
        String command = handleMessage(messageJson);
        sessionManager.getSessions().forEach(session -> {
            if (!session.getId().equals(currentSession.getId()) && session.isOpen()) { // Exclude the current session
                session.send(Mono.just(session.textMessage(command)))
                        .subscribe(
                                null,
                                error -> log.error("Error sending message to session {}: {}", session.getId(), error)
                        );
            }
        });
    }

    private String handleMessage(String messageJson) {
        try {
            // Convert JSON to ActionMoveRequest
            ActionMoveRequest actionMoveRequest = toActionMoveRequest(messageJson);
            String actionMoveNameStr = actionMoveRequest.getActionMoveName();
            Long speed = actionMoveRequest.getSpeed();
            ActionMove.ActionMoveName actionMoveName = ActionMove.ActionMoveName.valueOf(actionMoveNameStr.toUpperCase());
            String command = String.format("{\"action_move_name\":\"%s\",\"speed\":%d}", actionMoveName.getEspEndpoint(), speed);
            return command;
        }
        catch (Exception e) {
            log.error("Lỗi xử lí tin nhắn "+messageJson+" thành lệnh " +e);
            return messageJson;
        }
    }

    // Converts JSON string to ActionMoveRequest
    private ActionMoveRequest toActionMoveRequest(String json) {
        try {
            return objectMapper.readValue(json, ActionMoveRequest.class);
        } catch (Exception e) {
            log.error("Error converting JSON to ActionMoveRequest: {}", json, e);
            throw new RuntimeException("Invalid JSON: " + json, e);
        }
    }

    // Converts ActionMoveResponse to JSON string
    private String toJson(ActionMoveResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("Error converting ActionMoveResponse to JSON", e);
            throw new RuntimeException(e);
        }
    }
    // Converts ActionMoveRequest to JSON string
    private String toJson(ActionMoveRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("Error converting ActionMoveRequest to JSON", e);
            throw new RuntimeException(e);
        }
    }
}
