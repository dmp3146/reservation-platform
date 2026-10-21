package com.udea.reservations.platform.services.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ServiceCreateRequest(
    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres.")
    String nombre,

    @NotBlank(message = "La descripción es obligatoria.")
    String descripcion,

    @NotNull(message = "La duración en minutos es obligatoria.")
    @Positive(message = "La duración en minutos debe ser mayor que cero.")
    Integer duracionMinutos
) {}
