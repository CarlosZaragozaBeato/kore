package com.zensyra.domain.workout.model;

import com.zensyra.domain.training_load.model.TrainingLoad;
import com.zensyra.suunto.dto.SuuntoWorkoutDto;
import java.time.Instant;

public record Workout(
        String id,
        Integer activityId,
        Instant startTime,
        Double durationSeconds,
        Double distanceMeters,
        Double ascentMeters,
        Double descentMeters,
        TrainingLoad trainingLoad
) {
    public static Workout fromSuuntoDto(SuuntoWorkoutDto dto) {
        TrainingLoad load = new TrainingLoad(
                dto.energyConsumption(),
                dto.avgHeartRate(),
                dto.maxHeartRate(),
                dto.stepCount()
        );

        return new Workout(
                dto.workoutKey(),
                dto.activityId(),
                dto.startTime() != null ? Instant.ofEpochMilli(dto.startTime()) : null,
                dto.totalTime(),
                dto.totalDistance(),
                dto.totalAscent(),
                dto.totalDescent(),
                load
        );
    }
}