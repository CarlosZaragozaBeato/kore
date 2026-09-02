package com.zensyra.ccollector.core.resource.weight;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class WeightResourceTest {

    private static final String HEADER = "X-CCollector-Username";

    private String freshUser() {
        String username = "w-" + UUID.randomUUID();
        given().contentType("application/json")
                .body("{\"username\":\"" + username + "\"}")
                .when().post("/api/v1/auth/login").then().statusCode(200);
        return username;
    }

    @Test
    void requires_session() {
        given().when().get("/api/v1/weight").then().statusCode(401);
    }

    @Test
    void upsert_is_one_entry_per_date() {
        String user = freshUser();
        String date = LocalDate.now().toString();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"" + date + "\",\"weightKg\":70.5}")
                .when().post("/api/v1/weight").then().statusCode(200)
                .body("data.weightKg", is(70.5f));
        // misma fecha -> actualiza, no duplica
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"" + date + "\",\"weightKg\":71.2}")
                .when().post("/api/v1/weight").then().statusCode(200)
                .body("data.weightKg", is(71.2f));

        given().header(HEADER, user).when().get("/api/v1/weight")
                .then().statusCode(200)
                .body("data.size()", is(1))
                .body("data[0].weightKg", is(71.2f));
    }

    @Test
    void rejects_non_positive_weight() {
        String user = freshUser();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"" + LocalDate.now() + "\",\"weightKg\":0}")
                .when().post("/api/v1/weight").then().statusCode(400).body("success", is(false));
    }

    @Test
    void goal_get_and_put() {
        String user = freshUser();
        // por defecto vacío
        given().header(HEADER, user).when().get("/api/v1/weight/goal")
                .then().statusCode(200).body("data.minKg", is((Object) null));

        given().header(HEADER, user).contentType("application/json")
                .body("{\"minKg\":68,\"maxKg\":72,\"maintenanceKcal\":2600}")
                .when().put("/api/v1/weight/goal").then().statusCode(200)
                .body("data.minKg", is(68.0f))
                .body("data.maxKg", is(72.0f))
                .body("data.maintenanceKcal", is(2600.0f));
    }

    @Test
    void goal_rejects_min_over_max() {
        String user = freshUser();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"minKg\":80,\"maxKg\":70}")
                .when().put("/api/v1/weight/goal").then().statusCode(400).body("success", is(false));
    }
}
