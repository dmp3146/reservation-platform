package com.udea.reservations.platform.bookings.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record BookingDetailResponse(
    Integer bookingId,
    Integer serviceId,
    String serviceName,
    String providerName,
    String customerName,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    String status
) {}
