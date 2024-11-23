package com.run.iot_ws_simple_4;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ActionMoveRepository repository;

    public DataInitializer(ActionMoveRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) throws Exception {
        repository.deleteAll()
                .thenMany(Flux.just(
                        ActionMove.builder().actionMoveName(ActionMove.ActionMoveName.FORWARD).speed(10L).build(),
                        ActionMove.builder().actionMoveName(ActionMove.ActionMoveName.BACKWARD).speed(5L).build()
                ))
                .flatMap(repository::save)
                .subscribe(System.out::println);
    }
}

