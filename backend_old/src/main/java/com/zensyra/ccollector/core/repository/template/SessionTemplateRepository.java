package com.zensyra.ccollector.core.repository.template;

import com.zensyra.ccollector.core.domain.template.SessionTemplate;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SessionTemplateRepository implements PanacheRepository<SessionTemplate> {

    public List<SessionTemplate> listByUser(Long userId) {
        return list("userId", Sort.by("name").ascending(), userId);
    }

    public Optional<SessionTemplate> findByIdAndUser(Long id, Long userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }

    public Optional<SessionTemplate> findByNameAndUser(String name, Long userId) {
        return find("name = ?1 and userId = ?2", name, userId).firstResultOptional();
    }
}
