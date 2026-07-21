package com.zensyra.ccollector.core.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;

/**
 * Guarda los {@link Instant} como texto ISO-8601 en SQLite. Evita el manejo de
 * fechas del driver (que mezcla epoch-millis y texto) y deja la base de datos
 * legible/portable. El orden lexicográfico ISO coincide con el cronológico.
 */
@Converter(autoApply = true)
public class InstantStringConverter implements AttributeConverter<Instant, String> {

    @Override
    public String convertToDatabaseColumn(Instant attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public Instant convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Instant.parse(dbData);
    }
}
