package com.udea.reservations.platform.common.dto;

import java.util.List;
import java.util.UUID;

public record ApiErrorResponse(
    String errorCode,
    String message,
    List<String> details,
    String traceId
) {
    public static ApiErrorResponse of(String errorCode, String message, List<String> details) {
        return new ApiErrorResponse(errorCode, message, details, UUID.randomUUID().toString());
    }
}