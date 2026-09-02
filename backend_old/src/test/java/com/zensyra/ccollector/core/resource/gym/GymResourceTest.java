package com.zensyra.ccollector.core.resource.gym;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class GymResourceTest {

    private static final String HEADER = "X-CCollector-Username";

    private String freshUser() {
        String username = "gym-" + UUID.randomUUID();
        given().contentType("application/json")
                .body("{\"username\":\"" + username + "\"}")
                .when().post("/api/v1/auth/login").then().statusCode(200);
        return username;
    }

    @Test
    void endpoints_require_session() {
        given().when().get("/api/v1/exercises").then().statusCode(401);
        given().when().get("/api/v1/routines").then().statusCode(401);
        given().when().get("/api/v1/strength-sessions").then().statusCode(401);
    }

    @Test
    void exercise_crud() {
        String user = freshUser();
        int id = given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Sentadilla\",\"muscleGroup\":\"Piernas\",\"equipment\":\"Peso corporal\","
                        + "\"imageUrl\":\"https://x/sentadilla.gif\","
                        + "\"instructions\":\"Baja con la espalda neutra y sube empujando el suelo.\","
                        + "\"metValue\":5.0}")
                .when().post("/api/v1/exercises")
                .then().statusCode(200)
                .body("data.name", is("Sentadilla"))
                .body("data.muscleGroup", is("Piernas"))
                .body("data.imageUrl", is("https://x/sentadilla.gif"))
                .body("data.instructions", is("Baja con la espalda neutra y sube empujando el suelo."))
                .body("data.metValue", is(5.0f))
                .extract().path("data.id");

        given().header(HEADER, user).when().get("/api/v1/exercises")
                .then().statusCode(200).body("data.size()", is(1));

        given().header(HEADER, user).when().delete("/api/v1/exercises/" + id).then().statusCode(200);
        given().header(HEADER, user).when().get("/api/v1/exercises")
                .then().statusCode(200).body("data.size()", is(0));
    }

    @Test
    void exercise_seed_loads_examples_and_is_idempotent() {
        String user = freshUser();
        given().header(HEADER, user).when().post("/api/v1/exercises/seed")
                .then().statusCode(200)
                .body("data.added", is(21))
                .body("data.skipped", is(0));
        // segunda siembra no duplica
        given().header(HEADER, user).when().post("/api/v1/exercises/seed")
                .then().statusCode(200)
                .body("data.added", is(0))
                .body("data.skipped", is(21));
    }

    @Test
    void routine_with_items_and_update_replaces() {
        String user = freshUser();
        int id = given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Full body\",\"description\":\"Casa\",\"items\":["
                        + "{\"exerciseName\":\"Flexiones\",\"sets\":4,\"reps\":12,\"restSeconds\":60},"
                        + "{\"exerciseName\":\"Sentadillas\",\"sets\":4,\"reps\":15}]}")
                .when().post("/api/v1/routines")
                .then().statusCode(200)
                .body("data.id", notNullValue())
                .body("data.items.size()", is(2))
                .body("data.items[0].exerciseName", is("Flexiones"))
                .body("data.items[0].sets", is(4))
                .extract().path("data.id");

        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Full body v2\",\"items\":[{\"exerciseName\":\"Dominadas\",\"sets\":3,\"reps\":8}]}")
                .when().put("/api/v1/routines/" + id)
                .then().statusCode(200)
                .body("data.name", is("Full body v2"))
                .body("data.items.size()", is(1))
                .body("data.items[0].exerciseName", is("Dominadas"));

        given().header(HEADER, user).when().delete("/api/v1/routines/" + id).then().statusCode(200);
        given().header(HEADER, user).when().get("/api/v1/routines/" + id).then().statusCode(404);
    }

    @Test
    void strength_session_links_routine_snapshot() {
        String user = freshUser();
        int routineId = given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Empuje\",\"items\":[{\"exerciseName\":\"Fondos\"}]}")
                .when().post("/api/v1/routines").then().statusCode(200).extract().path("data.id");

        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-20\",\"routineId\":" + routineId + ",\"notes\":\"buen día\"}")
                .when().post("/api/v1/strength-sessions")
                .then().statusCode(200)
                .body("data.routineName", is("Empuje"))
                .body("data.notes", is("buen día"));

        given().header(HEADER, user).when().get("/api/v1/strength-sessions")
                .then().statusCode(200).body("data.size()", is(1));
    }

    @Test
    void strength_session_can_be_planned_for_a_future_day() {
        String user = freshUser();
        // sin estado explícito, se registra como realizada (DONE)
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-20\",\"notes\":\"hecho\"}")
                .when().post("/api/v1/strength-sessions")
                .then().statusCode(200)
                .body("data.status", is("DONE"));

        // planificar gimnasio para un día concreto del calendario
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-08-01\",\"status\":\"PLANNED\",\"notes\":\"piernas\"}")
                .when().post("/api/v1/strength-sessions")
                .then().statusCode(200)
                .body("data.status", is("PLANNED"));

        given().header(HEADER, user).when().get("/api/v1/strength-sessions")
                .then().statusCode(200).body("data.size()", is(2))
                .body("data.findAll { it.status == 'PLANNED' }.size()", is(1));
    }

    @Test
    void strength_session_rejects_foreign_routine() {
        String user = freshUser();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-20\",\"routineId\":999999}")
                .when().post("/api/v1/strength-sessions")
                .then().statusCode(400).body("success", is(false));
    }

    @Test
    void gym_roundtrips_through_session_export() {
        String user = freshUser();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Press banca\",\"muscleGroup\":\"Pecho\"}")
                .when().post("/api/v1/exercises").then().statusCode(200);
        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Torso\",\"items\":[{\"exerciseName\":\"Press banca\",\"sets\":5,\"reps\":5}]}")
                .when().post("/api/v1/routines").then().statusCode(200);

        given().header(HEADER, user).when().get("/api/v1/session/export")
                .then().statusCode(200)
                .body("schemaVersion", is(15))
                .body("exercises.size()", is(1))
                .body("routines.size()", is(1))
                .body("routines[0].items[0].exerciseName", is("Press banca"));

        String imported = "gymimp-" + UUID.randomUUID();
        String doc = "{\"schemaVersion\":3,\"user\":{\"username\":\"" + imported + "\"},"
                + "\"workouts\":[],\"plans\":[],"
                + "\"exercises\":[{\"name\":\"Remo\",\"muscleGroup\":\"Espalda\"}],"
                + "\"routines\":[{\"name\":\"Tira\",\"items\":[{\"exerciseName\":\"Remo\",\"sets\":4,\"reps\":10}]}],"
                + "\"strengthSessions\":[{\"date\":\"2026-07-19\",\"routineName\":\"Tira\",\"notes\":\"ok\"}]}";
        given().contentType("application/json").body(doc)
                .when().post("/api/v1/session/import").then().statusCode(200);

        given().header(HEADER, imported).when().get("/api/v1/routines")
                .then().statusCode(200)
                .body("data.size()", is(1))
                .body("data[0].items[0].exerciseName", is("Remo"));
        // documento v3 (sin estado) → la sesión se asume realizada (DONE)
        given().header(HEADER, imported).when().get("/api/v1/strength-sessions")
                .then().statusCode(200).body("data.size()", is(1))
                .body("data[0].routineName", is("Tira"))
                .body("data[0].status", is("DONE"));
    }
}
