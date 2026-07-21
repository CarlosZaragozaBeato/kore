package com.zensyra.ccollector.core.resource.session;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class SessionResourceTest {

    private static final String HEADER = "X-CCollector-Username";

    private static String freshUser() {
        String username = "exp-" + UUID.randomUUID();
        given().contentType("application/json")
                .body("{\"username\":\"" + username + "\"}")
                .when().post("/api/v1/auth/login")
                .then().statusCode(200);
        return username;
    }

    @Test
    void export_returns_versioned_document_with_workouts() {
        String user = freshUser();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-18\",\"type\":\"RUNNING\",\"distanceMeters\":3000,\"durationSeconds\":900}")
                .when().post("/api/v1/workouts").then().statusCode(200);

        given().header(HEADER, user)
                .when().get("/api/v1/session/export")
                .then().statusCode(200)
                .body("schemaVersion", is(4))
                .body("user.username", is(user))
                .body("workouts.size()", is(1))
                .body("workouts[0].distanceMeters", is(3000.0f));
    }

    @Test
    void import_recreates_session_on_a_fresh_username() {
        String imported = "imp-" + UUID.randomUUID();
        String doc = "{\"schemaVersion\":1,\"exportedAt\":\"2026-07-21T10:00:00Z\","
                + "\"user\":{\"username\":\"" + imported + "\",\"createdAt\":\"2026-07-01T00:00:00Z\"},"
                + "\"workouts\":[{\"date\":\"2026-07-15\",\"type\":\"RUNNING\",\"distanceMeters\":8000,"
                + "\"durationSeconds\":2400,\"source\":\"MANUAL\",\"createdAt\":\"2026-07-15T18:00:00Z\"}]}";

        given().contentType("application/json").body(doc)
                .when().post("/api/v1/session/import")
                .then().statusCode(200)
                .body("success", is(true))
                .body("data.username", is(imported));

        // the imported workout is now visible under that session
        given().header(HEADER, imported)
                .when().get("/api/v1/workouts")
                .then().statusCode(200)
                .body("data.size()", is(1))
                .body("data[0].paceSecondsPerKm", is(300));

        // re-importing the same username conflicts
        given().contentType("application/json").body(doc)
                .when().post("/api/v1/session/import")
                .then().statusCode(409)
                .body("success", is(false));
    }
}
