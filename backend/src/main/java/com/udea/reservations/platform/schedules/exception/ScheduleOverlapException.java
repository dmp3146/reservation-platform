package com.udea.reservations.platform.schedules.exception;

import com.udea.reservations.platform.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

public class ScheduleOverlapException extends BusinessRuleException {

    public ScheduleOverlapException() {
        super(
            "SCHEDULE_OVERLAP",
            "El horario se superpone con otro bloque del servicio.",
            HttpStatus.CONFLICT.value()
        );
    }
}
