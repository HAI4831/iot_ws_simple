package com.run.iot_ws_simple_4;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
public class WebSocketSessionManager {
    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    public void addSession(WebSocketSession session) {
        sessions.add(session);
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
    }

    public CopyOnWriteArraySet<WebSocketSession> getSessions() {
        return sessions;
    }
    public void broadcastMessage(String message) {
        sessions.forEach(session -> {
            if (session.isOpen()) {
                session.send(Mono.just(session.textMessage(message)))
                        .subscribe(null, error -> log.error("Error sending message to session: {}", session.getId(), error));
            }
        });
    }
    private void broadcastMessageToAllSessions(String messageJson) {
        sessions.forEach(session -> {
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
        sessions.forEach(session -> {
            if (!session.getId().equals(currentSession.getId()) && session.isOpen()) { // Loại bỏ session hiện tại
                session.send(Mono.just(session.textMessage(messageJson)))
                        .subscribe(
                                null,
                                error -> log.error("Error sending message to session {}: {}", session.getId(), error)
                        );
            }
        });
    }

}

