package com.zensyra.ccollector.core.resource.agent;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasItem;

@QuarkusTest
class AgentResourceTest {

    private static final String HEADER = "X-CCollector-Username";

    @Test
    void manifest_is_public_and_describes_resources() {
        given()
                .when().get("/api/v1/agent/manifest")
                .then().statusCode(200)
                .body("schemaVersion", is(4))
                .body("apiBase", is("/api/v1"))
                .body("sessionHeader", is("X-CCollector-Username"))
                .body("resources.name", hasItem("workouts"))
                .body("resources.name", hasItem("dietPlans"))
                .body("io.import", is("POST /api/v1/session/import"))
                .body("docs.openapi", is("/q/openapi"))
                .body("docs.context", notNullValue());
    }

    @Test
    void context_requires_session() {
        given().when().get("/api/v1/agent/context").then().statusCode(401);
    }

    @Test
    void context_returns_session_and_analytics() {
        String user = "agent-" + UUID.randomUUID();
        given().contentType("application/json")
                .body("{\"username\":\"" + user + "\"}")
                .when().post("/api/v1/auth/login").then().statusCode(200);
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-20\",\"type\":\"RUNNING\",\"distanceMeters\":5000,\"durationSeconds\":1500}")
                .when().post("/api/v1/workouts").then().statusCode(200);

        given().header(HEADER, user)
                .when().get("/api/v1/agent/context")
                .then().statusCode(200)
                .body("session.schemaVersion", is(4))
                .body("session.user.username", is(user))
                .body("session.workouts.size()", is(1))
                .body("analytics.totals.workouts", is(1));
    }

    @Test
    void openapi_spec_is_available() {
        given().when().get("/q/openapi").then().statusCode(200);
    }
}
