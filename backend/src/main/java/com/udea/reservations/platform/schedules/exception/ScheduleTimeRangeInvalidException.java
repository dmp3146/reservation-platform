package com.udea.reservations.platform.schedules.exception;

import com.udea.reservations.platform.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

public class ScheduleTimeRangeInvalidException extends BusinessRuleException {

    public ScheduleTimeRangeInvalidException() {
        super(
            "SCHEDULE_TIME_RANGE_INVALID",
            "La hora de finalización debe ser posterior a la hora de inicio.",
            HttpStatus.BAD_REQUEST.value()
        );
    }
}
