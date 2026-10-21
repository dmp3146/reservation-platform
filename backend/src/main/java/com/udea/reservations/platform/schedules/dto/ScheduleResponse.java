package com.udea.reservations.platform.schedules.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleResponse(
    Integer id,
    Integer serviceId,
    LocalDate fecha,
    LocalTime horaInicio,
    LocalTime horaFin
) {}
