package com.zensyra.ccollector.core.service.nutrition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zensyra.ccollector.core.domain.nutrition.BaseUnit;
import com.zensyra.ccollector.core.domain.nutrition.Ingredient;
import com.zensyra.ccollector.core.dto.catalog.SeedResult;
import com.zensyra.ccollector.core.dto.nutrition.IngredientRequest;
import com.zensyra.ccollector.core.repository.nutrition.IngredientRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.io.InputStream;
import java.util.List;

@ApplicationScoped
public class IngredientService {

    private static final String SEED = "/seeds/ingredients.json";

    private final IngredientRepository ingredients;
    private final ObjectMapper mapper;

    public IngredientService(IngredientRepository ingredients, ObjectMapper mapper) {
        this.ingredients = ingredients;
        this.mapper = mapper;
    }

    public List<Ingredient> list(Long userId) {
        return ingredients.listByUser(userId);
    }

    @Transactional
    public Ingredient create(Long userId, IngredientRequest req) {
        validate(req);
        Ingredient i = new Ingredient();
        i.userId = userId;
        apply(i, req);
        ingredients.persist(i);
        return i;
    }

    @Transactional
    public Ingredient update(Long userId, Long id, IngredientRequest req) {
        validate(req);
        Ingredient i = get(userId, id);
        apply(i, req);
        return i;
    }

    @Transactional
    public void delete(Long userId, Long id) {
        ingredients.delete(get(userId, id));
    }

    /** Carga el catálogo de ejemplo (JSON empaquetado). Idempotente por nombre. */
    @Transactional
    public SeedResult seed(Long userId) {
        List<IngredientRequest> seeds = readSeed();
        int added = 0;
        int skipped = 0;
        for (IngredientRequest req : seeds) {
            if (req.name() == null || req.name().isBlank()
                    || ingredients.findByNameAndUser(req.name().trim(), userId).isPresent()) {
                skipped++;
                continue;
            }
            Ingredient i = new Ingredient();
            i.userId = userId;
            apply(i, req);
            ingredients.persist(i);
            added++;
        }
        return new SeedResult(added, skipped, seeds.size());
    }

    private List<IngredientRequest> readSeed() {
        try (InputStream in = IngredientService.class.getResourceAsStream(SEED)) {
            if (in == null) {
                return List.of();
            }
            return mapper.readValue(in, mapper.getTypeFactory()
                    .constructCollectionType(List.class, IngredientRequest.class));
        } catch (Exception e) {
            throw new BadRequestException("No se pudo leer el catálogo de ejemplo de ingredientes");
        }
    }

    private Ingredient get(Long userId, Long id) {
        return ingredients.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Ingrediente no encontrado"));
    }

    private void apply(Ingredient i, IngredientRequest req) {
        i.name = req.name().trim();
        i.baseUnit = req.baseUnit() == null ? BaseUnit.GRAM : req.baseUnit();
        i.imageUrl = req.imageUrl() == null || req.imageUrl().isBlank() ? null : req.imageUrl().trim();
        i.calories = req.calories();
        i.protein = req.protein();
        i.carbs = req.carbs();
        i.fat = req.fat();
        i.fiber = req.fiber();
        i.sugars = req.sugars();
        i.sodium = req.sodium();
    }

    private void validate(IngredientRequest req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new BadRequestException("El ingrediente necesita un nombre");
        }
        if (negative(req.calories()) || negative(req.protein()) || negative(req.carbs())
                || negative(req.fat()) || negative(req.fiber()) || negative(req.sugars())
                || negative(req.sodium())) {
            throw new BadRequestException("Los valores nutricionales no pueden ser negativos");
        }
    }

    private static boolean negative(Double v) {
        return v != null && v < 0;
    }
}
