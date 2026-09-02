package com.zensyra.ccollector.core.service.analytics;

import com.zensyra.ccollector.core.domain.workout.Workout;
import com.zensyra.ccollector.core.dto.analytics.BreakdownDTO;
import com.zensyra.ccollector.core.dto.analytics.DailyDeltaDTO;
import com.zensyra.ccollector.core.dto.analytics.DaySummaryDTO;
import com.zensyra.ccollector.core.dto.analytics.LoadComparisonDTO;
import com.zensyra.ccollector.core.dto.analytics.LoadSignalsDTO;
import com.zensyra.ccollector.core.dto.analytics.RangeStatsDTO;
import com.zensyra.ccollector.core.repository.workout.WorkoutRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Análisis comparativo de carga: dos rangos (por defecto semana actual vs
 * previa) con desglose por disciplina/origen, diferencias diarias y acumuladas,
 * y señales de sobrecarga (ACWR, monotony, ramp). Reutiliza {@link TrainingLoad}.
 */
@ApplicationScoped
public class LoadComparisonService {

    private final WorkoutRepository workouts;

    public LoadComparisonService(WorkoutRepository workouts) {
        this.workouts = workouts;
    }

    public LoadComparisonDTO compare(Long userId, LocalDate curStart, LocalDate curEnd,
                                     LocalDate prevStart, LocalDate prevEnd) {
        LocalDate today = LocalDate.now();
        if (curStart == null || curEnd == null) {
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            curStart = monday;
            curEnd = monday.plusDays(6);
        }
        if (curEnd.isBefore(curStart)) {
            throw new BadRequestException("El fin del rango actual no puede ser anterior al inicio");
        }
        if (prevStart == null || prevEnd == null) {
            long len = ChronoUnit.DAYS.between(curStart, curEnd); // días inclusive - 1
            prevEnd = curStart.minusDays(1);
            prevStart = prevEnd.minusDays(len);
        }
        if (prevEnd.isBefore(prevStart)) {
            throw new BadRequestException("El fin del rango previo no puede ser anterior al inicio");
        }

        List<Workout> all = workouts.listByUser(userId);
        RangeStatsDTO current = rangeStats("actual", curStart, curEnd, all);
        RangeStatsDTO previous = rangeStats("previo", prevStart, prevEnd, all);
        return new LoadComparisonDTO(
                Instant.now(), current, previous,
                dailyDeltas(current, previous),
                signals(all, today));
    }

    private RangeStatsDTO rangeStats(String label, LocalDate from, LocalDate to, List<Workout> all) {
        List<Workout> in = all.stream()
                .filter(w -> !w.date.isBefore(from) && !w.date.isAfter(to))
                .toList();

        List<DaySummaryDTO> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            final LocalDate day = d;
            Acc acc = new Acc();
            in.stream().filter(w -> w.date.equals(day)).forEach(acc::add);
            days.add(new DaySummaryDTO(day, acc.distance, acc.duration, Math.round(acc.load)));
        }

        Acc total = new Acc();
        in.forEach(total::add);

        return new RangeStatsDTO(label, from, to, in.size(), total.distance, total.duration,
                Math.round(total.load), days,
                breakdown(in, w -> w.type.name()),
                breakdown(in, w -> w.source.name()));
    }

    private List<BreakdownDTO> breakdown(List<Workout> in, java.util.function.Function<Workout, String> key) {
        Map<String, Acc> buckets = new LinkedHashMap<>();
        for (Workout w : in) {
            buckets.computeIfAbsent(key.apply(w), k -> new Acc()).add(w);
        }
        List<BreakdownDTO> out = new ArrayList<>();
        buckets.forEach((k, acc) -> out.add(
                new BreakdownDTO(k, acc.count, acc.distance, acc.duration, Math.round(acc.load))));
        return out;
    }

    private List<DailyDeltaDTO> dailyDeltas(RangeStatsDTO current, RangeStatsDTO previous) {
        int n = Math.min(current.days().size(), previous.days().size());
        List<DailyDeltaDTO> out = new ArrayList<>();
        long cumulative = 0;
        for (int i = 0; i < n; i++) {
            DaySummaryDTO c = current.days().get(i);
            DaySummaryDTO p = previous.days().get(i);
            long delta = c.load() - p.load();
            cumulative += delta;
            out.add(new DailyDeltaDTO(i, c.date(), p.date(), c.load(), p.load(), delta, cumulative));
        }
        return out;
    }

    /** Señales de carga actuales del usuario (para recomendaciones de plan). */
    public LoadSignalsDTO signals(Long userId) {
        return signals(workouts.listByUser(userId), LocalDate.now());
    }

    private LoadSignalsDTO signals(List<Workout> all, LocalDate today) {
        long acute = Math.round(sumLoad(all, today.minusDays(6), today));
        double chronicWeekly = sumLoad(all, today.minusDays(27), today) / 4.0;
        Double acwr = chronicWeekly > 0 ? round2(acute / chronicWeekly) : null;

        // Monotony (Foster) sobre la semana natural actual.
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        double[] daily = new double[7];
        for (int i = 0; i < 7; i++) {
            daily[i] = sumLoad(all, monday.plusDays(i), monday.plusDays(i));
        }
        double mean = mean(daily);
        double sd = sd(daily, mean);
        Double monotony = mean == 0 ? null : (sd == 0 ? null : round2(mean / sd));
        String monotonyState = mean == 0 ? "UNKNOWN" : (sd == 0 ? "RISK" : rangeState(monotony, 1.5, 2.0));

        // Ramp: carga semana actual vs semana previa.
        double curWeek = sumLoad(all, monday, monday.plusDays(6));
        double prevWeek = sumLoad(all, monday.minusWeeks(1), monday.minusDays(1));
        Double rampPct = prevWeek > 0 ? round2((curWeek - prevWeek) / prevWeek * 100) : null;

        return new LoadSignalsDTO(
                acute, Math.round(chronicWeekly), acwr, acwrState(acwr),
                monotony, monotonyState, rampPct, rampState(rampPct));
    }

    private double sumLoad(List<Workout> all, LocalDate from, LocalDate to) {
        return all.stream()
                .filter(w -> !w.date.isBefore(from) && !w.date.isAfter(to))
                .mapToDouble(TrainingLoad::of)
                .sum();
    }

    private static String acwrState(Double acwr) {
        if (acwr == null) {
            return "UNKNOWN";
        }
        if (acwr > 1.5) {
            return "RISK";
        }
        if (acwr < 0.8 || acwr > 1.3) {
            return "WARN";
        }
        return "OK";
    }

    /** OK por debajo de {@code warn}, WARN hasta {@code risk}, RISK por encima. */
    private static String rangeState(Double value, double warn, double risk) {
        if (value == null) {
            return "UNKNOWN";
        }
        if (value > risk) {
            return "RISK";
        }
        if (value >= warn) {
            return "WARN";
        }
        return "OK";
    }

    private static String rampState(Double rampPct) {
        if (rampPct == null) {
            return "UNKNOWN";
        }
        if (rampPct > 50) {
            return "RISK";
        }
        if (rampPct > 30) {
            return "WARN";
        }
        return "OK";
    }

    private static double mean(double[] xs) {
        double s = 0;
        for (double x : xs) {
            s += x;
        }
        return s / xs.length;
    }

    private static double sd(double[] xs, double mean) {
        double s = 0;
        for (double x : xs) {
            s += (x - mean) * (x - mean);
        }
        return Math.sqrt(s / xs.length);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
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
    }
}
