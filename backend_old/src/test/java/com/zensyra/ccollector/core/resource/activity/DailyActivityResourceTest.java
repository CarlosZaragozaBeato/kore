package com.zensyra.ccollector.core.resource.activity;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class DailyActivityResourceTest {

    private static final String HEADER = "X-CCollector-Username";

    private String freshUser() {
        String username = "act-" + UUID.randomUUID();
        given().contentType("application/json")
                .body("{\"username\":\"" + username + "\"}")
                .when().post("/api/v1/auth/login").then().statusCode(200);
        return username;
    }

    @Test
    void requires_session() {
        given().when().get("/api/v1/activity/daily").then().statusCode(401);
    }

    @Test
    void upsert_is_idempotent_by_date() {
        String user = freshUser();
        String today = java.time.LocalDate.now().toString();

        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"" + today + "\",\"steps\":8000,\"burnedKcal\":2400}")
                .when().post("/api/v1/activity/daily")
                .then().statusCode(200)
                .body("data.steps", is(8000))
                .body("data.burnedKcal", is(2400.0f));

        // mismo día -> actualiza en sitio, no duplica
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"" + today + "\",\"steps\":9500,\"burnedKcal\":2600}")
                .when().post("/api/v1/activity/daily").then().statusCode(200);

        given().header(HEADER, user).when().get("/api/v1/activity/daily")
                .then().statusCode(200)
                .body("data.size()", is(1))
                .body("data[0].steps", is(9500));
    }

    @Test
    void daily_burn_overrides_workout_estimate_in_energy_balance() {
        String user = freshUser();
        String today = java.time.LocalDate.now().toString();

        // Un entreno hoy -> quema estimada. Sin actividad diaria, el balance usa esa estimación.
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"" + today + "\",\"type\":\"RUNNING\",\"distanceMeters\":10000,\"durationSeconds\":3000}")
                .when().post("/api/v1/workouts").then().statusCode(200);

        // Fija la quema diaria total: sustituye a la estimación (evita doble conteo).
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"" + today + "\",\"steps\":12000,\"burnedKcal\":2800}")
                .when().post("/api/v1/activity/daily").then().statusCode(200);

        given().header(HEADER, user).when().get("/api/v1/energy/summary")
                .then().statusCode(200)
                .body("days.find { it.date == '" + today + "' }.burnedKcal", is(2800));
    }

    @Test
    void roundtrips_through_session_export_v10() {
        String user = freshUser();
        String today = java.time.LocalDate.now().toString();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"" + today + "\",\"steps\":7777,\"burnedKcal\":2100}")
                .when().post("/api/v1/activity/daily").then().statusCode(200);

        given().header(HEADER, user).when().get("/api/v1/session/export")
                .then().statusCode(200)
                .body("schemaVersion", is(15))
                .body("dailyActivities.size()", is(1))
                .body("dailyActivities[0].steps", is(7777));

        String imported = "actimp-" + UUID.randomUUID();
        String doc = "{\"schemaVersion\":10,\"user\":{\"username\":\"" + imported + "\"},"
                + "\"dailyActivities\":[{\"date\":\"" + today + "\",\"steps\":5000,\"burnedKcal\":1900}]}";
        given().contentType("application/json").body(doc)
                .when().post("/api/v1/session/import").then().statusCode(200);

        given().header(HEADER, imported).when().get("/api/v1/activity/daily")
                .then().statusCode(200).body("data.size()", is(1))
                .body("data[0].steps", is(5000));
    }
}
