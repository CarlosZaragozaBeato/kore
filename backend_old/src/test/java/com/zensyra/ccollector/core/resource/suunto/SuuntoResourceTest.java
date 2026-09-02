package com.zensyra.ccollector.core.resource.suunto;

import com.zensyra.ccollector.core.client.suunto.SuuntoApiClient;
import com.zensyra.ccollector.core.client.suunto.SuuntoOAuthClient;
import com.zensyra.ccollector.core.client.suunto.SuuntoTokenResponse;
import com.zensyra.ccollector.core.client.suunto.SuuntoWorkout;
import com.zensyra.ccollector.core.client.suunto.SuuntoWorkoutsResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class SuuntoResourceTest {

    private static final String HEADER = "X-CCollector-Username";

    @InjectMock
    @RestClient
    SuuntoOAuthClient oauthClient;

    @InjectMock
    @RestClient
    SuuntoApiClient apiClient;

    private String user;

    @BeforeEach
    void setup() {
        user = "suunto-" + UUID.randomUUID();
        given().contentType("application/json")
                .body("{\"username\":\"" + user + "\"}")
                .when().post("/api/v1/auth/login").then().statusCode(200);
    }

    private void configureSuunto() {
        given().header(HEADER, user).contentType("application/json")
                .body("{\"enabled\":true,\"clientId\":\"cid\",\"clientSecret\":\"secret\","
                        + "\"refreshToken\":\"rt\",\"subscriptionKey\":\"subkey\"}")
                .when().put("/api/v1/suunto/settings")
                .then().statusCode(200);
    }

    @Test
    void settings_never_expose_secrets() {
        configureSuunto();
        given().header(HEADER, user)
                .when().get("/api/v1/suunto/settings")
                .then().statusCode(200)
                .body("data.enabled", is(true))
                .body("data.clientId", is("cid"))
                .body("data.hasClientSecret", is(true))
                .body("data.hasRefreshToken", is(true))
                .body("data.hasSubscriptionKey", is(true))
                // ni el DTO ni el JSON contienen los valores secretos
                .body("data.clientSecret", is((Object) null))
                .body("data.refreshToken", is((Object) null));
    }

    @Test
    void sync_imports_then_backfills_without_duplicating() {
        configureSuunto();
        when(oauthClient.refresh(anyString(), anyString(), anyString()))
                .thenReturn(new SuuntoTokenResponse("access-123", "bearer", "rt", 86400L, "workout"));
        when(apiClient.listWorkouts(anyString(), anyString()))
                .thenReturn(new SuuntoWorkoutsResponse(List.of(
                        // activityId 1 = Running, FC anidada en hrdata, kcal en energyConsumption, pasos
                        new SuuntoWorkout("w1", 1, 1_600_000_000_000L, 10000.0, 3000.0, 120.0, 110.0,
                                700.0, new SuuntoWorkout.HrData(150.0, 175.0), 9000),
                        // activityId 2 = Cycling
                        new SuuntoWorkout("w2", 2, 1_600_100_000_000L, 5000.0, 1500.0, null, null,
                                350.0, new SuuntoWorkout.HrData(140.0, 160.0), null))));

        // first sync imports both
        given().header(HEADER, user)
                .when().post("/api/v1/suunto/sync")
                .then().statusCode(200)
                .body("data.imported", is(2))
                .body("data.updated", is(0))
                .body("data.skipped", is(0))
                .body("data.total", is(2));

        // workouts now present, tagged as SUUNTO, con FC (anidada), tipo mapeado
        // desde activityId, pasos y cadencia/zancada derivadas.
        given().header(HEADER, user)
                .when().get("/api/v1/workouts")
                .then().statusCode(200)
                .body("data.size()", is(2))
                .body("data.source", hasItems("SUUNTO"))
                .body("data.avgHeartRate", hasItems(150, 140))
                .body("data.maxHeartRate", hasItems(175, 160))
                .body("data.type", hasItems("RUNNING", "CYCLING"))
                .body("data.stepCount", hasItems(9000))
                // cadencia = 9000 pasos / 50 min = 180 spm; zancada = 10000/9000 ≈ 1.11 m
                .body("data.avgCadenceSpm", hasItems(180))
                .body("data.strideLengthMeters", hasItems(1.11f));

        // second sync: mismos workoutKey pero datos corregidos -> backfill in-place
        // (regresión: antes se ignoraban por dedup y quedaban obsoletos).
        when(apiClient.listWorkouts(anyString(), anyString()))
                .thenReturn(new SuuntoWorkoutsResponse(List.of(
                        new SuuntoWorkout("w1", 1, 1_600_000_000_000L, 10000.0, 3000.0, 120.0, 110.0,
                                700.0, new SuuntoWorkout.HrData(158.0, 182.0), 9200),
                        new SuuntoWorkout("w2", 2, 1_600_100_000_000L, 5000.0, 1500.0, null, null,
                                350.0, new SuuntoWorkout.HrData(140.0, 160.0), null))));

        given().header(HEADER, user)
                .when().post("/api/v1/suunto/sync")
                .then().statusCode(200)
                .body("data.imported", is(0))
                .body("data.updated", is(2))
                .body("data.skipped", is(0));

        // sin duplicados y con los valores rellenados
        given().header(HEADER, user)
                .when().get("/api/v1/workouts")
                .then().statusCode(200)
                .body("data.size()", is(2))
                .body("data.avgHeartRate", hasItems(158))
                .body("data.stepCount", hasItems(9200));
    }

    @Test
    void purge_removes_old_suunto_but_keeps_manual() {
        configureSuunto();
        when(oauthClient.refresh(anyString(), anyString(), anyString()))
                .thenReturn(new SuuntoTokenResponse("access-123", "bearer", "rt", 86400L, "workout"));
        // startTime 1_600_000_000_000 ms = 2020-09 -> muy anterior a la ventana.
        when(apiClient.listWorkouts(anyString(), anyString()))
                .thenReturn(new SuuntoWorkoutsResponse(List.of(
                        new SuuntoWorkout("old1", 1, 1_600_000_000_000L, 10000.0, 3000.0, null, null,
                                700.0, new SuuntoWorkout.HrData(150.0, 175.0), 9000),
                        new SuuntoWorkout("old2", 2, 1_600_100_000_000L, 5000.0, 1500.0, null, null,
                                350.0, new SuuntoWorkout.HrData(140.0, 160.0), null))));
        given().header(HEADER, user).when().post("/api/v1/suunto/sync").then().statusCode(200);

        // Un entreno MANUAL de hoy: la retención no debe tocarlo nunca.
        String today = java.time.LocalDate.now().toString();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"" + today + "\",\"type\":\"RUNNING\",\"distanceMeters\":8000,\"durationSeconds\":2400}")
                .when().post("/api/v1/workouts").then().statusCode(200);

        given().header(HEADER, user)
                .when().post("/api/v1/suunto/purge")
                .then().statusCode(200)
                .body("data.purged", is(2));

        given().header(HEADER, user)
                .when().get("/api/v1/workouts")
                .then().statusCode(200)
                .body("data.size()", is(1))
                .body("data[0].source", is("MANUAL"));
    }

    @Test
    void sync_requires_configuration() {
        // enabled=false by default -> 400
        given().header(HEADER, user)
                .when().post("/api/v1/suunto/sync")
                .then().statusCode(400)
                .body("success", is(false));
    }

    @Test
    void sync_surfaces_suunto_errors_as_502() {
        configureSuunto();
        when(oauthClient.refresh(anyString(), anyString(), anyString()))
                .thenThrow(new WebApplicationException("boom", 401));

        given().header(HEADER, user)
                .when().post("/api/v1/suunto/sync")
                .then().statusCode(502)
                .body("success", is(false));
    }

    @Test
    void sync_requires_session() {
        given().when().post("/api/v1/suunto/sync").then().statusCode(401);
    }
}
