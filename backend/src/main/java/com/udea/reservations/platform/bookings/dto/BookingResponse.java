package com.udea.reservations.platform.bookings.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record BookingResponse(
    Integer bookingId,
    Integer serviceId,
    String serviceName,
    String providerName,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    String status
) {}
