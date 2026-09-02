package com.zensyra.ccollector.core.service.energy;

import com.zensyra.ccollector.core.domain.nutrition.DietPlan;
import com.zensyra.ccollector.core.domain.nutrition.Ingredient;
import com.zensyra.ccollector.core.domain.nutrition.Recipe;
import com.zensyra.ccollector.core.domain.nutrition.RecipeIngredient;
import com.zensyra.ccollector.core.domain.workout.Workout;
import com.zensyra.ccollector.core.dto.energy.EnergyDayDTO;
import com.zensyra.ccollector.core.dto.energy.EnergySummaryDTO;
import com.zensyra.ccollector.core.dto.energy.EnergyWeekDTO;
import com.zensyra.ccollector.core.repository.nutrition.DietMealRepository;
import com.zensyra.ccollector.core.repository.nutrition.DietPlanRepository;
import com.zensyra.ccollector.core.repository.nutrition.IngredientRepository;
import com.zensyra.ccollector.core.repository.activity.DailyActivityRepository;
import com.zensyra.ccollector.core.repository.nutrition.RecipeIngredientRepository;
import com.zensyra.ccollector.core.repository.nutrition.RecipeRepository;
import com.zensyra.ccollector.core.repository.weight.WeightGoalRepository;
import com.zensyra.ccollector.core.repository.workout.WorkoutRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Balance energético del atleta: kcal consumidas (comidas de las dietas →
 * receta → catálogo de ingredientes) vs quemadas (energía de Suunto o estimada).
 * Sirve al objetivo central de Kore: mantener el peso (ni subir ni bajar).
 */
@ApplicationScoped
public class EnergyService {

    private static final int DAYS = 14;
    private static final int WEEKS = 8;
    /** Margen (kcal) alrededor del mantenimiento para considerar "equilibrio". */
    private static final double MARGIN = 150;

    private final WorkoutRepository workouts;
    private final DietPlanRepository dietPlans;
    private final DietMealRepository dietMeals;
    private final RecipeRepository recipes;
    private final RecipeIngredientRepository recipeIngredients;
    private final IngredientRepository ingredients;
    private final WeightGoalRepository weightGoals;
    private final DailyActivityRepository dailyActivity;

    public EnergyService(WorkoutRepository workouts, DietPlanRepository dietPlans,
                         DietMealRepository dietMeals, RecipeRepository recipes,
                         RecipeIngredientRepository recipeIngredients, IngredientRepository ingredients,
                         WeightGoalRepository weightGoals, DailyActivityRepository dailyActivity) {
        this.workouts = workouts;
        this.dietPlans = dietPlans;
        this.dietMeals = dietMeals;
        this.recipes = recipes;
        this.recipeIngredients = recipeIngredients;
        this.ingredients = ingredients;
        this.weightGoals = weightGoals;
        this.dailyActivity = dailyActivity;
    }

    public EnergySummaryDTO summary(Long userId) {
        Double maintenance = weightGoals.findByUser(userId)
                .map(g -> g.maintenanceKcal).orElse(null);

        Map<LocalDate, Double> consumed = consumedByDate(userId);
        Map<LocalDate, Double> burned = burnedByDate(userId);

        LocalDate today = LocalDate.now();
        List<EnergyDayDTO> days = new ArrayList<>();
        for (int i = DAYS - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            long c = Math.round(consumed.getOrDefault(d, 0.0));
            long b = Math.round(burned.getOrDefault(d, 0.0));
            long balance = c - b;
            days.add(new EnergyDayDTO(d, c, b, balance, state(balance, maintenance, MARGIN)));
        }

        return new EnergySummaryDTO(Instant.now(), maintenance, days, weekly(consumed, burned, maintenance, today));
    }

