package com.zensyra.ccollector.core.repository.nutrition;

import com.zensyra.ccollector.core.domain.nutrition.RecipeIngredient;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class RecipeIngredientRepository implements PanacheRepository<RecipeIngredient> {

    public List<RecipeIngredient> listByRecipe(Long recipeId) {
        return list("recipeId", Sort.by("position").ascending(), recipeId);
    }

    public void deleteByRecipe(Long recipeId) {
        delete("recipeId", recipeId);
    }
}
