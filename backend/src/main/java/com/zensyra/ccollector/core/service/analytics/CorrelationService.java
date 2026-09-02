package com.zensyra.ccollector.core.service.analytics;

import com.zensyra.ccollector.core.domain.activity.DailyActivity;
import com.zensyra.ccollector.core.domain.weight.WeightEntry;
import com.zensyra.ccollector.core.dto.analytics.CorrelationReportDTO;
import com.zensyra.ccollector.core.dto.analytics.CorrelationReportDTO.Correlation;
import com.zensyra.ccollector.core.dto.analytics.CorrelationReportDTO.WeekPoint;
import com.zensyra.ccollector.core.dto.analytics.PeriodSummaryDTO;
import com.zensyra.ccollector.core.dto.energy.EnergyWeekDTO;
import com.zensyra.ccollector.core.repository.activity.DailyActivityRepository;
import com.zensyra.ccollector.core.repository.weight.WeightEntryRepository;
import com.zensyra.ccollector.core.service.energy.EnergyService;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Cruza las secciones semana a semana (entreno × nutrición × actividad × peso)
 * y estima correlaciones (Pearson) entre los pares con sentido, para responder
 * preguntas como "¿cómo influye lo que como en mi ritmo?".
 */
@ApplicationScoped
public class CorrelationService {

    /** Nº mínimo de semanas emparejadas para arriesgar un coeficiente. */
    private static final int MIN_PAIRS = 3;

    private final AnalyticsService analytics;
    private final EnergyService energy;
    private final WeightEntryRepository weights;
    private final DailyActivityRepository dailyActivity;

    public CorrelationService(AnalyticsService analytics, EnergyService energy,
                              WeightEntryRepository weights, DailyActivityRepository dailyActivity) {
        this.analytics = analytics;
        this.energy = energy;
        this.weights = weights;
        this.dailyActivity = dailyActivity;
    }

    public CorrelationReportDTO report(Long userId) {
        List<WeekPoint> series = buildSeries(userId);

        List<Correlation> correlations = List.of(
                correlate(series, "Carga de entreno", WeekPoint::load, "Balance energético", WeekPoint::balanceKcal,
                        "Una relación negativa sugiere que entrenas más en las semanas de mayor déficit."),
                correlate(series, "Calorías consumidas", WeekPoint::consumedKcal, "Carga de entreno", WeekPoint::load,
                        "Positiva = repostas más cuando sube la carga (buena señal de fueling)."),
                correlate(series, "Pasos diarios", steps(), "Carga de entreno", WeekPoint::load,
                        "Cuánto acompaña tu actividad diaria a la carga de entrenamiento."),
                correlate(series, "Balance energético", WeekPoint::balanceKcal, "Peso", WeekPoint::weightKg,
                        "Positiva = las semanas de superávit acompañan subidas de peso."),
                correlate(series, "Carga de entreno", WeekPoint::load, "Ritmo medio", WeekPoint::avgPaceSecondsPerKm,
                        "Ojo: menos s/km = más rápido; negativa = a más carga, ritmos más rápidos."),
                correlate(series, "Calorías consumidas", WeekPoint::consumedKcal, "Ritmo medio", WeekPoint::avgPaceSecondsPerKm,
                        "Relación entre lo que comes y el ritmo (recuerda: menos s/km = más rápido)."));

        String note = "Calculado sobre " + series.size() + " semanas almacenadas. La carga de "
                + "históricos puntuales de Suunto para mirar más atrás sigue pendiente (retención "
                + "mínima: esos datos no se guardan a largo plazo).";
        return new CorrelationReportDTO(Instant.now(), series.size(), series, correlations, note);
    }

