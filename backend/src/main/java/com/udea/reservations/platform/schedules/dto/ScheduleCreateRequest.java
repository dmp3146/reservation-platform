package com.udea.reservations.platform.schedules.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleCreateRequest(
    @NotNull(message = "El servicio es obligatorio.")
    @Positive(message = "El identificador del servicio debe ser positivo.")
    Integer serviceId,

    @NotNull(message = "La fecha es obligatoria.")
    LocalDate fecha,

    @NotNull(message = "La hora de inicio es obligatoria.")
    LocalTime horaInicio,

    @NotNull(message = "La hora de finalización es obligatoria.")
    LocalTime horaFin
) {}
