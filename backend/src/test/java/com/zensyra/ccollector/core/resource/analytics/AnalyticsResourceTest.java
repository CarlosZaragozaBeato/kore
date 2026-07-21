package com.zensyra.ccollector.core.resource.analytics;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class AnalyticsResourceTest {

    private static final String HEADER = "X-CCollector-Username";

    private String freshUser() {
        String username = "an-" + UUID.randomUUID();
        given().contentType("application/json")
                .body("{\"username\":\"" + username + "\"}")
                .when().post("/api/v1/auth/login").then().statusCode(200);
        return username;
    }

    private void addWorkout(String user, String date, int distanceM, int durationS, Integer rpe) {
        String rpeField = rpe == null ? "" : ",\"perceivedEffort\":" + rpe;
        given().header(HEADER, user).contentType("application/json")
                .body("{\"date\":\"" + date + "\",\"type\":\"RUNNING\",\"distanceMeters\":" + distanceM
                        + ",\"durationSeconds\":" + durationS + rpeField + "}")
                .when().post("/api/v1/workouts").then().statusCode(200);
    }

    @Test
    void summary_requires_session() {
        given().when().get("/api/v1/analytics/summary").then().statusCode(401);
    }

    @Test
    void totals_and_load_aggregate() {
        String user = freshUser();
        // this week's Monday, so it lands in the last weekly bucket
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        // 10 km in 50:00 (3000 s) at RPE 6 -> load = 50 * 6 = 300; pace 300 s/km
        addWorkout(user, monday.toString(), 10000, 3000, 6);
        // 5 km in 25:00 (1500 s), no RPE -> estimated RPE 5 -> load = 25 * 5 = 125
        addWorkout(user, monday.toString(), 5000, 1500, null);

        given().header(HEADER, user)
                .when().get("/api/v1/analytics/summary")
                .then().statusCode(200)
                .body("totals.workouts", is(2))
                .body("totals.distanceMeters", is(15000.0f))
                .body("totals.durationSeconds", is(4500))
                // 4500 s / 15 km = 300 s/km
                .body("totals.avgPaceSecondsPerKm", is(300))
                // 300 + 125 = 425
                .body("totals.load", is(425.0f))
                .body("weekly.size()", is(12))
                // last weekly bucket holds both workouts
                .body("weekly[11].workouts", is(2))
                .body("weekly[11].load", is(425.0f))
                .body("monthly.size()", is(6));
    }
}
