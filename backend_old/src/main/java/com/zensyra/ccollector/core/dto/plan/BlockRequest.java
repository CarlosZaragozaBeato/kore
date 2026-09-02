package com.zensyra.ccollector.core.dto.plan;

import com.zensyra.ccollector.core.domain.plan.BlockFocus;
import com.zensyra.ccollector.core.domain.plan.BlockLevel;
import com.zensyra.ccollector.core.domain.plan.LoadStance;

import java.time.LocalDate;
import java.util.List;

/**
 * Alta/edición de un bloque de periodización. Puede colgar de un bloque
 * existente ({@code parentId}) o traer sus propios hijos anidados
 * ({@code children}) para crear una temporada entera de una vez (macro con sus
 * mesos y micros). En edición se ignoran los hijos: cada bloque se edita solo.
 */
public record BlockRequest(
        Long parentId,
        BlockLevel level,
        BlockFocus focus,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        LoadStance loadStance,
        String note,
        List<BlockRequest> children
) {
}
