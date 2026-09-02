package com.zensyra.ccollector.core.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class PingResourceTest {

    @Test
    void ping_returns_success_envelope() {
        given()
                .when().get("/api/v1/ping")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.status", is("up"));
    }

    @Test
    void health_endpoint_is_up() {
        given()
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }
}
