package com.zensyra.ccollector.core.service.nutrition;

import com.zensyra.ccollector.core.domain.nutrition.Recipe;
import com.zensyra.ccollector.core.domain.workout.Workout;
import com.zensyra.ccollector.core.dto.nutrition.RecipeRecommendationDTO;
import com.zensyra.ccollector.core.dto.nutrition.RecipeRecommendationDTO.Context;
import com.zensyra.ccollector.core.dto.nutrition.RecipeRecommendationDTO.Ranked;
import com.zensyra.ccollector.core.repository.activity.DailyActivityRepository;
import com.zensyra.ccollector.core.repository.nutrition.RecipeRepository;
import com.zensyra.ccollector.core.repository.weight.WeightGoalRepository;
import com.zensyra.ccollector.core.repository.workout.WorkoutRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Sugiere recetas en función del contexto reciente del atleta: cuánto ha
 * entrenado (últimos 7 días), la actividad diaria registrada y el objetivo de
 * peso. La lógica es determinista (sin modelo): más carga → repostaje de
 * carbohidratos; descanso → foco en proteína para recuperar; en medio,
 * equilibrado. Luego ordena las recetas del usuario por encaje con ese foco.
 */
@ApplicationScoped
public class RecipeRecommendationService {

    private static final int WINDOW_DAYS = 7;
    private static final double HIGH_KCAL = 3500;
    private static final int HIGH_SESSIONS = 5;
    private static final int MAX_RESULTS = 5;

    private final WorkoutRepository workouts;
    private final RecipeRepository recipes;
    private final WeightGoalRepository weightGoals;
    private final DailyActivityRepository dailyActivity;
    private final Clock clock;

    @Inject
    public RecipeRecommendationService(WorkoutRepository workouts, RecipeRepository recipes,
                                       WeightGoalRepository weightGoals,
                                       DailyActivityRepository dailyActivity) {
        this(workouts, recipes, weightGoals, dailyActivity, Clock.systemDefaultZone());
    }

    RecipeRecommendationService(WorkoutRepository workouts, RecipeRepository recipes,
                                WeightGoalRepository weightGoals,
                                DailyActivityRepository dailyActivity, Clock clock) {
        this.workouts = workouts;
        this.recipes = recipes;
        this.weightGoals = weightGoals;
        this.dailyActivity = dailyActivity;
        this.clock = clock;
    }

    public RecipeRecommendationDTO recommend(Long userId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(WINDOW_DAYS - 1L);

        List<Workout> recent = workouts.listByUser(userId).stream()
                .filter(w -> w.date != null && !w.date.isBefore(from) && !w.date.isAfter(today))
                .toList();
        int sessions = recent.size();
        double trainingKcal = recent.stream()
                .filter(w -> w.energyKcal != null)
                .mapToDouble(w -> w.energyKcal)
                .sum();

        var activityBurns = dailyActivity.listByUser(userId).stream()
                .filter(a -> a.date != null && !a.date.isBefore(from) && !a.date.isAfter(today))
                .filter(a -> a.burnedKcal != null)
                .mapToDouble(a -> a.burnedKcal)
                .toArray();
        Double activityKcal = activityBurns.length == 0
                ? null
                : java.util.Arrays.stream(activityBurns).sum();

        String intensity;
        String focus;
        String rationale;
        double calorieFactor;
        if (sessions == 0) {
            intensity = "REST";
            focus = "PROTEIN";
            calorieFactor = 0.95;
            rationale = "Sin entrenos en los últimos 7 días: prioriza proteína para "
                    + "recuperar y mantén las calorías algo por debajo del mantenimiento.";
        } else if (sessions >= HIGH_SESSIONS || trainingKcal >= HIGH_KCAL) {
            intensity = "HIGH";
            focus = "CARB";
            calorieFactor = 1.15;
            rationale = "Carga alta (" + sessions + " entrenos / " + Math.round(trainingKcal)
                    + " kcal en 7 días): reposta carbohidratos y sube las calorías.";
        } else {
            intensity = "MODERATE";
            focus = "BALANCED";
            calorieFactor = 1.05;
            rationale = "Carga moderada: reparto equilibrado de macros con un ligero "
                    + "extra calórico.";
        }

        Double maintenance = weightGoals.findByUser(userId)
                .map(g -> g.maintenanceKcal)
                .orElse(null);
        Integer target = maintenance != null
                ? (int) Math.round(maintenance * calorieFactor)
                : null;

        final String f = focus;
        List<Ranked> ranked = recipes.listByUser(userId).stream()
                .map(r -> rank(r, f))
                .sorted(Comparator.comparingDouble(Ranked::score).reversed())
                .limit(MAX_RESULTS)
                .toList();

        Context context = new Context(sessions, trainingKcal, activityKcal, intensity, focus,
                target, rationale);
        return new RecipeRecommendationDTO(context, ranked);
    }

    /** Puntúa una receta según el foco, a partir de sus macros declaradas. */
    private Ranked rank(Recipe r, String focus) {
        double p = r.protein != null ? r.protein : 0;
        double c = r.carbs != null ? r.carbs : 0;
        double fat = r.fat != null ? r.fat : 0;

        double pCal = p * 4;
        double cCal = c * 4;
        double fCal = fat * 9;
        double totalCal = pCal + cCal + fCal;

        double score;
        String reason;
        if (totalCal <= 0) {
            score = 0;
            reason = "Sin macros: no se puede valorar el encaje.";
        } else {
            double pShare = pCal / totalCal;
            double cShare = cCal / totalCal;
            switch (focus) {
                case "CARB" -> {
                    score = cShare;
                    reason = "Rica en carbohidratos (" + pct(cShare) + "): buen repostaje.";
                }
                case "PROTEIN" -> {
                    score = pShare;
                    reason = "Rica en proteína (" + pct(pShare) + "): ayuda a recuperar.";
                }
                default -> {
                    // cercanía a un reparto equilibrado 50% C / 25% P / 25% G
                    double fShare = fCal / totalCal;
                    score = 1 - (Math.abs(cShare - 0.5) + Math.abs(pShare - 0.25) + Math.abs(fShare - 0.25));
                    reason = "Reparto equilibrado (" + pct(cShare) + " C / " + pct(pShare) + " P).";
                }
            }
        }
        return new Ranked(r.id, r.name, r.calories, r.protein, r.carbs, r.fat, round(score), reason);
    }

    private static String pct(double share) {
        return Math.round(share * 100) + "%";
    }

    private static double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }
}