    private List<EnergyWeekDTO> weekly(Map<LocalDate, Double> consumed, Map<LocalDate, Double> burned,
                                       Double maintenance, LocalDate today) {
        LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Map<LocalDate, double[]> buckets = new LinkedHashMap<>();
        for (int i = WEEKS - 1; i >= 0; i--) {
            buckets.put(thisMonday.minusWeeks(i), new double[2]);
        }
        accumulate(consumed, thisMonday, buckets, 0);
        accumulate(burned, thisMonday, buckets, 1);

        List<EnergyWeekDTO> out = new ArrayList<>();
        Double weeklyMaintenance = maintenance == null ? null : maintenance * 7;
        buckets.forEach((monday, cb) -> {
            long c = Math.round(cb[0]);
            long b = Math.round(cb[1]);
            long balance = c - b;
            String label = monday.get(IsoFields.WEEK_BASED_YEAR) + "-W"
                    + String.format("%02d", monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            out.add(new EnergyWeekDTO(label, monday, monday.plusDays(6), c, b, balance,
                    state(balance, weeklyMaintenance, MARGIN * 7)));
        });
        return out;
    }

    private static void accumulate(Map<LocalDate, Double> byDate, LocalDate firstMonday,
                                   Map<LocalDate, double[]> buckets, int index) {
        byDate.forEach((date, value) -> {
            LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            double[] cb = buckets.get(monday);
            if (cb != null) {
                cb[index] += value;
            }
        });
    }

    private Map<LocalDate, Double> consumedByDate(Long userId) {
        Map<String, Double> recipeKcal = recipeKcalByName(userId);
        Map<LocalDate, Double> consumed = new HashMap<>();
        for (DietPlan plan : dietPlans.listByUser(userId)) {
            dietMeals.listByPlan(plan.id).forEach(m -> {
                if (m.recipeName == null) {
                    return;
                }
                Double kcal = recipeKcal.get(m.recipeName.trim());
                if (kcal != null) {
                    consumed.merge(m.date, kcal, Double::sum);
                }
            });
        }
        return consumed;
    }

    private Map<String, Double> recipeKcalByName(Long userId) {
        Map<Long, Ingredient> catalog = new HashMap<>();
        for (Ingredient i : ingredients.listByUser(userId)) {
            catalog.put(i.id, i);
        }
        Map<String, Double> byName = new HashMap<>();
        for (Recipe r : recipes.listByUser(userId)) {
            if (r.name == null) {
                continue;
            }
            double kcal = r.calories != null ? r.calories : deriveKcal(r.id, catalog);
            byName.put(r.name.trim(), kcal);
        }
        return byName;
    }

    /** Deriva kcal de una receta desde el catálogo (por 100 g/ml × cantidad). */
    private double deriveKcal(Long recipeId, Map<Long, Ingredient> catalog) {
        double kcal = 0;
        for (RecipeIngredient ri : recipeIngredients.listByRecipe(recipeId)) {
            if (ri.ingredientId == null || ri.quantity == null) {
                continue;
            }
            Ingredient ing = catalog.get(ri.ingredientId);
            if (ing != null && ing.calories != null) {
                kcal += ing.calories * ri.quantity / 100.0;
            }
        }
        return kcal;
    }

    private Map<LocalDate, Double> burnedByDate(Long userId) {
        Map<LocalDate, Double> burned = new HashMap<>();
        for (Workout w : workouts.listByUser(userId)) {
            burned.merge(w.date, EnergyEstimate.burnedKcal(w), Double::sum);
        }
        // La quema diaria (Suunto/manual) es el TOTAL del día (basal + actividad
        // + entrenos): cuando existe, sustituye a la estimación por entrenos para
        // ese día, evitando contar dos veces.
        dailyActivity.listByUser(userId).forEach(a -> {
            if (a.burnedKcal != null) {
                burned.put(a.date, a.burnedKcal);
            }
        });
        return burned;
    }

    private static String state(long balance, Double maintenance, double margin) {
        if (maintenance == null) {
            return "UNKNOWN";
        }
        if (balance > maintenance + margin) {
            return "SURPLUS";
        }
        if (balance < maintenance - margin) {
            return "DEFICIT";
        }
        return "BALANCED";
    }
}
