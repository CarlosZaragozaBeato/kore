package com.zensyra.ccollector.core.resource.analytics;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class CompareResourceTest {

    private static final String HEADER = "X-CCollector-Username";

    private String freshUser() {
        String username = "cmp-" + UUID.randomUUID();
        given().contentType("application/json")
                .body("{\"username\":\"" + username + "\"}")
                .when().post("/api/v1/auth/login").then().statusCode(200);
        return username;
    }

    @Test
    void requires_session() {
        given().when().get("/api/v1/analytics/compare").then().statusCode(401);
    }

    @Test
    void compares_two_explicit_weeks_by_day() {
        String user = freshUser();
        // semana actual: 60 min · RPE 5 -> carga 300, el miércoles 2026-07-15
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-15\",\"type\":\"RUNNING\",\"distanceMeters\":10000,"
                        + "\"durationSeconds\":3600,\"perceivedEffort\":5}")
                .when().post("/api/v1/workouts").then().statusCode(200);
        // semana previa: 30 min · RPE 5 -> carga 150, el miércoles 2026-07-08
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-08\",\"type\":\"RUNNING\",\"distanceMeters\":5000,"
                        + "\"durationSeconds\":1800,\"perceivedEffort\":5}")
                .when().post("/api/v1/workouts").then().statusCode(200);

        given().header(HEADER, user)
                .queryParam("currentStart", "2026-07-13").queryParam("currentEnd", "2026-07-19")
                .queryParam("previousStart", "2026-07-06").queryParam("previousEnd", "2026-07-12")
                .when().get("/api/v1/analytics/compare")
                .then().statusCode(200)
                .body("current.load", is(300))
                .body("previous.load", is(150))
                .body("current.workouts", is(1))
                .body("current.days.size()", is(7))
                .body("dailyDeltas.size()", is(7))
                // ambos entrenos caen en el 3er día del rango (índice 2 = miércoles)
                .body("dailyDeltas[2].currentLoad", is(300))
                .body("dailyDeltas[2].previousLoad", is(150))
                .body("dailyDeltas[2].deltaLoad", is(150))
                .body("dailyDeltas[2].cumulativeDeltaLoad", is(150))
                .body("current.byDiscipline.find { it.key == 'RUNNING' }.load", is(300));
    }

    @Test
    void defaults_to_current_vs_previous_week() {
        String user = freshUser();
        given().header(HEADER, user)
                .when().get("/api/v1/analytics/compare")
                .then().statusCode(200)
                .body("current.days.size()", is(7))
                .body("previous.days.size()", is(7))
                .body("signals.acwrState", is("UNKNOWN"));
    }

    @Test
    void rejects_bad_date() {
        String user = freshUser();
        given().header(HEADER, user).queryParam("currentStart", "nope")
                .when().get("/api/v1/analytics/compare")
                .then().statusCode(400).body("success", is(false));
    }
}
