package com.run.iot_ws_simple_4.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.run.iot_ws_simple_4.dto.request.ActionMoveRequest;
import com.run.iot_ws_simple_4.dto.request.SensorDataRequest;
import com.run.iot_ws_simple_4.entities.ActionMove;
import com.run.iot_ws_simple_4.services.SensorDataService;
import com.run.iot_ws_simple_4.utils.WebSocketSessionManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
@Slf4j
public class MoveWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    private final SensorDataService sensorDataService;
    private final WebSocketSessionManager sessionManager;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        sessionManager.addSession(session);

        return session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(message -> log.info("Received: {}", message))
                .flatMap(message -> handleMessage(session, message))
                .doFinally(signalType -> sessionManager.removeSession(session))
                .then();
    }

    private Mono<Void> handleMessage(WebSocketSession session, String message) {
        try {
            String key = null;
            String value = null;

            // Check for the presence of different types of sensor data
            if (message.contains("gas_data")) {
                key = "gas_data";
                value = extractValueFromJson(message, "gas_data");
            } else if (message.contains("temperature_data") && message.contains("humidity_data")) { // Fixed this condition
                String key1 = "temperature_data"; // Declare key1
                String value1 = extractValueFromJson(message, "temperature_data"); // Separate value for temperature
                String key2 = "humidity_data"; // Declare key2
                String value2 = extractValueFromJson(message, "humidity_data"); // Separate value for humidity

                // You can now handle both keys/values here (e.g., save or send both data)
                SensorDataRequest sensorDataRequest1 = SensorDataRequest.builder()
                        .dataType(key1)
                        .value(value1)
                        .build();
                sensorDataService.saveSensorData(sensorDataRequest1).subscribe();

                SensorDataRequest sensorDataRequest2 = SensorDataRequest.builder()
                        .dataType(key2)
                        .value(value2)
                        .build();
                sensorDataService.saveSensorData(sensorDataRequest2).subscribe();

                String jsonMessage1 = objectMapper.writeValueAsString(sensorDataRequest1);
                String jsonMessage2 = objectMapper.writeValueAsString(sensorDataRequest2);
                broadcastMessageToAllSessionsOther(session, jsonMessage1);
                broadcastMessageToAllSessionsOther(session, jsonMessage2);
                return Mono.empty();
            }

            if (key != null && value != null) {
                SensorDataRequest sensorDataRequest = SensorDataRequest.builder()
                        .dataType(key)
                        .value(value)
                        .build();
                sensorDataService.saveSensorData(sensorDataRequest).subscribe();
                String jsonMessage = objectMapper.writeValueAsString(sensorDataRequest);
                broadcastMessageToAllSessionsOther(session, jsonMessage);
            }

            if (message.contains("action_move_name")) {
                // Convert JSON to ActionMoveRequest
                ActionMoveRequest actionMoveRequest = toActionMoveRequest(message);
                String actionMoveNameStr = actionMoveRequest.getActionMoveName();
                Long speed = actionMoveRequest.getSpeed();
                ActionMove.ActionMoveName actionMoveName = ActionMove.ActionMoveName.valueOf(actionMoveNameStr.toUpperCase());
                String command = String.format("{\"action_move_name\":\"%s\",\"speed\":%d}", actionMoveName.getEspEndpoint(), speed);
                broadcastMessageToAllSessionsOther(session, command);
            }

        } catch (Exception e) {
            log.error("Error processing message: {}", message, e);
        }
        return Mono.empty();
    }

    private ActionMoveRequest toActionMoveRequest(String json) {
        log.warn(" converting toActionMoveRequest: {}", json);
        try {
            return objectMapper.readValue(json, ActionMoveRequest.class);
        } catch (Exception e) {
            log.warn("Error converting JSON to ActionMoveRequest: {}", json, e);
            throw new RuntimeException("Invalid JSON: " + json, e);
        }
    }

    private String extractValueFromJson(String message, String key) {
        try {
            return objectMapper.readTree(message).get(key).asText();
        } catch (JsonProcessingException e) {
            log.error("Error extracting value for key '{}' from message", key, e);
            return null;
        }
    }

    private void sendJsonResponse(WebSocketSession session, String dataType, String value) {
        try {
            // Create a JSON response with the sensor data
            ObjectNode response = objectMapper.createObjectNode();
            response.put(dataType, value);

            String responseMessage = objectMapper.writeValueAsString(response);

            // Send the response message to the WebSocket session
            session.send(Mono.just(session.textMessage(responseMessage)))
                    .subscribe(
                            null,
                            error -> log.error("Error sending message to session {}: {}", session.getId(), error)
                    );
            log.info("[Status Update] Sending status: {}", responseMessage);
        } catch (JsonProcessingException e) {
            log.error("Error serializing JSON for response: {}", e.getMessage());
        }
    }

    private void broadcastMessageToAllSessionsOther(WebSocketSession currentSession, String messageJson) {
        sessionManager.getSessions().forEach(session -> {
            if (!session.getId().equals(currentSession.getId()) && session.isOpen()) { // Exclude the current session
                session.send(Mono.just(session.textMessage(messageJson)))
                        .subscribe(
                                null,
                                error -> log.error("Error sending message to session {}: {}", session.getId(), error)
                        );
            }
        });
    }
}
