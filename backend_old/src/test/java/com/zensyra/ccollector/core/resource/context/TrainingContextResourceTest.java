package com.zensyra.ccollector.core.resource.context;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class TrainingContextResourceTest {

    private static final String HEADER = "X-CCollector-Username";

    private String freshUser() {
        String username = "ctx-" + UUID.randomUUID();
        given().contentType("application/json")
                .body("{\"username\":\"" + username + "\"}")
                .when().post("/api/v1/auth/login").then().statusCode(200);
        return username;
    }

    @Test
    void requires_session() {
        given().when().get("/api/v1/context/training?from=2026-07-01&to=2026-07-31").then().statusCode(401);
    }

    @Test
    void rejects_missing_or_bad_range() {
        String user = freshUser();
        given().header(HEADER, user).when().get("/api/v1/context/training").then().statusCode(400);
        given().header(HEADER, user).when().get("/api/v1/context/training?from=nope&to=2026-07-31")
                .then().statusCode(400);
        given().header(HEADER, user).when().get("/api/v1/context/training?from=2026-07-31&to=2026-07-01")
                .then().statusCode(400);
    }

    @Test
    void bundles_done_planned_and_blocks_in_range_with_guidance() {
        String user = freshUser();

        // Realizado en rango (fecha no futura).
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-21\",\"type\":\"RUNNING\",\"distanceMeters\":8000,\"durationSeconds\":2400}")
                .when().post("/api/v1/workouts").then().statusCode(200);
        // Planificado en rango.
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-23\",\"type\":\"RUNNING\",\"status\":\"ACCEPTED\","
                        + "\"steps\":[{\"kind\":\"INTERVAL\",\"repeat\":5,\"targetDistanceMeters\":1000,"
                        + "\"targetPaceMinSecPerKm\":225,\"targetPaceMaxSecPerKm\":240}]}")
                .when().post("/api/v1/planned").then().statusCode(200);
        // Bloque que solapa el rango.
        given().header(HEADER, user).contentType("application/json")
                .body("{\"level\":\"MICRO\",\"focus\":\"BUILD\",\"name\":\"Semana test\","
                        + "\"startDate\":\"2026-07-20\",\"endDate\":\"2026-07-26\"}")
                .when().post("/api/v1/blocks").then().statusCode(200);
        // Fuera de rango: no debe aparecer.
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-06-01\",\"type\":\"RUNNING\",\"distanceMeters\":5000,\"durationSeconds\":1500}")
                .when().post("/api/v1/workouts").then().statusCode(200);

        given().header(HEADER, user)
                .when().get("/api/v1/context/training?from=2026-07-20&to=2026-07-26")
                .then().statusCode(200)
                .body("data.from", is("2026-07-20"))
                .body("data.to", is("2026-07-26"))
                .body("data.username", notNullValue())
                .body("data.workouts.size()", is(1))
                .body("data.workouts[0].paceSecondsPerKm", is(300))
                .body("data.planned.size()", is(1))
                .body("data.planned[0].steps[0].targetPaceMinSecPerKm", is(225))
                .body("data.blocks.size()", is(1))
                .body("data.blocks[0].name", is("Semana test"))
                .body("data.summary.workoutCount", is(1))
                .body("data.summary.totalDistanceMeters", is(8000.0f))
                .body("data.guidance.returnFormat", notNullValue())
                .body("data.guidance.stepKinds", notNullValue());
    }
}
