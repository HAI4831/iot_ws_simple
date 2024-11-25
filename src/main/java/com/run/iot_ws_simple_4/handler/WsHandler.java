//package com.run.iot_ws_simple_4.handler;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.run.iot_ws_simple_4.dto.request.SensorDataRequest;
//import com.run.iot_ws_simple_4.entities.SensorData;
//import com.run.iot_ws_simple_4.services.SensorDataService;
//import com.run.iot_ws_simple_4.utils.WebSocketSessionManager;
//import lombok.AllArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import org.springframework.web.reactive.socket.WebSocketHandler;
//import org.springframework.web.reactive.socket.WebSocketMessage;
//import org.springframework.web.reactive.socket.WebSocketSession;
//import reactor.core.publisher.Mono;
//
//@Component
//@AllArgsConstructor
//@Slf4j
//public class WsHandler implements WebSocketHandler {
//
//    private final ObjectMapper objectMapper;
//    private final SensorDataService sensorDataService;
//    private final WebSocketSessionManager sessionManager;
//
//    @Override
//    public Mono<Void> handle(WebSocketSession session) {
//        sessionManager.addSession(session);
//
//        return session.receive()
//                .map(WebSocketMessage::getPayloadAsText)
//                .doOnNext(message -> {
//                    log.info("Received: {}", message);
//                })
//                .flatMap(message -> handleMessage(session, message))  // Call the handleMessage function here
//                .doFinally(signalType -> sessionManager.removeSession(session))
//                .then();
//    }
//
//    private Mono<Void> handleMessage(WebSocketSession session, String message) {
//        try {
//            if (message.contains("temperature_data") || message.contains("humidity_data")) {
//                // Xử lý dữ liệu cảm biến DHT11: nhiệt độ và độ ẩm
//                String temperatureData = extractValueFromJson(message, "temperature_data");
//                String humidityData = extractValueFromJson(message, "humidity_data");
//
//                // Tạo đối tượng SensorData từ dữ liệu cảm biến
//                SensorDataRequest temperatureSensorData = SensorDataRequest.builder()
//                        .dataType("temperature_data")
//                        .value(temperatureData) // Dữ liệu nhiệt độ
//                        .build();
//
//                SensorDataRequest humiditySensorData = SensorDataRequest.builder()
//                        .dataType("humidity_data")
//                        .value(humidityData) // Dữ liệu độ ẩm
//                        .build();
//
//                // Lưu dữ liệu cảm biến vào cơ sở dữ liệu
//                return Mono.zip(
//                        sensorDataService.saveSensorData(temperatureSensorData),
//                        sensorDataService.saveSensorData(humiditySensorData)
//                ).flatMap(result -> {
//                    // Tạo phản hồi dưới dạng JSON và gửi lại cho WebSocket
//                    String responseMessage = toJson(result.getT1());  // Chọn dữ liệu nhiệt độ hoặc độ ẩm làm phản hồi
//                    broadcastMessageToAllSessionsOther(session, responseMessage);  // Gửi phản hồi cho các session khác
//                    return session.send(Mono.just(session.textMessage(responseMessage)));
//                });
//            }
//        } catch (Exception e) {
//            log.error("Error processing message: {}", message, e);
//        }
//        return Mono.empty();
//    }
//
//    private String extractValueFromJson(String message, String key) {
//        try {
//            // Tạo một đối tượng JSON từ chuỗi message để lấy giá trị tương ứng với key
//            return objectMapper.readTree(message).get(key).asText();
//        } catch (JsonProcessingException e) {
//            log.error("Error extracting value for key '{}' from message", key, e);
//            return null;
//        }
//    }
//
//    private void broadcastMessageToAllSessionsOther(WebSocketSession currentSession, String messageJson) {
//        sessionManager.getSessions().forEach(session -> {
//            if (!session.getId().equals(currentSession.getId()) && session.isOpen()) { // Exclude the current session
//                session.send(Mono.just(session.textMessage(messageJson)))
//                        .subscribe(
//                                null,
//                                error -> log.error("Error sending message to session {}: {}", session.getId(), error)
//                        );
//            }
//        });
//    }
//
//    private String toJson(Object object) {
//        try {
//            return objectMapper.writeValueAsString(object);
//        } catch (JsonProcessingException e) {
//            log.error("Error converting object to JSON", e);
//            throw new RuntimeException(e);
//        }
//    }
//}
