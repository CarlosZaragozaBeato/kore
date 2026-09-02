package com.zensyra.ccollector.core.dto.plan;

import com.zensyra.ccollector.core.domain.plan.BlockFocus;
import com.zensyra.ccollector.core.domain.plan.BlockLevel;
import com.zensyra.ccollector.core.domain.plan.LoadStance;
import com.zensyra.ccollector.core.domain.plan.TrainingBlock;

import java.time.LocalDate;
import java.util.List;

/** Bloque de periodización con sus hijos anidados (árbol macro→meso→micro). */
public record BlockDTO(
        Long id,
        Long parentId,
        BlockLevel level,
        BlockFocus focus,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        LoadStance loadStance,
        String note,
        List<BlockDTO> children
) {

    public static BlockDTO from(TrainingBlock b, List<BlockDTO> children) {
        return new BlockDTO(b.id, b.parentId, b.level, b.focus, b.name,
                b.startDate, b.endDate, b.loadStance, b.note, children);
    }
}
