package com.run.iot_websocket;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionMoveRepository extends ReactiveMongoRepository<ActionMove, String> {
}
