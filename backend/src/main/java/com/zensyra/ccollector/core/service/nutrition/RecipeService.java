package com.zensyra.ccollector.core.service.nutrition;

import com.zensyra.ccollector.core.domain.nutrition.Recipe;
import com.zensyra.ccollector.core.domain.nutrition.RecipeIngredient;
import com.zensyra.ccollector.core.dto.nutrition.RecipeDTO;
import com.zensyra.ccollector.core.dto.nutrition.RecipeDTO.IngredientDTO;
import com.zensyra.ccollector.core.dto.nutrition.RecipeRequest;
import com.zensyra.ccollector.core.repository.nutrition.RecipeIngredientRepository;
import com.zensyra.ccollector.core.repository.nutrition.RecipeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class RecipeService {

    private final RecipeRepository recipes;
    private final RecipeIngredientRepository ingredients;

    public RecipeService(RecipeRepository recipes, RecipeIngredientRepository ingredients) {
        this.recipes = recipes;
        this.ingredients = ingredients;
    }

    public List<RecipeDTO> list(Long userId) {
        return recipes.listByUser(userId).stream().map(this::toDTO).toList();
    }

    public RecipeDTO get(Long userId, Long id) {
        return toDTO(require(userId, id));
    }

    @Transactional
    public RecipeDTO create(Long userId, RecipeRequest req) {
        validate(req);
        Recipe r = new Recipe();
        r.userId = userId;
        r.createdAt = Instant.now();
        apply(r, req);
        recipes.persist(r);
        replaceIngredients(r.id, req);
        return toDTO(r);
    }

    @Transactional
    public RecipeDTO update(Long userId, Long id, RecipeRequest req) {
        validate(req);
        Recipe r = require(userId, id);
        apply(r, req);
        replaceIngredients(r.id, req);
        return toDTO(r);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Recipe r = require(userId, id);
        ingredients.deleteByRecipe(r.id);
        recipes.delete(r);
    }

    private Recipe require(Long userId, Long id) {
        return recipes.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Receta no encontrada"));
    }

    private void apply(Recipe r, RecipeRequest req) {
        r.name = req.name().trim();
        r.description = blankToNull(req.description());
        r.servings = req.servings();
        r.calories = req.calories();
        r.protein = req.protein();
        r.carbs = req.carbs();
        r.fat = req.fat();
        r.steps = blankToNull(req.steps());
    }

    private void replaceIngredients(Long recipeId, RecipeRequest req) {
        ingredients.deleteByRecipe(recipeId);
        if (req.ingredients() == null) {
            return;
        }
        int position = 0;
        for (RecipeRequest.IngredientRequest ir : req.ingredients()) {
            if (ir.name() == null || ir.name().isBlank()) {
                throw new BadRequestException("Cada ingrediente necesita un nombre");
            }
            RecipeIngredient ingredient = new RecipeIngredient();
            ingredient.recipeId = recipeId;
            ingredient.position = position++;
            ingredient.name = ir.name().trim();
            ingredient.quantity = ir.quantity();
            ingredient.unit = blankToNull(ir.unit());
            ingredients.persist(ingredient);
        }
    }

    private void validate(RecipeRequest req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new BadRequestException("La receta necesita un nombre");
        }
    }

    private RecipeDTO toDTO(Recipe r) {
        List<IngredientDTO> ing = ingredients.listByRecipe(r.id).stream()
                .map(i -> new IngredientDTO(i.name, i.quantity, i.unit))
                .toList();
        return new RecipeDTO(r.id, r.name, r.description, r.servings, r.calories,
                r.protein, r.carbs, r.fat, r.steps, r.createdAt, ing);
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
