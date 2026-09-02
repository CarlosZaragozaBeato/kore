package com.zensyra.ccollector.core.service.suunto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zensyra.ccollector.core.client.suunto.SuuntoWorkout;
import com.zensyra.ccollector.core.client.suunto.SuuntoWorkoutsResponse;
import com.zensyra.ccollector.core.domain.workout.Workout;
import com.zensyra.ccollector.core.domain.workout.WorkoutType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Blinda la extracción de Suunto contra el esquema JSON REAL (nombres de campo
 * de la Cloud API oficial). Deserializa un payload como el de producción y
 * comprueba que el mapper saca FC (anidada en hrdata), kcal y el tipo desde
 * activityId. Regresión: antes la FC ({@code hravg} plano) salía null y todo se
 * mapeaba a RUNNING.
 */
class SuuntoWorkoutMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Estructura tal cual la documenta Suunto (envoltorio {"payload":[...]}),
    // con la FC dentro de "hrdata". activityId 22 = Trail running -> RUNNING.
    private static final String REAL_PAYLOAD = """
            {
              "payload": [
                {
                  "workoutKey": "6a5f04f30eab57460e8e4b1e",
                  "activityId": 22,
                  "startTime": 1600000000000,
                  "totalTime": 1698.0,
                  "totalDistance": 5256.0,
                  "totalAscent": 42.0,
                  "totalDescent": 40.0,
                  "energyConsumption": 512.0,
                  "stepCount": 5100,
                  "hrdata": { "workoutAvgHR": 156.0, "workoutMaxHR": 181.0 },
                  "avgSpeed": 3.1,
                  "timeOffsetInMinutes": 120
                }
              ]
            }
            """;

    @Test
    void maps_real_payload_fields() throws Exception {
        SuuntoWorkoutsResponse resp = MAPPER.readValue(REAL_PAYLOAD, SuuntoWorkoutsResponse.class);
        assertEquals(1, resp.workoutsOrEmpty().size());

        SuuntoWorkout src = resp.workoutsOrEmpty().get(0);
        Workout w = SuuntoWorkoutMapper.toWorkout(src, 7L);

        assertEquals("6a5f04f30eab57460e8e4b1e", w.sourceId);
        assertEquals(WorkoutType.RUNNING, w.type); // activityId 22 = trail running
        assertEquals(5256.0, w.distanceMeters);
        assertEquals(1698L, w.durationSeconds);
        assertEquals(156, w.avgHeartRate);   // leída de hrdata.workoutAvgHR
        assertEquals(181, w.maxHeartRate);   // leída de hrdata.workoutMaxHR
        assertEquals(512.0, w.energyKcal);   // energyConsumption
        assertEquals(5100, w.stepCount);     // stepCount del resumen
    }

    @Test
    void maps_activity_ids_to_disciplines() {
        assertEquals(WorkoutType.RUNNING, SuuntoWorkoutMapper.mapType(1));
        assertEquals(WorkoutType.CYCLING, SuuntoWorkoutMapper.mapType(2));
        assertEquals(WorkoutType.CYCLING, SuuntoWorkoutMapper.mapType(10)); // mountain biking
        assertEquals(WorkoutType.SWIMMING, SuuntoWorkoutMapper.mapType(21));
        assertEquals(WorkoutType.SWIMMING, SuuntoWorkoutMapper.mapType(85)); // openwater
        assertEquals(WorkoutType.STRENGTH, SuuntoWorkoutMapper.mapType(23)); // gym
        assertEquals(WorkoutType.OTHER, SuuntoWorkoutMapper.mapType(11)); // hiking
        assertEquals(WorkoutType.OTHER, SuuntoWorkoutMapper.mapType(null));
    }

    @Test
    void tolerates_missing_hrdata() {
        SuuntoWorkout src = new SuuntoWorkout("k", 1, 1600000000000L, 1000.0, 300.0,
                null, null, null, null, null);
        Workout w = SuuntoWorkoutMapper.toWorkout(src, 1L);
        assertNull(w.avgHeartRate);
        assertNull(w.maxHeartRate);
        assertNull(w.energyKcal);
        assertNull(w.stepCount);
    }

    @Test
    void backfill_preserves_identity_and_updates_metrics() {
        // Un entreno ya importado, mal mapeado (RUNNING sin FC ni pasos).
        Workout existing = new Workout();
        existing.id = 42L;
        existing.userId = 7L;
        existing.source = com.zensyra.ccollector.core.domain.workout.WorkoutSource.SUUNTO;
        existing.sourceId = "w1";
        java.time.Instant created = java.time.Instant.ofEpochSecond(1_600_000_000L);
        existing.createdAt = created;

        SuuntoWorkout src = new SuuntoWorkout("w1", 2, 1600000000000L, 5000.0, 1500.0,
                null, null, 350.0, new SuuntoWorkout.HrData(140.0, 160.0), 4800);
        SuuntoWorkoutMapper.applyMetrics(existing, src);

        // Identidad intacta…
        assertEquals(42L, existing.id);
        assertEquals(7L, existing.userId);
        assertEquals("w1", existing.sourceId);
        assertEquals(created, existing.createdAt);
        // …y métricas rellenadas.
        assertEquals(WorkoutType.CYCLING, existing.type);
        assertEquals(140, existing.avgHeartRate);
        assertEquals(4800, existing.stepCount);
    }
}
