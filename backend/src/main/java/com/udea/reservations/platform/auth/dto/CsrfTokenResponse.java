package com.udea.reservations.platform.auth.dto;

public record CsrfTokenResponse(
    String token,
    String headerName,
    String parameterName
) {}
