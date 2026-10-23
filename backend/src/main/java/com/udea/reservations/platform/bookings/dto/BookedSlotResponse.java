package com.udea.reservations.platform.bookings.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record BookedSlotResponse(
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime
) {}
