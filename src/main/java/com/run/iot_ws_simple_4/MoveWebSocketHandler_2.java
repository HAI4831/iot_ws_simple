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
//                .doOnNext(actionMoveRequest -> {
//                    // Log valid requests
//                    if (actionMoveRequest != null) {
//                        log.info("Received: {}", actionMoveRequest);
//                    } else {
//                        log.warn("Received non-JSON message, skipping processing");
//                    }
//                })
//                .flatMap(actionMoveRequest -> {
//                    // Proceed only if the request is valid
//                    if (actionMoveRequest != null) {
//                        return actionMoveService.saveActionMove(actionMoveRequest)
//                                .map(response -> session.textMessage(toJson(response)));  // Map ActionMoveResponse to WebSocket response
//                    } else {
//                        // If not a valid request, return empty Mono to skip further processing
//                        return Mono.empty();
//                    }
//                });
//
//        // Send the response back to the client
//        return session.send(stringFlux);
//    }
//
//    // Converts JSON string to ActionMoveRequest
//    private ActionMoveRequest toActionMoveRequest(String json) {
//        try {
//            // Attempt to parse the JSON into ActionMoveRequest
//            return objectMapper.readValue(json, ActionMoveRequest.class);
//        } catch (JsonProcessingException e) {
//            // Log the error but return null (skip processing if JSON is invalid)
//            log.warn("Received invalid JSON, could not parse: {}", json, e);
//            return null;  // Return null instead of throwing exception
//        } catch (Exception e) {
//            // Catch any other exceptions and log them
//            log.error("Unexpected error while parsing JSON: {}", json, e);
//            return null;
//        }
//    }
//
//    // Converts ActionMoveResponse to JSON string
//    private String toJson(ActionMoveResponse response) {
//        try {
//            // Convert ActionMoveResponse to JSON string
//            return objectMapper.writeValueAsString(response);
//        } catch (JsonProcessingException e) {
//            log.error("Error converting ActionMoveResponse to JSON", e);
//            throw new RuntimeException(e);
//        }
//    }
//}
