package com.zensyra.ccollector.core.resource.plan;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class PlanResourceTest {

    private static final String HEADER = "X-CCollector-Username";

    private String freshUser() {
        String username = "plan-" + UUID.randomUUID();
        given().contentType("application/json")
                .body("{\"username\":\"" + username + "\"}")
                .when().post("/api/v1/auth/login").then().statusCode(200);
        return username;
    }

    private void addWorkout(String user, String date) {
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"" + date + "\",\"type\":\"RUNNING\",\"distanceMeters\":5000,\"durationSeconds\":1500}")
                .when().post("/api/v1/workouts").then().statusCode(200);
    }

    @Test
    void list_requires_session() {
        given().when().get("/api/v1/plans").then().statusCode(401);
    }

    @Test
    void create_computes_adherence_against_workouts() {
        String user = freshUser();
        // one of the two planned sessions has a matching workout
        addWorkout(user, "2026-07-13");

        int id = given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Plan 10k\",\"goal\":\"sub 45\",\"startDate\":\"2026-07-13\",\"endDate\":\"2026-07-19\","
                        + "\"sessions\":["
                        + "{\"date\":\"2026-07-13\",\"type\":\"RUNNING\",\"targetDistanceMeters\":5000},"
                        + "{\"date\":\"2026-07-15\",\"type\":\"RUNNING\",\"targetDistanceMeters\":8000}]}")
                .when().post("/api/v1/plans")
                .then().statusCode(200)
                .body("data.id", notNullValue())
                .body("data.plannedCount", is(2))
                .body("data.completedCount", is(1))
                .body("data.adherencePct", is(50))
                .body("data.sessions[0].done", is(true))
                .body("data.sessions[1].done", is(false))
                .extract().path("data.id");

        given().header(HEADER, user)
                .when().get("/api/v1/plans/" + id)
                .then().statusCode(200)
                .body("data.name", is("Plan 10k"));
    }

    @Test
    void update_replaces_sessions_and_delete_removes_plan() {
        String user = freshUser();
        int id = given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Base\",\"startDate\":\"2026-07-01\","
                        + "\"sessions\":[{\"date\":\"2026-07-02\",\"type\":\"RUNNING\"}]}")
                .when().post("/api/v1/plans").then().statusCode(200).extract().path("data.id");

        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Base v2\",\"startDate\":\"2026-07-01\","
                        + "\"sessions\":["
                        + "{\"date\":\"2026-07-02\",\"type\":\"RUNNING\"},"
                        + "{\"date\":\"2026-07-04\",\"type\":\"STRENGTH\"}]}")
                .when().put("/api/v1/plans/" + id)
                .then().statusCode(200)
                .body("data.name", is("Base v2"))
                .body("data.plannedCount", is(2));

        given().header(HEADER, user).when().delete("/api/v1/plans/" + id).then().statusCode(200);
        given().header(HEADER, user).when().get("/api/v1/plans/" + id).then().statusCode(404);
    }

    @Test
    void create_persists_structured_steps() {
        String user = freshUser();
        // "4 km cal + 200 m ×5 /1'": un paso WARMUP y un paso INTERVAL con repeat/descanso.
        int id = given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Series\",\"startDate\":\"2026-07-13\","
                        + "\"sessions\":[{\"date\":\"2026-07-14\",\"type\":\"RUNNING\",\"steps\":["
                        + "{\"kind\":\"WARMUP\",\"targetDistanceMeters\":4000},"
                        + "{\"kind\":\"INTERVAL\",\"repeat\":5,\"targetDistanceMeters\":200,"
                        + "\"recoverySeconds\":60,\"targetPaceMinSecPerKm\":220,\"targetPaceMaxSecPerKm\":230}]}]}")
                .when().post("/api/v1/plans")
                .then().statusCode(200)
                .body("data.sessions[0].steps.size()", is(2))
                .body("data.sessions[0].steps[0].kind", is("WARMUP"))
                .body("data.sessions[0].steps[0].orderIndex", is(0))
                .body("data.sessions[0].steps[1].kind", is("INTERVAL"))
                .body("data.sessions[0].steps[1].repeat", is(5))
                .body("data.sessions[0].steps[1].recoverySeconds", is(60))
                .body("data.sessions[0].steps[1].targetPaceMinSecPerKm", is(220))
                .extract().path("data.id");

        // los pasos sobreviven a la lectura y al export portable
        given().header(HEADER, user).when().get("/api/v1/plans/" + id)
                .then().statusCode(200)
                .body("data.sessions[0].steps.size()", is(2));
        given().header(HEADER, user).when().get("/api/v1/session/export")
                .then().statusCode(200)
                .body("plans[0].sessions[0].steps[1].repeat", is(5));
    }

    @Test
    void rejects_plan_without_name() {
        String user = freshUser();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"startDate\":\"2026-07-01\"}")
                .when().post("/api/v1/plans")
                .then().statusCode(400).body("success", is(false));
    }

    @Test
    void plans_are_isolated_and_roundtrip_through_session_export() {
        String user = freshUser();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Exportable\",\"startDate\":\"2026-07-10\","
                        + "\"sessions\":[{\"date\":\"2026-07-11\",\"type\":\"RUNNING\",\"targetDistanceMeters\":6000}]}")
                .when().post("/api/v1/plans").then().statusCode(200);

        // export carries the plan
        given().header(HEADER, user)
                .when().get("/api/v1/session/export")
                .then().statusCode(200)
                .body("plans.size()", is(1))
                .body("plans[0].name", is("Exportable"))
                .body("plans[0].sessions[0].targetDistanceMeters", is(6000.0f));

        // import into a fresh session recreates the plan
        String imported = "planimp-" + UUID.randomUUID();
        String doc = "{\"schemaVersion\":2,\"user\":{\"username\":\"" + imported + "\"},"
                + "\"workouts\":[],\"plans\":[{\"name\":\"Imported\",\"startDate\":\"2026-07-10\","
                + "\"sessions\":[{\"date\":\"2026-07-11\",\"type\":\"RUNNING\"}]}]}";
        given().contentType("application/json").body(doc)
                .when().post("/api/v1/session/import").then().statusCode(200);
        given().header(HEADER, imported)
                .when().get("/api/v1/plans")
                .then().statusCode(200)
                .body("data.size()", is(1))
                .body("data[0].name", is("Imported"));
    }
}
