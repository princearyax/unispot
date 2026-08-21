package com.prince.unispot.core.exception;

import java.time.Instant;

public record ErrorResponse(
    String path,
    String error,
    String message,
    int status,
    Instant timestamp
) {}