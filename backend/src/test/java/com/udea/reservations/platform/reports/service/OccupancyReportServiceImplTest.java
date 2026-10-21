package com.udea.reservations.platform.reports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.udea.reservations.platform.reports.dto.OccupancyReportResponse;
import com.udea.reservations.platform.reports.dto.ResourceOccupancyMetric;
import com.udea.reservations.platform.reports.dto.ServiceOccupancyMetric;
import com.udea.reservations.platform.reports.exception.InvalidOccupancyDateRangeException;
import com.udea.reservations.platform.reports.repository.OccupancyReportRepository;
import com.udea.reservations.platform.reports.repository.OccupancyReportRepository.OccupancySummaryProjection;
import com.udea.reservations.platform.reports.repository.OccupancyReportRepository.ServiceOccupancyProjection;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OccupancyReportServiceImplTest {

    @Mock
    private OccupancyReportRepository reportRepository;

    @Mock
    private OccupancySummaryProjection summary;

    @Mock
    private ServiceOccupancyProjection serviceMetric;

    @Mock
    private OccupancyReportRepository.ResourceOccupancyProjection resourceMetric;

    @InjectMocks
    private OccupancyReportServiceImpl reportService;

    @Test
    void returnsDateBoundedSummaryServiceMetricsAndResourceLimitation() {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 31);
        when(reportRepository.findSummary(startDate, endDate)).thenReturn(summary);
        when(summary.getTotalBookings()).thenReturn(6L);
        when(summary.getEffectiveBookings()).thenReturn(4L);
        when(summary.getCancelledBookings()).thenReturn(2L);
        when(summary.getBookingsWithoutServiceAttribution()).thenReturn(1L);
        when(reportRepository.findServiceMetrics(startDate, endDate))
            .thenReturn(List.of(serviceMetric));
        when(serviceMetric.getServiceId()).thenReturn(15);
        when(serviceMetric.getServiceName()).thenReturn("Consulta");
        when(serviceMetric.getEffectiveBookings()).thenReturn(3L);
        when(serviceMetric.getCancelledBookings()).thenReturn(1L);
        when(serviceMetric.getReservedMinutes()).thenReturn(120L);
        when(reportRepository.findResourceMetrics(startDate, endDate))
            .thenReturn(List.of(resourceMetric));
        when(resourceMetric.getResourceId()).thenReturn(7);
        when(resourceMetric.getResourceName()).thenReturn("Consultorio 1");
        when(resourceMetric.getEffectiveBookings()).thenReturn(2L);

        OccupancyReportResponse response =
            reportService.generate(startDate, endDate);

        assertThat(response.startDate()).isEqualTo(startDate);
        assertThat(response.endDate()).isEqualTo(endDate);
        assertThat(response.totalBookings()).isEqualTo(6);
        assertThat(response.effectiveBookings()).isEqualTo(4);
        assertThat(response.cancelledBookings()).isEqualTo(2);
        assertThat(response.bookingsWithoutServiceAttribution()).isEqualTo(1);
        assertThat(response.hasBookings()).isTrue();
        assertThat(response.services()).containsExactly(
            new ServiceOccupancyMetric(15, "Consulta", 3, 1, 120)
        );
        assertThat(response.resources()).containsExactly(
            new ResourceOccupancyMetric(7, "Consultorio 1", 2)
        );
        assertThat(response.resourceMetricsAvailable()).isTrue();
        assertThat(response.resourceMetricsLimitation())
            .contains("horario_disponible", "disponibilidad_servicio");
        verify(reportRepository).findSummary(startDate, endDate);
        verify(reportRepository).findServiceMetrics(startDate, endDate);
        verify(reportRepository).findResourceMetrics(startDate, endDate);
    }

    @Test
    void returnsEmptyValidReportWhenPeriodHasNoBookings() {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 31);
        when(reportRepository.findSummary(startDate, endDate)).thenReturn(summary);
        when(summary.getTotalBookings()).thenReturn(0L);
        when(summary.getEffectiveBookings()).thenReturn(0L);
        when(summary.getCancelledBookings()).thenReturn(0L);
        when(summary.getBookingsWithoutServiceAttribution()).thenReturn(0L);
        when(reportRepository.findServiceMetrics(startDate, endDate))
            .thenReturn(List.of());
        when(reportRepository.findResourceMetrics(startDate, endDate))
            .thenReturn(List.of());

        OccupancyReportResponse response =
            reportService.generate(startDate, endDate);

        assertThat(response.totalBookings()).isZero();
        assertThat(response.effectiveBookings()).isZero();
        assertThat(response.cancelledBookings()).isZero();
        assertThat(response.bookingsWithoutServiceAttribution()).isZero();
        assertThat(response.hasBookings()).isFalse();
        assertThat(response.services()).isEmpty();
        assertThat(response.resources()).isEmpty();
        assertThat(response.resourceMetricsAvailable()).isTrue();
    }

    @Test
    void rejectsMissingDatesAndReversedRangeWithoutQueryingRepository() {
        LocalDate date = LocalDate.of(2026, 10, 1);
        LocalDate followingDate = LocalDate.of(2026, 10, 2);

        assertThatThrownBy(() -> reportService.generate(null, date))
            .isInstanceOf(InvalidOccupancyDateRangeException.class);
        assertThatThrownBy(() -> reportService.generate(date, null))
            .isInstanceOf(InvalidOccupancyDateRangeException.class);
        assertThatThrownBy(() -> reportService.generate(followingDate, date))
            .isInstanceOf(InvalidOccupancyDateRangeException.class);

        verifyNoInteractions(reportRepository);
    }

    @Test
    void acceptsSingleDayDateRangeAndPassesInclusiveBoundsToQueries() {
        LocalDate date = LocalDate.of(2026, 10, 21);
        when(reportRepository.findSummary(date, date)).thenReturn(summary);
        when(summary.getTotalBookings()).thenReturn(0L);
        when(summary.getEffectiveBookings()).thenReturn(0L);
        when(summary.getCancelledBookings()).thenReturn(0L);
        when(summary.getBookingsWithoutServiceAttribution()).thenReturn(0L);
        when(reportRepository.findServiceMetrics(date, date)).thenReturn(List.of());
        when(reportRepository.findResourceMetrics(date, date)).thenReturn(List.of());

        reportService.generate(date, date);

        verify(reportRepository).findSummary(date, date);
        verify(reportRepository).findServiceMetrics(date, date);
        verify(reportRepository).findResourceMetrics(date, date);
    }
}
