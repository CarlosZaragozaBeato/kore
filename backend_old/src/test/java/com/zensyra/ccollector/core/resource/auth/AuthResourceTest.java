package com.zensyra.ccollector.core.resource.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class AuthResourceTest {

    private static String login(String username) {
        return given()
                .contentType("application/json")
                .body("{\"username\":\"" + username + "\"}")
                .when().post("/api/v1/auth/login")
                .then().statusCode(200)
                .body("success", is(true))
                .body("data.username", is(username))
                .body("data.id", notNullValue())
                .extract().path("data.id").toString();
    }

    @Test
    void login_creates_session_for_new_username() {
        login("user-" + UUID.randomUUID());
    }

    @Test
    void login_is_idempotent_for_same_username() {
        String username = "user-" + UUID.randomUUID();
        String first = login(username);
        String second = login(username);
        org.junit.jupiter.api.Assertions.assertEquals(first, second);
    }

    @Test
    void login_rejects_empty_username() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"  \"}")
                .when().post("/api/v1/auth/login")
                .then().statusCode(400)
                .body("success", is(false));
    }
}
