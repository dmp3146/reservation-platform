package com.udea.reservations.platform.reports.service;

import com.udea.reservations.platform.reports.dto.OccupancyReportResponse;
import com.udea.reservations.platform.reports.dto.ResourceOccupancyMetric;
import com.udea.reservations.platform.reports.dto.ServiceOccupancyMetric;
import com.udea.reservations.platform.reports.exception.InvalidOccupancyDateRangeException;
import com.udea.reservations.platform.reports.repository.OccupancyReportRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OccupancyReportServiceImpl implements OccupancyReportService {

    private static final String RESOURCE_LIMITATION =
        "Solo las reservas vinculadas a horario_disponible pueden atribuirse "
            + "a recursos mediante agenda. Las reservas vinculadas a "
            + "disponibilidad_servicio no tienen una relación con recursos.";

    private final OccupancyReportRepository reportRepository;

    public OccupancyReportServiceImpl(
        OccupancyReportRepository reportRepository
    ) {
        this.reportRepository = reportRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OccupancyReportResponse generate(
        LocalDate startDate,
        LocalDate endDate
    ) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new InvalidOccupancyDateRangeException();
        }

        OccupancyReportRepository.OccupancySummaryProjection summary =
            reportRepository.findSummary(startDate, endDate);
        List<ServiceOccupancyMetric> serviceMetrics = reportRepository
            .findServiceMetrics(startDate, endDate)
            .stream()
            .map(metric -> new ServiceOccupancyMetric(
                metric.getServiceId(),
                metric.getServiceName(),
                metric.getEffectiveBookings(),
                metric.getCancelledBookings(),
                metric.getReservedMinutes()
            ))
            .toList();
        var resourceMetrics = reportRepository
            .findResourceMetrics(startDate, endDate)
            .stream()
            .map(metric -> new ResourceOccupancyMetric(
                metric.getResourceId(),
                metric.getResourceName(),
                metric.getEffectiveBookings()
            ))
            .toList();

        return new OccupancyReportResponse(
            startDate,
            endDate,
            summary.getTotalBookings(),
            summary.getEffectiveBookings(),
            summary.getCancelledBookings(),
            summary.getBookingsWithoutServiceAttribution(),
            summary.getTotalBookings() > 0,
            serviceMetrics,
            resourceMetrics,
            true,
            RESOURCE_LIMITATION
        );
    }
}
