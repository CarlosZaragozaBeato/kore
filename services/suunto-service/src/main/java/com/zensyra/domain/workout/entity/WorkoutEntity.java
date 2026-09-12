package com.zensyra.domain.workout.entity;

import com.zensyra.domain.workout.model.FitnessMetrics;
import com.zensyra.domain.workout.model.Gear;
import com.zensyra.domain.workout.model.IntensityZones;
import com.zensyra.domain.workout.model.Position;
import com.zensyra.domain.workout.model.TrainingLoad;
import com.zensyra.domain.workout.model.Weather;
import com.zensyra.domain.workout.model.WorkoutSummaryMetrics;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Embedded;
import jakarta.persistence.Column;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
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

    @Column(name = "workout_id")
    public Integer workoutId;

    @Column(name = "stop_time")
    public OffsetDateTime stopTime;

    @Column(name = "last_modified")
    public Long lastModified;

    @Column(name = "avg_pace")
    public Double avgPace;

    @Column(name = "avg_speed")
    public Double avgSpeed;

    @Column(name = "max_speed")
    public Double maxSpeed;

    @Column(name = "avg_power")
    public Double avgPower;

    @Column(name = "max_power")
    public Double maxPower;

    @Column(name = "max_altitude")
    public Double maxAltitude;

    @Column(name = "min_altitude")
    public Double minAltitude;

    @Column(name = "recovery_time")
    public Double recoveryTime;

    @Column(name = "cumulative_recovery_time")
    public Double cumulativeRecoveryTime;

    @Column(name = "time_offset_minutes")
    public Integer timeOffsetInMinutes;

    @Column(name = "estimated_floors_climbed")
    public Integer estimatedFloorsClimbed;

    @Column(name = "edited")
    public Boolean edited;

    @Column(name = "manually_added")
    public Boolean manuallyAdded;

    @Column(name = "comment_count")
    public Integer commentCount;

    @Column(name = "picture_count")
    public Integer pictureCount;

    @Column(name = "view_count")
    public Integer viewCount;

    @Column(name = "hr_max")
    public Double hrMax;

    @Column(name = "user_max_heart_rate")
    public Double userMaxHeartRate;

    @Embedded
    public TrainingLoad trainingLoad;

    @Embedded
    public FitnessMetrics fitnessMetrics;

    @Embedded
    public Gear gear;

    @Embedded
    public Weather weather;

    @Embedded
    public WorkoutSummaryMetrics summaryMetrics;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude", column = @Column(name = "start_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "start_longitude"))
    })
    public Position startPosition;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude", column = @Column(name = "stop_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "stop_longitude"))
    })
    public Position stopPosition;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude", column = @Column(name = "center_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "center_longitude"))
    })
    public Position centerPosition;

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