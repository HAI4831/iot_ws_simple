package com.run.iot_ws_simple_4.configs;

import com.run.iot_ws_simple_4.entities.ActionMove;
import com.run.iot_ws_simple_4.entities.SensorData;
import com.run.iot_ws_simple_4.repositories.ActionMoveRepository;
import com.run.iot_ws_simple_4.repositories.SensorDataRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ActionMoveRepository actionMoveRepository;
    private final SensorDataRepository sensorDataRepository;

    public DataInitializer(ActionMoveRepository actionMoveRepository, SensorDataRepository sensorDataRepository) {
        this.actionMoveRepository = actionMoveRepository;
        this.sensorDataRepository = sensorDataRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Khởi tạo dữ liệu ActionMove
        actionMoveRepository.deleteAll()
                .thenMany(Flux.just(
                        ActionMove.builder().actionMoveName(ActionMove.ActionMoveName.FORWARD).speed(10L).build(),
                        ActionMove.builder().actionMoveName(ActionMove.ActionMoveName.BACKWARD).speed(5L).build()
                ))
                .flatMap(actionMoveRepository::save)
                .subscribe(System.out::println);

        // Khởi tạo dữ liệu SensorData
        sensorDataRepository.deleteAll()
                .thenMany(Flux.just(
                        SensorData.builder()
                                .dataType("gas_data")
                                .value("123")
                                .build(),
                        SensorData.builder()
                                .dataType("temperature_data")
                                .value("25.5")
                                .build(),
                        SensorData.builder()
                                .dataType("humidity_data")
                                .value("49")
                                .build()
                ))
                .flatMap(sensorDataRepository::save)
                .subscribe(System.out::println);
    }
}
