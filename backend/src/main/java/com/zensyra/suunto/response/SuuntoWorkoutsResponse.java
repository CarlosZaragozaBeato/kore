package com.zensyra.suunto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zensyra.suunto.model.SuuntoWorkoutDto;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SuuntoWorkoutsResponse(
    List<SuuntoWorkoutDto> payload
) {
    public List<SuuntoWorkoutDto> workoutsOrEmpty() {
        return payload != null ? payload : List.of();
    } 
}
