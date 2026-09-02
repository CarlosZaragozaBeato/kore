package com.zensyra.ccollector.core.domain.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Bloque de periodización (Fase F): una ventana de fechas con un nivel
 * (macro/meso/micro), un foco y una postura de carga objetivo. Se anida por
 * {@code parentId} (un meso cuelga de un macro, un micro de un meso) para dar
 * estructura de temporada sobre el calendario. La relación con las sesiones es
 * <b>derivada por fechas</b> (solape de rango), no por clave ajena.
 */
@Entity
@Table(name = "training_blocks")
public class TrainingBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    /** Bloque contenedor (macro sobre meso, meso sobre micro), o nulo si es raíz. */
    @Column(name = "parent_id")
    public Long parentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public BlockLevel level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    public BlockFocus focus = BlockFocus.GENERAL;

    @Column(nullable = false, length = 200)
    public String name;

    @Column(name = "start_date", nullable = false)
    public LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    public LocalDate endDate;

    /** Postura de carga pretendida para el bloque (descarga/mantener/subir), opcional. */
    @Enumerated(EnumType.STRING)
    @Column(name = "load_stance", length = 10)
    public LoadStance loadStance;

    @Column(length = 2000)
    public String note;
}
