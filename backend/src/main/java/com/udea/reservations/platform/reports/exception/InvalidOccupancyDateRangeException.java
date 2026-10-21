package com.udea.reservations.platform.reports.exception;

import com.udea.reservations.platform.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

public class InvalidOccupancyDateRangeException extends BusinessRuleException {

    public InvalidOccupancyDateRangeException() {
        super(
            "INVALID_REPORT_DATE_RANGE",
            "Las fechas inicial y final son obligatorias y la fecha inicial "
                + "no puede ser posterior a la fecha final.",
            HttpStatus.BAD_REQUEST.value()
        );
    }
}
