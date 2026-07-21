package com.vointika.touroperator.presentation.request;

import java.util.UUID;

/** Points the operator's logo at one of its media records. */
public record SetOperatorLogoRequest(UUID mediaId) {
}
