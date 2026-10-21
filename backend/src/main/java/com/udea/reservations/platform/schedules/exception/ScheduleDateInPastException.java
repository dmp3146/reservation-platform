package com.udea.reservations.platform.schedules.exception;

import com.udea.reservations.platform.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

public class ScheduleDateInPastException extends BusinessRuleException {

    public ScheduleDateInPastException() {
        super(
            "SCHEDULE_DATE_IN_PAST",
            "La fecha de disponibilidad no puede ser anterior al día actual.",
            HttpStatus.BAD_REQUEST.value()
        );
    }
}
