package com.zensyra.suunto.mapper;

import com.zensyra.domain.workout.entity.WorkoutEntity;
import com.zensyra.domain.workout.model.IntensityZones;
import com.zensyra.domain.workout.model.Position;
import com.zensyra.domain.workout.model.TrainingLoad;
import com.zensyra.domain.workout.model.TrainingLoadMethod;
import com.zensyra.suunto.dto.SuuntoWorkoutDto;
import com.zensyra.domain.workout.model.IntensityZone;
import com.zensyra.domain.workout.model.IntensityZones;
import com.fasterxml.jackson.databind.JsonNode;

public final class SuuntoWorkoutMapper {

    private SuuntoWorkoutMapper() {
    }

    public static void mapBasicFields(
            WorkoutEntity entity,
            SuuntoWorkoutDto dto) {
        entity.id = dto.workoutKey();
        entity.workoutId = dto.workoutId();
        entity.activityId = dto.activityId();

        entity.startTime = dto.startTimeAsOffsetDateTime();
        entity.stopTime = dto.stopTimeAsOffsetDateTime();
        entity.lastModified = dto.lastModified();

        entity.distanceMeters = dto.distanceMeters();
        entity.durationSeconds = dto.durationSeconds();
        entity.ascentMeters = dto.ascentMeters();
        entity.descentMeters = dto.descentMeters();

        entity.avgPace = dto.avgPace();
        entity.avgSpeed = dto.avgSpeed();
        entity.maxSpeed = dto.maxSpeed();
        entity.avgPower = dto.avgPower();

        entity.energyConsumption = dto.energyConsumption();
        entity.maxAltitude = dto.maxAltitude();
        entity.minAltitude = dto.minAltitude();

        entity.recoveryTime = dto.recoveryTime();
        entity.cumulativeRecoveryTime = dto.cumulativeRecoveryTime();
        entity.timeOffsetInMinutes = dto.timeOffsetInMinutes();

        entity.estimatedFloorsClimbed = dto.estimatedFloorsClimbed();

        entity.edited = dto.isEdited();
        entity.manuallyAdded = dto.isManuallyAdded();

        entity.commentCount = dto.commentCount();
        entity.pictureCount = dto.pictureCount();
        entity.viewCount = dto.viewCount();

        entity.stepCount = dto.stepCount();

        entity.avgHeartRate = dto.avgHeartRate();
        entity.maxHeartRate = dto.maxHeartRate();
        entity.hrMax = dto.hrMax();
        entity.userMaxHeartRate = dto.userMaxHeartRate();
    }

    public static void mapPositions(
            WorkoutEntity entity,
            SuuntoWorkoutDto dto) {
        entity.startPosition = toPosition(dto.startPosition());
        entity.stopPosition = toPosition(dto.stopPosition());
        entity.centerPosition = toPosition(dto.centerPosition());
    }

    public static void mapTrainingLoad(
            WorkoutEntity entity,
            SuuntoWorkoutDto dto) {
        TrainingLoad trainingLoad = new TrainingLoad();

        if (dto.tssList() != null) {
            dto.tssList().forEach(tss -> {
                if (tss.calculationMethod() == null) {
                    return;
                }

                TrainingLoadMethod method = toTrainingLoadMethod(tss);

                switch (tss.calculationMethod().toUpperCase()) {
                    case "HR" -> trainingLoad.hr = method;
                    case "POWER" -> trainingLoad.power = method;
                    case "PACE" -> trainingLoad.pace = method;
                    case "MET" -> trainingLoad.met = method;
                    default -> {
                        // Método no soportado: permanece únicamente en raw_payload.
                    }
                }
            });
        }

        entity.trainingLoad = trainingLoad;
    }

    private static TrainingLoadMethod toTrainingLoadMethod(
            SuuntoWorkoutDto.TssData tss) {
        TrainingLoadMethod method = new TrainingLoadMethod();

        method.trainingStressScore = tss.trainingStressScore();
        method.intensityFactor = tss.intensityFactor();
        method.normalizedPower = tss.normalizedPower();
        method.averageGradeAdjustedPace = tss.averageGradeAdjustedPace();

        return method;
    }

    private static Position toPosition(
            SuuntoWorkoutDto.PositionData position) {
        if (position == null) {
            return null;
        }

        Position result = new Position();

        // Suunto: x = longitude, y = latitude.
        result.longitude = position.x();
        result.latitude = position.y();

        return result;
    }

    public static void mapIntensityZones(
            WorkoutEntity entity,
            SuuntoWorkoutDto dto) {
        if (dto.extensions() == null) {
            return;
        }

        IntensityZones zones = new IntensityZones();

        for (JsonNode extension : dto.extensions()) {
            if (!"IntensityExtension".equals(extension.path("type").asText())) {
                continue;
            }

            JsonNode zonesNode = extension.path("zones");

            mapZoneGroup(
                    zonesNode.path("heartRate"),
                    zones,
                    "hr");

            mapZoneGroup(
                    zonesNode.path("power"),
                    zones,
                    "power");

            mapZoneGroup(
                    zonesNode.path("speed"),
                    zones,
                    "speed");
        }

        entity.intensityZones = zones;
    }

    private static void mapZoneGroup(
            JsonNode group,
            IntensityZones zones,
            String type) {
        mapZone(group.path("zone1"), zones, type, 1);
        mapZone(group.path("zone2"), zones, type, 2);
        mapZone(group.path("zone3"), zones, type, 3);
        mapZone(group.path("zone4"), zones, type, 4);
        mapZone(group.path("zone5"), zones, type, 5);
    }

    private static void mapZone(
            JsonNode node,
            IntensityZones zones,
            String type,
            int number) {
        if (node.isMissingNode() || node.isNull()) {
            return;
        }

        IntensityZone zone = new IntensityZone();

        zone.totalTime = nullableDouble(node, "totalTime");
        zone.lowerLimit = nullableDouble(node, "lowerLimit");

        switch (type) {
            case "hr" -> assignHeartRateZone(zones, number, zone);
            case "power" -> assignPowerZone(zones, number, zone);
            case "speed" -> assignSpeedZone(zones, number, zone);
            default -> {
            }
        }
    }

    private static void assignHeartRateZone(
            IntensityZones zones,
            int number,
            IntensityZone zone) {
        switch (number) {
            case 1 -> zones.heartRateZone1 = zone;
            case 2 -> zones.heartRateZone2 = zone;
            case 3 -> zones.heartRateZone3 = zone;
            case 4 -> zones.heartRateZone4 = zone;
            case 5 -> zones.heartRateZone5 = zone;
            default -> {
            }
        }
    }

    private static void assignPowerZone(
            IntensityZones zones,
            int number,
            IntensityZone zone) {
        switch (number) {
            case 1 -> zones.powerZone1 = zone;
            case 2 -> zones.powerZone2 = zone;
            case 3 -> zones.powerZone3 = zone;
            case 4 -> zones.powerZone4 = zone;
            case 5 -> zones.powerZone5 = zone;
            default -> {
            }
        }
    }

    private static void assignSpeedZone(
            IntensityZones zones,
            int number,
            IntensityZone zone) {
        switch (number) {
            case 1 -> zones.speedZone1 = zone;
            case 2 -> zones.speedZone2 = zone;
            case 3 -> zones.speedZone3 = zone;
            case 4 -> zones.speedZone4 = zone;
            case 5 -> zones.speedZone5 = zone;
            default -> {
            }
        }
    }

    private static Double nullableDouble(
            JsonNode node,
            String field) {
        JsonNode value = node.get(field);

        if (value == null || value.isNull()) {
            return null;
        }

        return value.asDouble();
    }

}