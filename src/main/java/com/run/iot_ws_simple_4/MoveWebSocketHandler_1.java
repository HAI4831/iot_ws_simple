//package com.run.iot_ws_simple_4;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.AllArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import org.springframework.web.reactive.socket.WebSocketHandler;
//import org.springframework.web.reactive.socket.WebSocketMessage;
//import org.springframework.web.reactive.socket.WebSocketSession;
//import reactor.core.publisher.Mono;
//import reactor.core.publisher.Flux;
//
//@Component
//@AllArgsConstructor
//@Slf4j
//public class MoveWebSocketHandler implements WebSocketHandler {
//
//    private final ObjectMapper objectMapper;  // Injected ObjectMapper for JSON processing
//    private final ActionMoveService actionMoveService;  // Service to handle business logic
//
//    @Override
//    public Mono<Void> handle(WebSocketSession session) {
//        Flux<WebSocketMessage> stringFlux = session
//                .receive()  // Receive WebSocket messages from the client
//                .map(it -> toActionMoveRequest(it.getPayloadAsText()))  // Map payload to ActionMoveRequest
//                .doOnNext(actionMoveRequest -> log.info("Received: {}", actionMoveRequest))
//                .flatMap(actionMoveRequest -> {
//                    // Không dùng .block() nữa, thay vào đó trả về Mono
//                    return actionMoveService.saveActionMove(actionMoveRequest)
//                            .map(response -> session.textMessage(toJson(response)));  // Map ActionMoveResponse to WebSocket response
//                });
////                .map(actionMoveRequest -> {
////                    // Create a response based on ActionMoveRequest
////                    ActionMoveResponse response = actionMoveService.saveActionMove(actionMoveRequest).block();
//////                    ActionMoveResponse response = new ActionMoveResponse().toBuilder().status("response").message("message response").build();
////                    return session.textMessage(toJson(response));  // Convert the response to JSON
////                });
//
//        // Send the response back to the client
//        return session.send(stringFlux);
//    }
//
//    // Converts JSON string to ActionMoveRequest
//    private ActionMoveRequest toActionMoveRequest(String json) {
//        try {
//            return objectMapper.readValue(json, ActionMoveRequest.class);
//        } catch (Exception e) {
//            log.error("Error converting JSON to ActionMoveRequest: {}", json, e);
//            throw new RuntimeException("Invalid JSON: " + json, e);
//        }
//    }
//
//    // Converts ActionMoveResponse to JSON string
//    private String toJson(ActionMoveResponse response) {
//        try {
//            return objectMapper.writeValueAsString(response);
//        } catch (JsonProcessingException e) {
//            log.error("Error converting ActionMoveResponse to JSON", e);
//            throw new RuntimeException(e);
//        }
//    }
//}
