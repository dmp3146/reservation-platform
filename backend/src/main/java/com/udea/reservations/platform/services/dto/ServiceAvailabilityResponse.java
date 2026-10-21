package com.udea.reservations.platform.services.dto;

public record ServiceAvailabilityResponse(
    Integer id,
    String nombre,
    Integer duracionMinutos,
    Integer providerId,
    String nombreProveedor,
    boolean tieneHorariosFuturos
) {}
