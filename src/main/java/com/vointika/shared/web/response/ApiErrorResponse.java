package com.vointika.shared.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

public record ApiErrorResponse(
        int status,
        String error,
        String message,
        // Machine-readable error code (e.g. OPERATOR_NOT_READY). Present only
        // when the throw site supplied one; omitted from the JSON otherwise so
        // the common error shape is unchanged.
        @JsonInclude(JsonInclude.Include.NON_NULL) String code,
        Instant timestamp
) {}
