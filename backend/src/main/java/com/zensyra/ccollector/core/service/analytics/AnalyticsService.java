package com.zensyra.ccollector.core.service.analytics;

import com.zensyra.ccollector.core.domain.workout.Workout;
import com.zensyra.ccollector.core.dto.analytics.DashboardDTO;
import com.zensyra.ccollector.core.dto.analytics.PeriodSummaryDTO;
import com.zensyra.ccollector.core.dto.analytics.TotalsDTO;
import com.zensyra.ccollector.core.repository.workout.WorkoutRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class AnalyticsService {

    private static final int WEEKS = 12;
    private static final int MONTHS = 6;

    private final WorkoutRepository workouts;

    public AnalyticsService(WorkoutRepository workouts) {
        this.workouts = workouts;
    }

    public DashboardDTO dashboard(Long userId) {
        List<Workout> all = workouts.listByUser(userId);
        return new DashboardDTO(
                Instant.now(),
                totals(all),
                weekly(all, LocalDate.now()),
                monthly(all, LocalDate.now()));
    }

    private TotalsDTO totals(List<Workout> all) {
        Acc acc = new Acc();
        all.forEach(acc::add);
        return new TotalsDTO(acc.count, acc.distance, acc.duration, acc.avgPace(), round(acc.load));
    }

    private List<PeriodSummaryDTO> weekly(List<Workout> all, LocalDate today) {
        LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Map<LocalDate, Acc> buckets = new LinkedHashMap<>();
        for (int i = WEEKS - 1; i >= 0; i--) {
            buckets.put(thisMonday.minusWeeks(i), new Acc());
        }
        for (Workout w : all) {
            LocalDate monday = w.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            Acc acc = buckets.get(monday);
            if (acc != null) {
                acc.add(w);
            }
        }
        List<PeriodSummaryDTO> out = new ArrayList<>();
        buckets.forEach((monday, acc) -> {
            String label = monday.get(IsoFields.WEEK_BASED_YEAR) + "-W"
                    + String.format("%02d", monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            out.add(acc.toSummary(label, monday, monday.plusDays(6)));
        });
        return out;
    }

    private List<PeriodSummaryDTO> monthly(List<Workout> all, LocalDate today) {
        YearMonth current = YearMonth.from(today);
        Map<YearMonth, Acc> buckets = new LinkedHashMap<>();
        for (int i = MONTHS - 1; i >= 0; i--) {
            buckets.put(current.minusMonths(i), new Acc());
        }
        for (Workout w : all) {
            Acc acc = buckets.get(YearMonth.from(w.date));
            if (acc != null) {
                acc.add(w);
            }
        }
        List<PeriodSummaryDTO> out = new ArrayList<>();
        buckets.forEach((ym, acc) -> {
            String label = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            out.add(acc.toSummary(label, ym.atDay(1), ym.atEndOfMonth()));
        });
        return out;
    }

    private static long round(double v) {
        return Math.round(v);
    }

    /** Acumulador mutable de un bucket. */
    private static final class Acc {
        int count;
        double distance;
        long duration;
        double load;

        void add(Workout w) {
            count++;
            if (w.distanceMeters != null) {
                distance += w.distanceMeters;
            }
            if (w.durationSeconds != null) {
                duration += w.durationSeconds;
            }
            load += TrainingLoad.of(w);
        }

        Long avgPace() {
            if (distance <= 0 || duration <= 0) {
                return null;
            }
            return Math.round(duration / (distance / 1000.0));
        }

        PeriodSummaryDTO toSummary(String label, LocalDate from, LocalDate to) {
            return new PeriodSummaryDTO(label, from, to, count, distance, duration, avgPace(), round(load));
        }
    }
}
