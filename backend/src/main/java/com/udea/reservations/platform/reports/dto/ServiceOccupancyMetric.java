package com.udea.reservations.platform.reports.dto;

public record ServiceOccupancyMetric(
    Integer serviceId,
    String serviceName,
    long effectiveBookings,
    long cancelledBookings,
    long reservedMinutes
) {}
