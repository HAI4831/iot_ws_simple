package com.run.iot_ws_simple_4.controllers;

import com.run.iot_ws_simple_4.utils.WebSocketSessionManager;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class BroadcastController {
    private final WebSocketSessionManager sessionManager;

    @PostMapping("/broadcast")
    public ResponseEntity<String> broadcastMessage(@RequestBody String message) {
        sessionManager.broadcastMessage(message);
        return ResponseEntity.ok("Message broadcasted successfully");
    }
}

