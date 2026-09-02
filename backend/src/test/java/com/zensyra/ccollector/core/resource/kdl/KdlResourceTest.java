package com.zensyra.ccollector.core.resource.kdl;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class KdlResourceTest {

    private static final String HEADER = "X-CCollector-Username";

    private String freshUser() {
        String username = "kdl-" + UUID.randomUUID();
        given().contentType("application/json")
                .body("{\"username\":\"" + username + "\"}")
                .when().post("/api/v1/auth/login").then().statusCode(200);
        return username;
    }

    @Test
    void requires_session() {
        given().when().get("/api/v1/kdl/export").then().statusCode(401);
        given().contentType("application/json").body("{\"kore\":\"kdl\",\"items\":[]}")
                .when().post("/api/v1/kdl/import").then().statusCode(401);
    }

    @Test
    void batch_import_isolates_bad_items() {
        String user = freshUser();
        String doc = "{\"kore\":\"kdl\",\"kdlVersion\":1,\"items\":["
                + "{\"koreType\":\"ingredient\",\"schemaVersion\":1,\"payload\":{\"name\":\"Avena\",\"baseUnit\":\"GRAM\",\"calories\":389}},"
                + "{\"koreType\":\"workout\",\"schemaVersion\":1,\"payload\":{\"date\":\"2026-07-15\",\"type\":\"RUNNING\",\"distanceMeters\":10000,\"durationSeconds\":3600}},"
                + "{\"koreType\":\"bogus\",\"schemaVersion\":1,\"payload\":{}},"
                + "{\"koreType\":\"ingredient\",\"schemaVersion\":9,\"payload\":{\"name\":\"Futuro\"}}"
                + "]}";
        given().header(HEADER, user).contentType("application/json").body(doc)
                .when().post("/api/v1/kdl/import")
                .then().statusCode(200)
                .body("data.imported", is(2))
                .body("data.skipped", is(2))
                .body("data.byType.ingredient", is(1))
                .body("data.byType.workout", is(1))
                .body("data.errors.size()", is(2));

        given().header(HEADER, user).when().get("/api/v1/ingredients")
                .then().statusCode(200).body("data.size()", is(1)).body("data[0].name", is("Avena"));
        given().header(HEADER, user).when().get("/api/v1/workouts")
                .then().statusCode(200).body("data.size()", is(1));
    }

    @Test
    void section_import_accepts_bare_payload_array_and_kdl_doc() {
        String user = freshUser();

        // (1) Un plan COMPLETO como payload suelto -> POST /kdl/import/plan
        String plan = "{\"name\":\"Maratón 8 semanas\",\"goal\":\"MARATHON\",\"startDate\":\"2026-08-01\","
                + "\"sessions\":[{\"date\":\"2026-08-01\",\"type\":\"RUNNING\",\"targetDistanceMeters\":12000},"
                + "{\"date\":\"2026-08-03\",\"type\":\"RUNNING\",\"targetDistanceMeters\":8000}]}";
        given().header(HEADER, user).contentType("application/json").body(plan)
                .when().post("/api/v1/kdl/import/plan")
                .then().statusCode(200)
                .body("data.imported", is(1))
                .body("data.skipped", is(0))
                .body("data.byType.plan", is(1));

        given().header(HEADER, user).when().get("/api/v1/plans")
                .then().statusCode(200).body("data.size()", is(1))
                .body("data[0].sessions.size()", is(2));

        // (2) Un ARRAY de recetas -> cada elemento es un recipe
        String recipes = "[{\"name\":\"Tortilla\",\"calories\":250},{\"name\":\"Avena\",\"calories\":300}]";
        given().header(HEADER, user).contentType("application/json").body(recipes)
                .when().post("/api/v1/kdl/import/recipe")
                .then().statusCode(200)
                .body("data.imported", is(2))
                .body("data.byType.recipe", is(2));

        // (3) tipo no reconocido -> resultado con error, sin romper
        given().header(HEADER, user).contentType("application/json").body("{}")
                .when().post("/api/v1/kdl/import/bogus")
                .then().statusCode(200)
                .body("data.imported", is(0))
                .body("data.errors.size()", is(1));
    }

    @Test
    void section_import_loads_a_week_of_calendar_sessions_with_variants() {
        String user = freshUser();
        // Una semana de calendario: un día con series y otro día con dos variantes.
        String planned = "["
                + "{\"date\":\"2026-09-01\",\"type\":\"RUNNING\",\"targetDistanceMeters\":8000,"
                + "\"steps\":[{\"kind\":\"WARMUP\",\"targetDistanceMeters\":2000},"
                + "{\"kind\":\"INTERVAL\",\"repeat\":5,\"targetDistanceMeters\":1000,\"recoverySeconds\":90}]},"
                + "{\"date\":\"2026-09-03\",\"type\":\"RUNNING\",\"variantGroup\":\"g1\",\"variantLabel\":\"Carga normal\",\"targetDistanceMeters\":10000},"
                + "{\"date\":\"2026-09-03\",\"type\":\"RUNNING\",\"variantGroup\":\"g1\",\"variantLabel\":\"Descarga\",\"targetDistanceMeters\":6000}"
                + "]";
        given().header(HEADER, user).contentType("application/json").body(planned)
                .when().post("/api/v1/kdl/import/planned")
                .then().statusCode(200)
                .body("data.imported", is(3))
                .body("data.skipped", is(0))
                .body("data.byType.planned", is(3));

        given().header(HEADER, user).when().get("/api/v1/planned")
                .then().statusCode(200)
                .body("data.size()", is(3))
                .body("data.find { it.date == '2026-09-01' }.steps.size()", is(2));
    }

    @Test
    void section_import_loads_a_periodization_season_as_one_block() {
        String user = freshUser();
        // Un macro con su meso anidado -> un solo item block que crea el árbol.
        String block = "{\"level\":\"MACRO\",\"focus\":\"BASE\",\"name\":\"Temporada\","
                + "\"startDate\":\"2026-09-01\",\"endDate\":\"2026-11-30\","
                + "\"children\":[{\"level\":\"MESO\",\"focus\":\"BUILD\",\"name\":\"Bloque 1\","
                + "\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-28\"}]}";
        given().header(HEADER, user).contentType("application/json").body(block)
                .when().post("/api/v1/kdl/import/block")
                .then().statusCode(200)
                .body("data.imported", is(1))
                .body("data.skipped", is(0))
                .body("data.byType.block", is(1));

        given().header(HEADER, user).when().get("/api/v1/blocks")
                .then().statusCode(200)
                .body("data.size()", is(1))
                .body("data[0].children[0].name", is("Bloque 1"));
    }

    @Test
    void section_import_requires_session() {
        given().contentType("application/json").body("{}")
                .when().post("/api/v1/kdl/import/plan").then().statusCode(401);
    }

    @Test
    void export_wraps_resources_and_filters_by_type() {
        String user = freshUser();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Avena\",\"baseUnit\":\"GRAM\",\"calories\":389}")
                .when().post("/api/v1/ingredients").then().statusCode(200);
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-15\",\"type\":\"RUNNING\",\"distanceMeters\":5000,\"durationSeconds\":1500}")
                .when().post("/api/v1/workouts").then().statusCode(200);

        given().header(HEADER, user).when().get("/api/v1/kdl/export")
                .then().statusCode(200)
                .body("kore", is("kdl"))
                .body("items.findAll { it.koreType == 'ingredient' }.size()", is(1))
                .body("items.findAll { it.koreType == 'workout' }.size()", is(1));

        // filtro por tipo: solo ingredientes
        given().header(HEADER, user).queryParam("types", "ingredient")
                .when().get("/api/v1/kdl/export")
                .then().statusCode(200)
                .body("items.size()", is(1))
                .body("items[0].koreType", is("ingredient"));
    }

    @Test
    void roundtrip_export_then_import_into_another_session() {
        String source = freshUser();
        given().header(HEADER, source).contentType("application/json")
                .body("{\"name\":\"Pollo\",\"baseUnit\":\"GRAM\",\"calories\":165,\"protein\":31}")
                .when().post("/api/v1/ingredients").then().statusCode(200);
        given().header(HEADER, source).contentType("application/json")
                .body("{\"name\":\"Torso\",\"items\":[{\"exerciseName\":\"Press banca\",\"sets\":5,\"reps\":5}]}")
                .when().post("/api/v1/routines").then().statusCode(200);

        String exported = given().header(HEADER, source).when().get("/api/v1/kdl/export")
                .then().statusCode(200).extract().asString();

        String target = freshUser();
        given().header(HEADER, target).contentType("application/json").body(exported)
                .when().post("/api/v1/kdl/import")
                .then().statusCode(200)
                .body("data.skipped", is(0))
                .body("data.byType.ingredient", is(1))
                .body("data.byType.routine", is(1));

        given().header(HEADER, target).when().get("/api/v1/ingredients")
                .then().statusCode(200).body("data.size()", is(1)).body("data[0].name", is("Pollo"));
        given().header(HEADER, target).when().get("/api/v1/routines")
                .then().statusCode(200).body("data.size()", is(1))
                .body("data[0].items[0].exerciseName", is("Press banca"));
    }
}
