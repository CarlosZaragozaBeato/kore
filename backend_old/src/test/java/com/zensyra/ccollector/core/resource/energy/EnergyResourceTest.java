package com.zensyra.ccollector.core.resource.energy;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class EnergyResourceTest {

    private static final String HEADER = "X-CCollector-Username";

    private String freshUser() {
        String username = "en-" + UUID.randomUUID();
        given().contentType("application/json")
                .body("{\"username\":\"" + username + "\"}")
                .when().post("/api/v1/auth/login").then().statusCode(200);
        return username;
    }

    @Test
    void requires_session() {
        given().when().get("/api/v1/energy/summary").then().statusCode(401);
    }

    @Test
    void computes_consumed_vs_burned_and_state() {
        String user = freshUser();
        String date = LocalDate.now().toString();

        // receta con 500 kcal
        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Comida X\",\"calories\":500}")
                .when().post("/api/v1/recipes").then().statusCode(200);
        // dieta con una comida de hoy que referencia la receta por nombre
        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Plan\",\"startDate\":\"" + date + "\",\"meals\":["
                        + "{\"date\":\"" + date + "\",\"mealType\":\"LUNCH\",\"recipeName\":\"Comida X\"}]}")
                .when().post("/api/v1/diet-plans").then().statusCode(200);
        // entreno de hoy con 300 kcal quemadas
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"" + date + "\",\"type\":\"RUNNING\",\"durationSeconds\":1800,\"energyKcal\":300}")
                .when().post("/api/v1/workouts").then().statusCode(200);
        // objetivo de mantenimiento
        given().header(HEADER, user).contentType("application/json")
                .body("{\"maintenanceKcal\":2000}")
                .when().put("/api/v1/weight/goal").then().statusCode(200);

        // balance del día: consumidas 500, quemadas 300, balance 200 -> DEFICIT vs 2000
        given().header(HEADER, user).when().get("/api/v1/energy/summary")
                .then().statusCode(200)
                .body("maintenanceKcal", is(2000.0f))
                .body("days.size()", is(14))
                .body("weeks.size()", is(8))
                .body("days.find { it.date == '" + date + "' }.consumedKcal", is(500))
                .body("days.find { it.date == '" + date + "' }.burnedKcal", is(300))
                .body("days.find { it.date == '" + date + "' }.balanceKcal", is(200))
                .body("days.find { it.date == '" + date + "' }.state", is("DEFICIT"));
    }

    @Test
    void state_is_unknown_without_maintenance_goal() {
        String user = freshUser();
        given().header(HEADER, user).when().get("/api/v1/energy/summary")
                .then().statusCode(200)
                .body("maintenanceKcal", is((Object) null))
                .body("days[0].state", is("UNKNOWN"));
    }
}
