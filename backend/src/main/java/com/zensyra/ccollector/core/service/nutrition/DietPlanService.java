package com.zensyra.ccollector.core.service.nutrition;

import com.zensyra.ccollector.core.domain.nutrition.DietMeal;
import com.zensyra.ccollector.core.domain.nutrition.DietPlan;
import com.zensyra.ccollector.core.dto.nutrition.DietPlanDTO;
import com.zensyra.ccollector.core.dto.nutrition.DietPlanDTO.MealDTO;
import com.zensyra.ccollector.core.dto.nutrition.DietPlanRequest;
import com.zensyra.ccollector.core.repository.nutrition.DietMealRepository;
import com.zensyra.ccollector.core.repository.nutrition.DietPlanRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class DietPlanService {

    private final DietPlanRepository plans;
    private final DietMealRepository meals;

    public DietPlanService(DietPlanRepository plans, DietMealRepository meals) {
        this.plans = plans;
        this.meals = meals;
    }

    public List<DietPlanDTO> list(Long userId) {
        return plans.listByUser(userId).stream().map(this::toDTO).toList();
    }

    public DietPlanDTO get(Long userId, Long id) {
        return toDTO(require(userId, id));
    }

    @Transactional
    public DietPlanDTO create(Long userId, DietPlanRequest req) {
        validate(req);
        DietPlan p = new DietPlan();
        p.userId = userId;
        p.createdAt = Instant.now();
        apply(p, req);
        plans.persist(p);
        replaceMeals(p.id, req);
        return toDTO(p);
    }

    @Transactional
    public DietPlanDTO update(Long userId, Long id, DietPlanRequest req) {
        validate(req);
        DietPlan p = require(userId, id);
        apply(p, req);
        replaceMeals(p.id, req);
        return toDTO(p);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        DietPlan p = require(userId, id);
        meals.deleteByPlan(p.id);
        plans.delete(p);
    }

    private DietPlan require(Long userId, Long id) {
        return plans.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Plan de dieta no encontrado"));
    }

    private void apply(DietPlan p, DietPlanRequest req) {
        p.name = req.name().trim();
        p.startDate = req.startDate();
        p.endDate = req.endDate();
        p.targetCalories = req.targetCalories();
        p.targetProtein = req.targetProtein();
        p.targetCarbs = req.targetCarbs();
        p.targetFat = req.targetFat();
        p.notes = blankToNull(req.notes());
    }

    private void replaceMeals(Long planId, DietPlanRequest req) {
        meals.deleteByPlan(planId);
        if (req.meals() == null) {
            return;
        }
        for (DietPlanRequest.MealRequest mr : req.meals()) {
            if (mr.date() == null || mr.mealType() == null) {
                throw new BadRequestException("Cada comida necesita fecha y tipo");
            }
            DietMeal meal = new DietMeal();
            meal.dietPlanId = planId;
            meal.date = mr.date();
            meal.mealType = mr.mealType();
            meal.recipeName = blankToNull(mr.recipeName());
            meal.notes = blankToNull(mr.notes());
            meals.persist(meal);
        }
    }

    private void validate(DietPlanRequest req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new BadRequestException("El plan de dieta necesita un nombre");
        }
        if (req.startDate() == null) {
            throw new BadRequestException("El plan de dieta necesita una fecha de inicio");
        }
        if (req.endDate() != null && req.endDate().isBefore(req.startDate())) {
            throw new BadRequestException("La fecha de fin no puede ser anterior al inicio");
        }
    }

    private DietPlanDTO toDTO(DietPlan p) {
        List<MealDTO> mealDTOs = meals.listByPlan(p.id).stream()
                .map(m -> new MealDTO(m.date, m.mealType, m.recipeName, m.notes))
                .toList();
        return new DietPlanDTO(p.id, p.name, p.startDate, p.endDate, p.targetCalories,
                p.targetProtein, p.targetCarbs, p.targetFat, p.notes, p.createdAt, mealDTOs);
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
