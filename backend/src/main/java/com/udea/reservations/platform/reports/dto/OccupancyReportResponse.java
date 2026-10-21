package com.udea.reservations.platform.reports.dto;

import java.time.LocalDate;
import java.util.List;

public record OccupancyReportResponse(
    LocalDate startDate,
    LocalDate endDate,
    long totalBookings,
    long effectiveBookings,
    long cancelledBookings,
    long bookingsWithoutServiceAttribution,
    boolean hasBookings,
    List<ServiceOccupancyMetric> services,
    List<ResourceOccupancyMetric> resources,
    boolean resourceMetricsAvailable,
    String resourceMetricsLimitation
) {}
