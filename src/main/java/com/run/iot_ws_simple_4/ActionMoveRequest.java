// MoveRequest.java
package com.run.iot_websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ActionMoveRequest {
    @JsonProperty("action_move_name")
    private String actionMoveName;
    @JsonProperty("speed")
    private Long speed;
}
