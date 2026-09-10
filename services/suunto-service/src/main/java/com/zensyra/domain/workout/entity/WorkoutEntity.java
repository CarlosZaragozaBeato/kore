package com.zensyra.domain.workout.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.zensyra.suunto.dto.SuuntoWorkoutDto;

import java.time.OffsetDateTime;

@Entity
@Table(name = "workout")
public class WorkoutEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    public String id;

    @Column(name = "activity_id")
    public Integer activityId;

    @Column(name = "ascent_meters")
    public Double ascentMeters;

    @Column(name = "avg_heart_rate")
    public Double avgHeartRate;

    @Column(name = "descent_meters")
    public Double descentMeters;

    @Column(name = "distance_meters")
    public Double distanceMeters;

    @Column(name = "duration_seconds")
    public Double durationSeconds;

    @Column(name = "energy_consumption")
    public Double energyConsumption;

    @Column(name = "max_heart_rate")
    public Double maxHeartRate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    public String rawPayload;

    @Column(name = "start_time")
    public OffsetDateTime startTime;

    @Column(name = "step_count")
    public Integer stepCount;

    public static void updateOrInsert(SuuntoWorkoutDto dto, String rawPayload) {
        WorkoutEntity entity = findById(dto.workoutKey());

        if (entity == null) {
            entity = new WorkoutEntity();
            entity.id = dto.workoutKey();

            entity.activityId = dto.activityId();
            entity.ascentMeters = dto.ascentMeters();
            entity.avgHeartRate = dto.avgHeartRate();
            entity.descentMeters = dto.descentMeters();
            entity.distanceMeters = dto.distanceMeters();
            entity.durationSeconds = dto.durationSeconds();
            entity.energyConsumption = dto.energyConsumption();
            entity.maxHeartRate = dto.maxHeartRate();
            entity.rawPayload = rawPayload;
            entity.startTime = dto.startTimeAsOffsetDateTime();
            entity.stepCount = dto.stepCount();

            entity.persist();
            return;
        }

        entity.activityId = dto.activityId();
        entity.ascentMeters = dto.ascentMeters();
        entity.avgHeartRate = dto.avgHeartRate();
        entity.descentMeters = dto.descentMeters();
        entity.distanceMeters = dto.distanceMeters();
        entity.durationSeconds = dto.durationSeconds();
        entity.energyConsumption = dto.energyConsumption();
        entity.maxHeartRate = dto.maxHeartRate();
        entity.rawPayload = rawPayload;
        entity.startTime = dto.startTimeAsOffsetDateTime();
        entity.stepCount = dto.stepCount();
    }
}