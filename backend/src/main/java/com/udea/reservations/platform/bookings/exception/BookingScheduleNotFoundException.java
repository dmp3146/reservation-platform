package com.udea.reservations.platform.bookings.exception;

import com.udea.reservations.platform.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

public class BookingScheduleNotFoundException extends BusinessRuleException {

    public BookingScheduleNotFoundException() {
        super(
            "SCHEDULE_BLOCK_NOT_FOUND",
            "El bloque de disponibilidad solicitado no existe.",
            HttpStatus.NOT_FOUND.value()
        );
    }
}
