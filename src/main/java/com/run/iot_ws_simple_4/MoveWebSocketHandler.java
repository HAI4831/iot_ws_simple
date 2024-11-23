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
                        // Lưu dữ liệu và phản hồi dạng JSON
                        return actionMoveService.saveActionMove(actionMoveRequest)
                                .flatMap(response -> {
                                    // Chuyển thành JSON message
                                    String messageJson = toJson(response);
                                    // Gửi broadcast tới tất cả session
                                    broadcastMessageToAllSessionsOther(session, messageJson);
//                                    broadcastMessageToAllSessions(messageJson);
                                    return Mono.empty(); // Không gửi lại cho chính client đã gửi
                                });
                    } else {
                        return Mono.empty();
                    }
                });

        return session.send(incomingMessages)
                .doFinally(signalType -> sessionManager.removeSession(session)); // Remove session khi ngắt kết nối
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
        sessionManager.getSessions().forEach(session -> {
            if (!session.getId().equals(currentSession.getId()) && session.isOpen()) { // Loại bỏ session hiện tại
                session.send(Mono.just(session.textMessage(messageJson)))
                        .subscribe(
                                null,
                                error -> log.error("Error sending message to session {}: {}", session.getId(), error)
                        );
            }
        });
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
}
