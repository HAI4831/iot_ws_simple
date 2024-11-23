package com.run.iot_ws_simple_4;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ActionMoveService {

    private final ActionMoveRepository repository;

    public ActionMoveService(ActionMoveRepository repository) {
        this.repository = repository;
    }

    // Modify this method to accept ActionMoveRequest directly
    public Mono<ActionMoveResponse> saveActionMove(ActionMoveRequest actionMoveRequest) {
        // Convert ActionMoveRequest to ActionMove (assuming ActionMove entity exists)
        ActionMove actionMove = new ActionMove();
        actionMove.setActionMoveName(ActionMove.ActionMoveName.valueOf(actionMoveRequest.getActionMoveName()));
        actionMove.setSpeed(actionMoveRequest.getSpeed());

        // Save to repository and return ActionMoveResponse
        return repository.save(actionMove)
                .map(savedActionMove -> new ActionMoveResponse("SUCCESS", "Action '" + savedActionMove.getActionMoveName() + "' saved"));
    }

    public Flux<ActionMove> getAllActionMoves() {
        return repository.findAll();
    }

    public Mono<ActionMove> getActionMoveById(String id) {
        return repository.findById(id);
    }

    public Mono<Void> deleteActionMove(String id) {
        return repository.deleteById(id);
    }
}

