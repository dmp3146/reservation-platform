package com.udea.reservations.platform.services.dto;

public record ServiceResponse(
    Integer id,
    String nombre,
    String descripcion,
    Integer duracionMinutos
) {}
