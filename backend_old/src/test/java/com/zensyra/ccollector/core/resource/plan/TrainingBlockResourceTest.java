package com.zensyra.ccollector.core.resource.plan;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class TrainingBlockResourceTest {

    private static final String HEADER = "X-CCollector-Username";

    private String freshUser() {
        String username = "blocks-" + UUID.randomUUID();
        given().contentType("application/json")
                .body("{\"username\":\"" + username + "\"}")
                .when().post("/api/v1/auth/login").then().statusCode(200);
        return username;
    }

    @Test
    void list_requires_session() {
        given().when().get("/api/v1/blocks").then().statusCode(401);
    }

    @Test
    void create_a_season_with_nested_meso_and_micro_in_one_call() {
        String user = freshUser();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"level\":\"MACRO\",\"focus\":\"BASE\",\"name\":\"Temporada Ironman\","
                        + "\"startDate\":\"2026-09-01\",\"endDate\":\"2026-11-30\",\"loadStance\":\"BUILD\","
                        + "\"children\":[{\"level\":\"MESO\",\"focus\":\"BUILD\",\"name\":\"Bloque 1\","
                        + "\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-28\","
                        + "\"children\":[{\"level\":\"MICRO\",\"focus\":\"BUILD\",\"name\":\"Semana 1\","
                        + "\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-07\"}]}]}")
                .when().post("/api/v1/blocks")
                .then().statusCode(200)
                .body("data.id", notNullValue())
                .body("data.level", is("MACRO"))
                .body("data.children.size()", is(1))
                .body("data.children[0].level", is("MESO"))
                .body("data.children[0].children[0].level", is("MICRO"));

        // El árbol se lee con una sola raíz que cuelga meso y micro.
        given().header(HEADER, user).when().get("/api/v1/blocks")
                .then().statusCode(200)
                .body("data.size()", is(1))
                .body("data[0].children[0].children[0].name", is("Semana 1"));
    }

    @Test
    void child_level_must_be_finer_than_its_parent() {
        String user = freshUser();
        int macro = given().header(HEADER, user).contentType("application/json")
                .body("{\"level\":\"MACRO\",\"name\":\"M\",\"startDate\":\"2026-09-01\",\"endDate\":\"2026-10-01\"}")
                .when().post("/api/v1/blocks").then().statusCode(200).extract().path("data.id");

        // Colgar otro MACRO de un MACRO no vale (debe ser más fino).
        given().header(HEADER, user).contentType("application/json")
                .body("{\"parentId\":" + macro + ",\"level\":\"MACRO\",\"name\":\"X\","
                        + "\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-15\"}")
                .when().post("/api/v1/blocks").then().statusCode(400);
    }

    @Test
    void summary_derives_planned_vs_done_over_the_block_range() {
        String user = freshUser();
        int block = given().header(HEADER, user).contentType("application/json")
                .body("{\"level\":\"MICRO\",\"name\":\"Semana test\","
                        + "\"startDate\":\"2026-07-20\",\"endDate\":\"2026-07-26\"}")
                .when().post("/api/v1/blocks").then().statusCode(200).extract().path("data.id");

        // Dos sesiones planificadas en el rango y un entreno hecho (fecha no futura).
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-21\",\"type\":\"RUNNING\",\"status\":\"ACCEPTED\",\"targetDistanceMeters\":8000}")
                .when().post("/api/v1/planned").then().statusCode(200);
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-23\",\"type\":\"RUNNING\",\"status\":\"ACCEPTED\",\"targetDistanceMeters\":12000}")
                .when().post("/api/v1/planned").then().statusCode(200);
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-21\",\"type\":\"RUNNING\",\"distanceMeters\":8000,\"durationSeconds\":2400}")
                .when().post("/api/v1/workouts").then().statusCode(200);

        given().header(HEADER, user).when().get("/api/v1/blocks/" + block + "/summary")
                .then().statusCode(200)
                .body("data.plannedSessions", is(2))
                .body("data.completedWorkouts", is(1))
                .body("data.plannedDistanceMeters", is(20000.0f))
                .body("data.weeks", is(1))
                .body("data.adherencePct", is(50));
    }

    @Test
    void delete_cascades_to_descendants() {
        String user = freshUser();
        int macro = given().header(HEADER, user).contentType("application/json")
                .body("{\"level\":\"MACRO\",\"name\":\"Raíz\",\"startDate\":\"2026-09-01\",\"endDate\":\"2026-12-01\","
                        + "\"children\":[{\"level\":\"MESO\",\"name\":\"Hijo\","
                        + "\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-28\"}]}")
                .when().post("/api/v1/blocks").then().statusCode(200).extract().path("data.id");

        given().header(HEADER, user).when().delete("/api/v1/blocks/" + macro).then().statusCode(200);

        given().header(HEADER, user).when().get("/api/v1/blocks")
                .then().statusCode(200).body("data.size()", is(0));
    }

    @Test
    void blocks_roundtrip_through_session_export() {
        String user = freshUser();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"level\":\"MACRO\",\"focus\":\"PEAK\",\"name\":\"Export\","
                        + "\"startDate\":\"2026-09-01\",\"endDate\":\"2026-10-01\","
                        + "\"children\":[{\"level\":\"MESO\",\"name\":\"Sub\","
                        + "\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-14\"}]}")
                .when().post("/api/v1/blocks").then().statusCode(200);

        given().header(HEADER, user).when().get("/api/v1/session/export")
                .then().statusCode(200)
                .body("schemaVersion", is(15))
                .body("trainingBlocks.size()", is(1))
                .body("trainingBlocks[0].focus", is("PEAK"))
                .body("trainingBlocks[0].children[0].name", is("Sub"));
    }
}
