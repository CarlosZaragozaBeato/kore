package com.zensyra.ccollector.core.resource.nutrition;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
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
    void ingredient_with_image_and_recipes_by_ingredient() {
        String user = freshUser();
        int ingredientId = given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Avena\",\"baseUnit\":\"GRAM\",\"imageUrl\":\"https://x/avena.jpg\","
                        + "\"calories\":389,\"carbs\":66,\"protein\":17}")
                .when().post("/api/v1/ingredients")
                .then().statusCode(200)
                .body("data.imageUrl", is("https://x/avena.jpg"))
                .extract().path("data.id");

        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Porridge\",\"ingredients\":[{\"name\":\"Avena\",\"quantity\":80,"
                        + "\"unit\":\"g\",\"ingredientId\":" + ingredientId + "}]}")
                .when().post("/api/v1/recipes").then().statusCode(200);
        // otra receta sin ese ingrediente, no debe aparecer
        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Otra\",\"ingredients\":[{\"name\":\"Huevo\",\"quantity\":2}]}")
                .when().post("/api/v1/recipes").then().statusCode(200);

        given().header(HEADER, user).when().get("/api/v1/recipes/by-ingredient/" + ingredientId)
                .then().statusCode(200)
                .body("data.size()", is(1))
                .body("data[0].name", is("Porridge"));
    }

    @Test
    void recommendation_focuses_protein_when_resting() {
        String user = freshUser();
        // sin entrenos → descanso → foco en proteína
        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Pollo con verduras\",\"calories\":400,\"protein\":45,\"carbs\":10,\"fat\":8}")
                .when().post("/api/v1/recipes").then().statusCode(200);
        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Pasta al pesto\",\"calories\":600,\"protein\":15,\"carbs\":90,\"fat\":18}")
                .when().post("/api/v1/recipes").then().statusCode(200);

        given().header(HEADER, user).when().get("/api/v1/recipes/recommend")
                .then().statusCode(200)
                .body("context.intensity", is("REST"))
                .body("context.focus", is("PROTEIN"))
                .body("recommendations[0].name", is("Pollo con verduras"));
    }

    @Test
    void recommendation_focuses_carbs_after_heavy_training() {
        String user = freshUser();
        String today = LocalDate.now().toString();
        // 5 entrenos hoy → carga alta → repostaje de carbohidratos
        for (int i = 0; i < 5; i++) {
            given().header(HEADER, user).contentType("application/json")
                    .body("{\"date\":\"" + today + "\",\"type\":\"RUNNING\",\"durationSeconds\":3600,"
                            + "\"energyKcal\":800}")
                    .when().post("/api/v1/workouts").then().statusCode(200);
        }
        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Pollo con verduras\",\"calories\":400,\"protein\":45,\"carbs\":10,\"fat\":8}")
                .when().post("/api/v1/recipes").then().statusCode(200);
        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Pasta al pesto\",\"calories\":600,\"protein\":15,\"carbs\":90,\"fat\":18}")
                .when().post("/api/v1/recipes").then().statusCode(200);

        given().header(HEADER, user).when().get("/api/v1/recipes/recommend")
                .then().statusCode(200)
                .body("context.intensity", is("HIGH"))
                .body("context.focus", is("CARB"))
                .body("recommendations[0].name", is("Pasta al pesto"));
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
                .body("schemaVersion", is(15))
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
