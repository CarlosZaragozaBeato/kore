package com.zensyra.ccollector.core.service.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zensyra.ccollector.core.domain.template.SessionTemplate;
import com.zensyra.ccollector.core.domain.workout.WorkoutType;
import com.zensyra.ccollector.core.dto.catalog.SeedResult;
import com.zensyra.ccollector.core.dto.template.SessionTemplateRequest;
import com.zensyra.ccollector.core.repository.template.SessionTemplateRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.io.InputStream;
import java.util.List;

@ApplicationScoped
public class SessionTemplateService {

    private static final String SEED = "/seeds/session-templates.json";

    private final SessionTemplateRepository templates;
    private final ObjectMapper mapper;

    public SessionTemplateService(SessionTemplateRepository templates, ObjectMapper mapper) {
        this.templates = templates;
        this.mapper = mapper;
    }

    public List<SessionTemplate> list(Long userId) {
        return templates.listByUser(userId);
    }

    @Transactional
    public SessionTemplate create(Long userId, SessionTemplateRequest req) {
        validate(req);
        SessionTemplate t = new SessionTemplate();
        t.userId = userId;
        apply(t, req);
        templates.persist(t);
        return t;
    }

    @Transactional
    public SessionTemplate update(Long userId, Long id, SessionTemplateRequest req) {
        validate(req);
        SessionTemplate t = get(userId, id);
        apply(t, req);
        return t;
    }

    @Transactional
    public void delete(Long userId, Long id) {
        templates.delete(get(userId, id));
    }

    /** Carga plantillas de ejemplo (JSON empaquetado). Idempotente por nombre. */
    @Transactional
    public SeedResult seed(Long userId) {
        List<SessionTemplateRequest> seeds = readSeed();
        int added = 0;
        int skipped = 0;
        for (SessionTemplateRequest req : seeds) {
            if (req.name() == null || req.name().isBlank()
                    || templates.findByNameAndUser(req.name().trim(), userId).isPresent()) {
                skipped++;
                continue;
            }
            SessionTemplate t = new SessionTemplate();
            t.userId = userId;
            apply(t, req);
            templates.persist(t);
            added++;
        }
        return new SeedResult(added, skipped, seeds.size());
    }

    private List<SessionTemplateRequest> readSeed() {
        try (InputStream in = SessionTemplateService.class.getResourceAsStream(SEED)) {
            if (in == null) {
                return List.of();
            }
            return mapper.readValue(in, mapper.getTypeFactory()
                    .constructCollectionType(List.class, SessionTemplateRequest.class));
        } catch (Exception e) {
            throw new BadRequestException("No se pudo leer el catálogo de ejemplo de plantillas");
        }
    }

    private SessionTemplate get(Long userId, Long id) {
        return templates.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Plantilla no encontrada"));
    }

    private void apply(SessionTemplate t, SessionTemplateRequest req) {
        t.name = req.name().trim();
        t.discipline = req.discipline() == null ? WorkoutType.RUNNING : req.discipline();
        t.goal = req.goal();
        t.level = req.level();
        t.targetDistanceMeters = req.targetDistanceMeters();
        t.targetDurationSeconds = req.targetDurationSeconds();
        t.structure = blankToNull(req.structure());
        t.notes = blankToNull(req.notes());
    }

    private void validate(SessionTemplateRequest req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new BadRequestException("La plantilla necesita un nombre");
        }
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