    private List<WeekPoint> buildSeries(Long userId) {
        // Base: semanas de rendimiento (carga/km/ritmo) de AnalyticsService.
        List<PeriodSummaryDTO> weekly = analytics.dashboard(userId).weekly();

        // Nutrición por semana (lunes → consumidas/quemadas/balance).
        Map<LocalDate, EnergyWeekDTO> energyByMonday = new HashMap<>();
        for (EnergyWeekDTO w : energy.summary(userId).weeks()) {
            energyByMonday.put(w.from(), w);
        }

        Map<LocalDate, long[]> stepsByMonday = new HashMap<>();   // [sumSteps, hasSteps]
        for (DailyActivity a : dailyActivity.listByUser(userId)) {
            if (a.date == null || a.steps == null) {
                continue;
            }
            long[] acc = stepsByMonday.computeIfAbsent(monday(a.date), k -> new long[2]);
            acc[0] += a.steps;
            acc[1] = 1;
        }

        Map<LocalDate, double[]> weightByMonday = new HashMap<>();  // [sum, count]
        for (WeightEntry e : weights.listByUser(userId)) {
            if (e.date == null || e.weightKg == null) {
                continue;
            }
            double[] acc = weightByMonday.computeIfAbsent(monday(e.date), k -> new double[2]);
            acc[0] += e.weightKg;
            acc[1] += 1;
        }

        List<WeekPoint> series = new ArrayList<>();
        for (PeriodSummaryDTO p : weekly) {
            LocalDate monday = p.from();
            EnergyWeekDTO ew = energyByMonday.get(monday);
            Double consumed = ew != null && ew.consumedKcal() > 0 ? (double) ew.consumedKcal() : null;
            Double burned = ew != null && ew.burnedKcal() > 0 ? (double) ew.burnedKcal() : null;
            // el balance sólo tiene sentido "nutricional" si hay consumo registrado
            Double balance = consumed != null ? (double) ew.balanceKcal() : null;

            long[] st = stepsByMonday.get(monday);
            Long steps = st != null && st[1] == 1 ? st[0] : null;

            double[] wg = weightByMonday.get(monday);
            Double weight = wg != null && wg[1] > 0 ? round(wg[0] / wg[1]) : null;

            Double km = p.distanceMeters() > 0 ? round(p.distanceMeters() / 1000.0) : 0.0;
            Double pace = p.avgPaceSecondsPerKm() != null ? p.avgPaceSecondsPerKm().doubleValue() : null;

            series.add(new WeekPoint(p.label(), monday, round(p.load()), km, pace,
                    consumed, burned, balance, steps, weight));
        }
        return series;
    }

    private static Function<WeekPoint, Double> steps() {
        return p -> p.steps() != null ? p.steps().doubleValue() : null;
    }

    private Correlation correlate(List<WeekPoint> series, String aLabel, Function<WeekPoint, Double> a,
                                  String bLabel, Function<WeekPoint, Double> b, String hint) {
        List<double[]> pairs = new ArrayList<>();
        for (WeekPoint w : series) {
            Double av = a.apply(w);
            Double bv = b.apply(w);
            if (av != null && bv != null) {
                pairs.add(new double[]{av, bv});
            }
        }
        int n = pairs.size();
        Double r = pearson(pairs);
        if (r == null) {
            String reason = n < MIN_PAIRS
                    ? "Datos insuficientes: sólo " + n + " semana(s) con ambas métricas."
                    : "Sin variación suficiente en las " + n + " semanas para estimar la relación.";
            return new Correlation(aLabel, bLabel, null, n, "ninguna", "—", reason);
        }
        String strength = strength(r);
        String direction = r > 0 ? "positiva" : r < 0 ? "negativa" : "—";
        String interpretation = "Correlación " + strength + " " + direction + " (r=" + r
                + ") sobre " + n + " semanas. " + hint;
        return new Correlation(aLabel, bLabel, r, n, strength, direction, interpretation);
    }

    /** Pearson sobre pares [x, y]; null si <3 pares o alguna serie sin varianza. */
    private static Double pearson(List<double[]> pairs) {
        int n = pairs.size();
        if (n < MIN_PAIRS) {
            return null;
        }
        double mx = 0;
        double my = 0;
        for (double[] p : pairs) {
            mx += p[0];
            my += p[1];
        }
        mx /= n;
        my /= n;
        double sxy = 0;
        double sxx = 0;
        double syy = 0;
        for (double[] p : pairs) {
            double dx = p[0] - mx;
            double dy = p[1] - my;
            sxy += dx * dy;
            sxx += dx * dx;
            syy += dy * dy;
        }
        if (sxx <= 0 || syy <= 0) {
            return null;
        }
        return round(sxy / Math.sqrt(sxx * syy));
    }

    private static String strength(double r) {
        double a = Math.abs(r);
        if (a < 0.2) {
            return "ninguna";
        }
        if (a < 0.4) {
            return "débil";
        }
        if (a < 0.6) {
            return "moderada";
        }
        if (a < 0.8) {
            return "fuerte";
        }
        return "muy fuerte";
    }

    private static LocalDate monday(LocalDate d) {
        return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }
}
