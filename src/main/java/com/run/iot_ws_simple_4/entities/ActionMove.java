package com.run.iot_ws_simple_4.entities;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "action_move")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionMove {
    @Id
    private String id;

    @NotNull(message = "Action name must not be null")
    private ActionMoveName actionMoveName;

    @NotNull(message = "Speed must not be null")
    private Long speed;

    /**
     * Enum representing the possible actions for moving the robot.
     */
    @Getter
    public enum ActionMoveName {
        FORWARD("tien"),
        BACKWARD("lui"),
        LEFT("retrai"),
        RIGHT("rephai"),
        STOP("dung");

        private final String espEndpoint;

        ActionMoveName(String espEndpoint) {
            this.espEndpoint = espEndpoint;
        }
        public String getEspEndpoint() {
            return espEndpoint;
        }
    }
}
