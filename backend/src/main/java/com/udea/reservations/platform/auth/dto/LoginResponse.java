package com.udea.reservations.platform.auth.dto;

public record LoginResponse(
    Integer id,
    String name,
    String email,
    String role
) {}
