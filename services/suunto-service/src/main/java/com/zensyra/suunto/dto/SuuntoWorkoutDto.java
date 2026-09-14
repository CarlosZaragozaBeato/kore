package com.zensyra.suunto.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SuuntoWorkoutDto(

        @JsonProperty("workoutKey") String workoutKey,

        @JsonProperty("workoutId") Integer workoutId,

        @JsonProperty("activityId") Integer activityId,

        @JsonProperty("startTime") Long startTime,

        @JsonProperty("stopTime") Long stopTime,

        @JsonProperty("lastModified") Long lastModified,

        @JsonProperty("totalDistance") Double totalDistance,

        @JsonProperty("totalTime") Double totalTime,

        @JsonProperty("totalAscent") Double totalAscent,

        @JsonProperty("totalDescent") Double totalDescent,

        @JsonProperty("avgPace") Double avgPace,

        @JsonProperty("avgSpeed") Double avgSpeed,

        @JsonProperty("maxSpeed") Double maxSpeed,

        @JsonProperty("avgPower") Double avgPower,

        @JsonProperty("energyConsumption") Double energyConsumption,

        @JsonProperty("maxAltitude") Double maxAltitude,

        @JsonProperty("minAltitude") Double minAltitude,

        @JsonProperty("recoveryTime") Double recoveryTime,

        @JsonProperty("cumulativeRecoveryTime") Double cumulativeRecoveryTime,

        @JsonProperty("timeOffsetInMinutes") Integer timeOffsetInMinutes,

        @JsonProperty("estimatedFloorsClimbed") Integer estimatedFloorsClimbed,

        @JsonProperty("isEdited") Boolean isEdited,

        @JsonProperty("isManuallyAdded") Boolean isManuallyAdded,

        @JsonProperty("commentCount") Integer commentCount,

        @JsonProperty("pictureCount") Integer pictureCount,

        @JsonProperty("viewCount") Integer viewCount,

        @JsonProperty("hrdata") HrData hrData,

        @JsonProperty("cadence") CadenceData cadence,

        @JsonProperty("startPosition") PositionData startPosition,

        @JsonProperty("stopPosition") PositionData stopPosition,

        @JsonProperty("centerPosition") PositionData centerPosition,

        @JsonProperty("tssList") List<TssData> tssList,

        @JsonProperty("stepCount") Integer stepCount,

        @JsonProperty("extensions") List<JsonNode> extensions

) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HrData(

            @JsonProperty("avg") Double avg,

            @JsonProperty("max") Double max,

            @JsonProperty("hrmax") Double hrMax,

            @JsonProperty("userMaxHR") Double userMaxHR,

            @JsonProperty("workoutAvgHR") Double workoutAvgHR,

            @JsonProperty("workoutMaxHR") Double workoutMaxHR) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CadenceData(

            @JsonProperty("avg") Double avg,

            @JsonProperty("max") Double max) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PositionData(

            @JsonProperty("x") Double x,

            @JsonProperty("y") Double y) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TssData(

            @JsonProperty("intensityFactor") Double intensityFactor,

            @JsonProperty("normalizedPower") Double normalizedPower,

            @JsonProperty("calculationMethod") String calculationMethod,

            @JsonProperty("trainingStressScore") Double trainingStressScore,

            @JsonProperty("averageGradeAdjustedPace") Double averageGradeAdjustedPace) {
    }

    public Double avgHeartRate() {
        return hrData == null ? null : hrData.workoutAvgHR();
    }

    public Double maxHeartRate() {
        return hrData == null ? null : hrData.workoutMaxHR();
    }

    public Double hrMax() {
        return hrData == null ? null : hrData.hrMax();
    }

    public Double userMaxHeartRate() {
        return hrData == null ? null : hrData.userMaxHR();
    }

    public Double distanceMeters() {
        return totalDistance;
    }

    public Double durationSeconds() {
        return totalTime;
    }

    public Double ascentMeters() {
        return totalAscent;
    }

    public Double descentMeters() {
        return totalDescent;
    }

    public OffsetDateTime startTimeAsOffsetDateTime() {
        return startTime == null
                ? null
                : Instant.ofEpochMilli(startTime).atOffset(ZoneOffset.UTC);
    }

    public OffsetDateTime stopTimeAsOffsetDateTime() {
        return stopTime == null
                ? null
                : Instant.ofEpochMilli(stopTime).atOffset(ZoneOffset.UTC);
    }
}