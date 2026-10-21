package com.udea.reservations.platform.reports.controller;

import com.udea.reservations.platform.common.dto.ApiSuccessResponse;
import com.udea.reservations.platform.reports.dto.OccupancyReportResponse;
import com.udea.reservations.platform.reports.service.OccupancyReportService;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class OccupancyReportController {

    private final OccupancyReportService reportService;

    public OccupancyReportController(OccupancyReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/occupancy")
    public ResponseEntity<ApiSuccessResponse<OccupancyReportResponse>> getOccupancy(
        @RequestParam(required = false) LocalDate startDate,
        @RequestParam(required = false) LocalDate endDate
    ) {
        return ResponseEntity.ok(new ApiSuccessResponse<>(
            "Reporte de ocupación consultado exitosamente.",
            reportService.generate(startDate, endDate)
        ));
    }
}
