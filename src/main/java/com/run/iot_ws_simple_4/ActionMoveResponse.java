package com.run.iot_websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ActionMoveResponse {
    @JsonProperty("status")
    private String status;
    @JsonProperty("message")
    private String message;
}
