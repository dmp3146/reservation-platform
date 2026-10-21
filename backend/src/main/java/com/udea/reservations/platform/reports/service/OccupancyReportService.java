package com.udea.reservations.platform.reports.service;

import com.udea.reservations.platform.reports.dto.OccupancyReportResponse;
import java.time.LocalDate;

public interface OccupancyReportService {

    OccupancyReportResponse generate(LocalDate startDate, LocalDate endDate);
}
