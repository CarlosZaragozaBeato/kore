package com.zensyra.ccollector.core.resource.plan;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

@QuarkusTest
class PlannedSessionResourceTest {

    private static final String HEADER = "X-CCollector-Username";

    private String freshUser() {
        String username = "planned-" + UUID.randomUUID();
        given().contentType("application/json")
                .body("{\"username\":\"" + username + "\"}")
                .when().post("/api/v1/auth/login").then().statusCode(200);
        return username;
    }

    @Test
    void list_requires_session() {
        given().when().get("/api/v1/planned").then().statusCode(401);
    }

    @Test
    void create_standalone_session_defaults_to_proposed_and_carries_steps() {
        String user = freshUser();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-21\",\"type\":\"RUNNING\","
                        + "\"steps\":[{\"kind\":\"WARMUP\",\"targetDistanceMeters\":4000},"
                        + "{\"kind\":\"INTERVAL\",\"repeat\":5,\"targetDistanceMeters\":200,\"recoverySeconds\":60}]}")
                .when().post("/api/v1/planned")
                .then().statusCode(200)
                .body("data.id", notNullValue())
                .body("data.planId", nullValue())
                .body("data.status", is("PROPOSED"))
                .body("data.steps.size()", is(2));

        given().header(HEADER, user).when().get("/api/v1/planned")
                .then().statusCode(200).body("data.size()", is(1));
    }

    @Test
    void accepting_a_variant_rejects_its_siblings() {
        String user = freshUser();
        String group = "grp-" + UUID.randomUUID();

        int a = given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-22\",\"type\":\"RUNNING\",\"status\":\"PROPOSED\","
                        + "\"variantGroup\":\"" + group + "\",\"variantLabel\":\"Carga normal\"}")
                .when().post("/api/v1/planned").then().statusCode(200).extract().path("data.id");
        int b = given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-22\",\"type\":\"RUNNING\",\"status\":\"PROPOSED\","
                        + "\"variantGroup\":\"" + group + "\",\"variantLabel\":\"Descarga\"}")
                .when().post("/api/v1/planned").then().statusCode(200).extract().path("data.id");

        given().header(HEADER, user).when().post("/api/v1/planned/" + a + "/accept")
                .then().statusCode(200).body("data.status", is("ACCEPTED"));

        // la variante hermana queda descartada
        given().header(HEADER, user).when().get("/api/v1/planned").then().statusCode(200)
                .body("data.find { it.id == " + b + " }.status", is("REJECTED"));
    }

    @Test
    void comparison_matches_the_days_workout_and_bands_the_metrics() {
        String user = freshUser();
        // Planificado: 5 km con ritmo objetivo 4:00–4:20/km.
        int id = given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-24\",\"type\":\"RUNNING\",\"status\":\"ACCEPTED\","
                        + "\"targetDistanceMeters\":5000,"
                        + "\"steps\":[{\"kind\":\"STEADY\",\"targetPaceMinSecPerKm\":240,"
                        + "\"targetPaceMaxSecPerKm\":260}]}")
                .when().post("/api/v1/planned").then().statusCode(200).extract().path("data.id");

        // Realizado: 5 km en 1300 s → ritmo 4:20/km (dentro de banda), FC 150.
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-24\",\"type\":\"RUNNING\",\"distanceMeters\":5000,"
                        + "\"durationSeconds\":1300,\"avgHeartRate\":150}")
                .when().post("/api/v1/workouts").then().statusCode(200);

        given().header(HEADER, user).when().get("/api/v1/planned/" + id + "/comparison")
                .then().statusCode(200)
                .body("data.matched", is(true))
                .body("data.workoutId", notNullValue())
                .body("data.actual.paceSecondsPerKm", is(260))
                .body("data.metrics.find { it.key == 'distance' }.band", is("WITHIN"))
                .body("data.metrics.find { it.key == 'pace' }.band", is("WITHIN"));
    }

    @Test
    void comparison_reports_no_match_when_the_day_has_no_workout() {
        String user = freshUser();
        int id = given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-25\",\"type\":\"RUNNING\",\"targetDistanceMeters\":8000}")
                .when().post("/api/v1/planned").then().statusCode(200).extract().path("data.id");

        given().header(HEADER, user).when().get("/api/v1/planned/" + id + "/comparison")
                .then().statusCode(200)
                .body("data.matched", is(false))
                .body("data.actual", nullValue())
                .body("data.metrics.find { it.key == 'distance' }.band", is("NO_DATA"));
    }

    @Test
    void recommendation_defaults_to_maintain_without_load_data() {
        String user = freshUser();
        given().header(HEADER, user).when().get("/api/v1/planned/recommendation")
                .then().statusCode(200)
                .body("data.stance", is("MAINTAIN"))
                .body("data.label", notNullValue())
                .body("data.reason", notNullValue());
    }

    @Test
    void variants_generate_deload_and_build_siblings() {
        String user = freshUser();
        int id = given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-08-11\",\"type\":\"RUNNING\",\"targetDistanceMeters\":10000,"
                        + "\"steps\":[{\"kind\":\"INTERVAL\",\"repeat\":5,\"targetDistanceMeters\":1000}]}")
                .when().post("/api/v1/planned").then().statusCode(200).extract().path("data.id");

        given().header(HEADER, user).when().post("/api/v1/planned/" + id + "/variants")
                .then().statusCode(200)
                .body("data.recommendation.stance", notNullValue())
                .body("data.variants.size()", is(3))
                .body("data.variants.variantLabel", hasItems("Carga normal", "Descarga", "Subida de carga"))
                // descarga = -30 % de volumen sobre la base
                .body("data.variants.find { it.variantLabel == 'Descarga' }.targetDistanceMeters", is(7000.0f))
                .body("data.variants.find { it.variantLabel == 'Subida de carga' }.targetDistanceMeters", is(11000.0f));

        // Quedan en el calendario como propuestas del mismo grupo.
        given().header(HEADER, user).when().get("/api/v1/planned")
                .then().statusCode(200).body("data.size()", is(3));
    }

    @Test
    void standalone_sessions_roundtrip_through_session_export() {
        String user = freshUser();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"2026-07-23\",\"type\":\"RUNNING\",\"status\":\"ACCEPTED\","
                        + "\"steps\":[{\"kind\":\"INTERVAL\",\"repeat\":3,\"targetDistanceMeters\":400}]}")
                .when().post("/api/v1/planned").then().statusCode(200);

        given().header(HEADER, user).when().get("/api/v1/session/export")
                .then().statusCode(200)
                .body("calendarSessions.size()", is(1))
                .body("calendarSessions[0].status", is("ACCEPTED"))
                .body("calendarSessions[0].steps[0].repeat", is(3));
    }
}
