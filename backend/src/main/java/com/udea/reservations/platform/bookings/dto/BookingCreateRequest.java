package com.udea.reservations.platform.bookings.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalTime;

public record BookingCreateRequest(
    @NotNull(message = "El identificador del bloque de disponibilidad es obligatorio.")
    @Positive(message = "El identificador del bloque debe ser positivo.")
    Integer scheduleBlockId,

    @NotNull(message = "La hora de inicio es obligatoria.")
    LocalTime startTime
) {}
