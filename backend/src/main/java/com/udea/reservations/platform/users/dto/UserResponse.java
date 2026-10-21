package com.udea.reservations.platform.users.dto;

public record UserResponse(
    Integer id,
    String name,
    String email,
    String rol
) {}
