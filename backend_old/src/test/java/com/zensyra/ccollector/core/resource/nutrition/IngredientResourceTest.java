package com.zensyra.ccollector.core.resource.nutrition;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class IngredientResourceTest {

    private static final String HEADER = "X-CCollector-Username";

    private String freshUser() {
        String username = "ing-" + UUID.randomUUID();
        given().contentType("application/json")
                .body("{\"username\":\"" + username + "\"}")
                .when().post("/api/v1/auth/login").then().statusCode(200);
        return username;
    }

    @Test
    void requires_session() {
        given().when().get("/api/v1/ingredients").then().statusCode(401);
    }

    @Test
    void ingredient_crud_with_nutrition() {
        String user = freshUser();
        int id = given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Avena\",\"baseUnit\":\"GRAM\",\"calories\":389,\"protein\":16.9,"
                        + "\"carbs\":66,\"fat\":6.9,\"fiber\":10.6}")
                .when().post("/api/v1/ingredients")
                .then().statusCode(200)
                .body("data.name", is("Avena"))
                .body("data.baseUnit", is("GRAM"))
                .body("data.calories", is(389.0f))
                .extract().path("data.id");

        given().header(HEADER, user).when().get("/api/v1/ingredients")
                .then().statusCode(200).body("data.size()", is(1));

        given().header(HEADER, user).when().delete("/api/v1/ingredients/" + id).then().statusCode(200);
        given().header(HEADER, user).when().get("/api/v1/ingredients")
                .then().statusCode(200).body("data.size()", is(0));
    }

    @Test
    void rejects_negative_nutrition() {
        String user = freshUser();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Malo\",\"baseUnit\":\"GRAM\",\"calories\":-5}")
                .when().post("/api/v1/ingredients")
                .then().statusCode(400).body("success", is(false));
    }

    @Test
    void seed_loads_examples_and_is_idempotent() {
        String user = freshUser();
        int added = given().header(HEADER, user)
                .when().post("/api/v1/ingredients/seed")
                .then().statusCode(200)
                .body("data.added", is(20))
                .body("data.skipped", is(0))
                .extract().path("data.added");
        // se cargaron 20 ejemplos
        given().header(HEADER, user).when().get("/api/v1/ingredients")
                .then().statusCode(200).body("data.size()", is(added));

        // segunda siembra no duplica: todos omitidos
        given().header(HEADER, user)
                .when().post("/api/v1/ingredients/seed")
                .then().statusCode(200)
                .body("data.added", is(0))
                .body("data.skipped", is(20));
    }
}
