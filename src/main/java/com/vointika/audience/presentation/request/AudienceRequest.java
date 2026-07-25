package com.vointika.audience.presentation.request;

/** Create/update an audience: name + pax-per-unit (defaults to 1 when omitted). */
public record AudienceRequest(String name, Integer paxPerUnit) {
}
