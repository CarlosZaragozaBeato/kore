package com.zensyra.ccollector.core.resource.workout;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class WorkoutResourceTest {

    private static final String HEADER = "X-CCollector-Username";

    private static String freshUser() {
        String username = "runner-" + UUID.randomUUID();
        given().contentType("application/json")
                .body("{\"username\":\"" + username + "\"}")
                .when().post("/api/v1/auth/login")
                .then().statusCode(200);
        return username;
    }

    @Test
    void list_requires_session() {
        given()
                .when().get("/api/v1/workouts")
                .then().statusCode(401);
    }

    @Test
    void create_list_update_delete_flow() {
        String user = freshUser();

        // create: 5 km in 25 min -> pace 300 s/km
        int id = given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-20\",\"type\":\"RUNNING\",\"distanceMeters\":5000,\"durationSeconds\":1500}")
                .when().post("/api/v1/workouts")
                .then().statusCode(200)
                .body("data.paceSecondsPerKm", is(300))
                .body("data.source", is("MANUAL"))
                .body("data.id", notNullValue())
                .extract().path("data.id");

        // list contains it
        given().header(HEADER, user)
                .when().get("/api/v1/workouts")
                .then().statusCode(200)
                .body("data.size()", is(1))
                .body("data[0].id", is(id));

        // update notes and effort
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-20\",\"type\":\"RUNNING\",\"distanceMeters\":5000,\"durationSeconds\":1500,\"perceivedEffort\":7,\"notes\":\"tempo\"}")
                .when().put("/api/v1/workouts/" + id)
                .then().statusCode(200)
                .body("data.perceivedEffort", is(7))
                .body("data.notes", is("tempo"));

        // delete, then 404
        given().header(HEADER, user)
                .when().delete("/api/v1/workouts/" + id)
                .then().statusCode(200);
        given().header(HEADER, user)
                .when().get("/api/v1/workouts/" + id)
                .then().statusCode(404);
    }

    @Test
    void rejects_future_date() {
        String user = freshUser();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2999-01-01\",\"type\":\"RUNNING\"}")
                .when().post("/api/v1/workouts")
                .then().statusCode(400)
                .body("success", is(false));
    }

    @Test
    void workouts_are_isolated_per_user() {
        String owner = freshUser();
        String other = freshUser();

        int id = given().header(HEADER, owner).contentType("application/json")
                .body("{\"date\":\"2026-07-19\",\"type\":\"RUNNING\",\"distanceMeters\":10000,\"durationSeconds\":3000}")
                .when().post("/api/v1/workouts")
                .then().statusCode(200).extract().path("data.id");

        // other user does not see it and cannot fetch it
        given().header(HEADER, other)
                .when().get("/api/v1/workouts")
                .then().statusCode(200).body("data.size()", is(0));
        given().header(HEADER, other)
                .when().get("/api/v1/workouts/" + id)
                .then().statusCode(404);
    }
}
