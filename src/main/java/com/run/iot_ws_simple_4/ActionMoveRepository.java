package com.run.iot_ws_simple_4;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionMoveRepository extends ReactiveMongoRepository<ActionMove, String> {
}
