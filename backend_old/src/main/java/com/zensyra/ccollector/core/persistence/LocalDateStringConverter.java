package com.zensyra.ccollector.core.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;

/**
 * Guarda los {@link LocalDate} como texto ISO-8601 (yyyy-MM-dd) en SQLite,
 * por la misma razón que {@link InstantStringConverter}.
 */
@Converter(autoApply = true)
public class LocalDateStringConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        return dbData == null ? null : LocalDate.parse(dbData);
    }
}
