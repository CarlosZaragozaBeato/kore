package com.zensyra.ccollector.core.client.suunto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Envoltorio de la respuesta de /v2/workouts. Deserialización tolerante: solo
 * leemos lo que necesitamos y aceptamos el resto de campos sin romper.
 *
 * NOTA: los nombres de campo de {@link SuuntoWorkout} deben validarse contra una
 * respuesta real de Suunto; la deserialización tolerante evita fallos, pero un
 * nombre distinto dejaría el campo a null.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SuuntoWorkoutsResponse(List<SuuntoWorkout> payload) {

    public List<SuuntoWorkout> workoutsOrEmpty() {
        return payload == null ? List.of() : payload;
    }
}
