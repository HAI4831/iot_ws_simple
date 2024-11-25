package com.run.iot_ws_simple_4.services;

import com.run.iot_ws_simple_4.dto.request.SensorDataRequest;
import com.run.iot_ws_simple_4.entities.SensorData;
import com.run.iot_ws_simple_4.repositories.SensorDataRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class SensorDataService {

    private final SensorDataRepository sensorDataRepository;

    public Mono<SensorData> saveSensorData(SensorDataRequest request) {
        SensorData sensorData = new SensorData(null, request.getDataType(), request.getValue());
        return sensorDataRepository.save(sensorData);
    }
}
