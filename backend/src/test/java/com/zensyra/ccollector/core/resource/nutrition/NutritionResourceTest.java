package com.zensyra.ccollector.core.resource.nutrition;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class NutritionResourceTest {

    private static final String HEADER = "X-CCollector-Username";

    private String freshUser() {
        String username = "nut-" + UUID.randomUUID();
        given().contentType("application/json")
                .body("{\"username\":\"" + username + "\"}")
                .when().post("/api/v1/auth/login").then().statusCode(200);
        return username;
    }

    @Test
    void endpoints_require_session() {
        given().when().get("/api/v1/recipes").then().statusCode(401);
        given().when().get("/api/v1/diet-plans").then().statusCode(401);
    }

    @Test
    void recipe_with_ingredients_crud() {
        String user = freshUser();
        int id = given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Avena proteica\",\"servings\":1,\"calories\":450,\"protein\":30,"
                        + "\"steps\":\"Mezclar todo\\nCalentar\",\"ingredients\":["
                        + "{\"name\":\"Avena\",\"quantity\":80,\"unit\":\"g\"},"
                        + "{\"name\":\"Proteína\",\"quantity\":30,\"unit\":\"g\"}]}")
                .when().post("/api/v1/recipes")
                .then().statusCode(200)
                .body("data.name", is("Avena proteica"))
                .body("data.calories", is(450.0f))
                .body("data.ingredients.size()", is(2))
                .body("data.ingredients[0].name", is("Avena"))
                .extract().path("data.id");

        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Avena proteica v2\",\"ingredients\":[{\"name\":\"Avena\",\"quantity\":100,\"unit\":\"g\"}]}")
                .when().put("/api/v1/recipes/" + id)
                .then().statusCode(200)
                .body("data.name", is("Avena proteica v2"))
                .body("data.ingredients.size()", is(1));

        given().header(HEADER, user).when().delete("/api/v1/recipes/" + id).then().statusCode(200);
        given().header(HEADER, user).when().get("/api/v1/recipes/" + id).then().statusCode(404);
    }

    @Test
    void diet_plan_with_meals_and_targets() {
        String user = freshUser();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Volumen\",\"startDate\":\"2026-07-13\",\"endDate\":\"2026-07-19\","
                        + "\"targetCalories\":2800,\"targetProtein\":160,\"meals\":["
                        + "{\"date\":\"2026-07-13\",\"mealType\":\"BREAKFAST\",\"recipeName\":\"Avena proteica\"},"
                        + "{\"date\":\"2026-07-13\",\"mealType\":\"LUNCH\",\"recipeName\":\"Arroz con pollo\"}]}")
                .when().post("/api/v1/diet-plans")
                .then().statusCode(200)
                .body("data.id", notNullValue())
                .body("data.targetCalories", is(2800.0f))
                .body("data.meals.size()", is(2))
                .body("data.meals[0].mealType", is("BREAKFAST"));
    }

    @Test
    void diet_plan_rejects_end_before_start() {
        String user = freshUser();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Mal\",\"startDate\":\"2026-07-19\",\"endDate\":\"2026-07-13\"}")
                .when().post("/api/v1/diet-plans")
                .then().statusCode(400).body("success", is(false));
    }

    @Test
    void nutrition_roundtrips_through_session_export() {
        String user = freshUser();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Tortilla\",\"ingredients\":[{\"name\":\"Huevo\",\"quantity\":3,\"unit\":\"ud\"}]}")
                .when().post("/api/v1/recipes").then().statusCode(200);
        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Definición\",\"startDate\":\"2026-07-10\",\"meals\":["
                        + "{\"date\":\"2026-07-10\",\"mealType\":\"DINNER\",\"recipeName\":\"Tortilla\"}]}")
                .when().post("/api/v1/diet-plans").then().statusCode(200);

        given().header(HEADER, user).when().get("/api/v1/session/export")
                .then().statusCode(200)
                .body("schemaVersion", is(4))
                .body("recipes.size()", is(1))
                .body("recipes[0].ingredients[0].name", is("Huevo"))
                .body("dietPlans.size()", is(1))
                .body("dietPlans[0].meals[0].mealType", is("DINNER"));

        String imported = "nutimp-" + UUID.randomUUID();
        String doc = "{\"schemaVersion\":4,\"user\":{\"username\":\"" + imported + "\"},"
                + "\"workouts\":[],\"plans\":[],\"exercises\":[],\"routines\":[],\"strengthSessions\":[],"
                + "\"recipes\":[{\"name\":\"Batido\",\"ingredients\":[{\"name\":\"Plátano\",\"quantity\":1,\"unit\":\"ud\"}]}],"
                + "\"dietPlans\":[{\"name\":\"Import\",\"startDate\":\"2026-07-10\","
                + "\"meals\":[{\"date\":\"2026-07-10\",\"mealType\":\"SNACK\",\"recipeName\":\"Batido\"}]}]}";
        given().contentType("application/json").body(doc)
                .when().post("/api/v1/session/import").then().statusCode(200);

        given().header(HEADER, imported).when().get("/api/v1/recipes")
                .then().statusCode(200).body("data.size()", is(1))
                .body("data[0].name", is("Batido"));
        given().header(HEADER, imported).when().get("/api/v1/diet-plans")
                .then().statusCode(200).body("data.size()", is(1))
                .body("data[0].meals[0].mealType", is("SNACK"));
    }
}
