package com.vointika.shared.list;

import com.vointika.shared.exception.InvalidFieldException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public final class ValueCoercion {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object coerce(String raw, Class<?> targetType) {
        try {
            if (targetType == String.class) return raw;
            if (targetType == UUID.class) return UUID.fromString(raw);
            if (targetType == Instant.class) return Instant.parse(raw);
            if (targetType == LocalDate.class) return LocalDate.parse(raw);
            if (targetType == LocalDateTime.class) return LocalDateTime.parse(raw);
            if (targetType == LocalTime.class) return LocalTime.parse(raw);
            if (targetType == Integer.class || targetType == int.class) return Integer.valueOf(raw);
            if (targetType == Long.class || targetType == long.class) return Long.valueOf(raw);
            if (targetType == Short.class || targetType == short.class) return Short.valueOf(raw);
            if (targetType == Boolean.class || targetType == boolean.class) {
                // Strict: only the literals "true"/"false" parse — avoid the
                // Boolean.valueOf footgun where any non-"true" string silently
                // becomes false (a filter typo would otherwise return the
                // opposite of the user's intent without error).
                if ("true".equals(raw)) return Boolean.TRUE;
                if ("false".equals(raw)) return Boolean.FALSE;
                throw new InvalidFieldException("Invalid value '" + raw + "' for type Boolean");
            }
            if (targetType == BigDecimal.class) return new BigDecimal(raw);
            if (targetType == BigInteger.class) return new BigInteger(raw);
            if (targetType.isEnum()) {
                return Enum.valueOf((Class<Enum>) targetType, raw);
            }
        } catch (RuntimeException e) {
            throw new InvalidFieldException("Invalid value '" + raw + "' for type " + targetType.getSimpleName());
        }
        throw new IllegalStateException("Unsupported value type: " + targetType);
    }

    private ValueCoercion() {}
}
