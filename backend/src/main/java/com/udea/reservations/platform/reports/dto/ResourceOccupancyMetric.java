package com.udea.reservations.platform.reports.dto;

public record ResourceOccupancyMetric(
    Integer resourceId,
    String resourceName,
    long effectiveBookings
) {}
