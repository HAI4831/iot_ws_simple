package com.run.iot_ws_simple_4.repositories;

import com.run.iot_ws_simple_4.entities.SensorData;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface SensorDataRepository extends ReactiveMongoRepository<SensorData, String> {
}
