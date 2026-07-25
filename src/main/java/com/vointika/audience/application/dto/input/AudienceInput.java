package com.vointika.audience.application.dto.input;

/** The editable fields of an audience (create + update). */
public record AudienceInput(String name, Integer paxPerUnit) {
}
