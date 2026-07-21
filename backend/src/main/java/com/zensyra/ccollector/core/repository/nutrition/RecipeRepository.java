package com.zensyra.ccollector.core.repository.nutrition;

import com.zensyra.ccollector.core.domain.nutrition.Recipe;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RecipeRepository implements PanacheRepository<Recipe> {

    public List<Recipe> listByUser(Long userId) {
        return list("userId", Sort.by("name").ascending(), userId);
    }

    public Optional<Recipe> findByIdAndUser(Long id, Long userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }
}
