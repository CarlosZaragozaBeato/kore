package com.zensyra.ccollector.core.resource.template;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class SessionTemplateResourceTest {

    private static final String HEADER = "X-CCollector-Username";

    private String freshUser() {
        String username = "tpl-" + UUID.randomUUID();
        given().contentType("application/json")
                .body("{\"username\":\"" + username + "\"}")
                .when().post("/api/v1/auth/login").then().statusCode(200);
        return username;
    }

    @Test
    void requires_session() {
        given().when().get("/api/v1/session-templates").then().statusCode(401);
    }

    @Test
    void template_crud() {
        String user = freshUser();
        int id = given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Series 5×1000\",\"discipline\":\"RUNNING\",\"goal\":\"TEN_K\","
                        + "\"level\":\"ADVANCED\",\"targetDistanceMeters\":11000,"
                        + "\"structure\":\"5×1000 a ritmo 10K\"}")
                .when().post("/api/v1/session-templates")
                .then().statusCode(200)
                .body("data.name", is("Series 5×1000"))
                .body("data.discipline", is("RUNNING"))
                .body("data.goal", is("TEN_K"))
                .body("data.level", is("ADVANCED"))
                .extract().path("data.id");

        given().header(HEADER, user).when().get("/api/v1/session-templates")
                .then().statusCode(200).body("data.size()", is(1));

        given().header(HEADER, user).when().delete("/api/v1/session-templates/" + id).then().statusCode(200);
        given().header(HEADER, user).when().get("/api/v1/session-templates")
                .then().statusCode(200).body("data.size()", is(0));
    }

    @Test
    void rejects_blank_name() {
        String user = freshUser();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"\",\"discipline\":\"RUNNING\"}")
                .when().post("/api/v1/session-templates")
                .then().statusCode(400).body("success", is(false));
    }

    @Test
    void seed_loads_examples_and_is_idempotent() {
        String user = freshUser();
        given().header(HEADER, user).when().post("/api/v1/session-templates/seed")
                .then().statusCode(200)
                .body("data.added", is(14))
                .body("data.skipped", is(0));
        given().header(HEADER, user).when().post("/api/v1/session-templates/seed")
                .then().statusCode(200)
                .body("data.added", is(0))
                .body("data.skipped", is(14));
    }

    @Test
    void templates_roundtrip_through_session_export() {
        String user = freshUser();
        given().header(HEADER, user).contentType("application/json")
                .body("{\"name\":\"Tirada larga 30 km\",\"discipline\":\"RUNNING\",\"goal\":\"MARATHON\",\"level\":\"ADVANCED\"}")
                .when().post("/api/v1/session-templates").then().statusCode(200);

        given().header(HEADER, user).when().get("/api/v1/session/export")
                .then().statusCode(200)
                .body("schemaVersion", is(15))
                .body("sessionTemplates.size()", is(1))
                .body("sessionTemplates[0].goal", is("MARATHON"));

        String imported = "tplimp-" + UUID.randomUUID();
        String doc = "{\"schemaVersion\":8,\"user\":{\"username\":\"" + imported + "\"},"
                + "\"sessionTemplates\":[{\"name\":\"Series 6×400\",\"discipline\":\"RUNNING\",\"goal\":\"FIVE_K\",\"level\":\"INTERMEDIATE\"}]}";
        given().contentType("application/json").body(doc)
                .when().post("/api/v1/session/import").then().statusCode(200);

        given().header(HEADER, imported).when().get("/api/v1/session-templates")
                .then().statusCode(200).body("data.size()", is(1))
                .body("data[0].name", is("Series 6×400"));
    }
}
