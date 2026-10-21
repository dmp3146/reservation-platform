package com.udea.reservations.platform.bookings.exception;

import com.udea.reservations.platform.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

public class BookingDateTimeInPastException extends BusinessRuleException {

    public BookingDateTimeInPastException() {
        super(
            "BOOKING_DATETIME_IN_PAST",
            "No se puede reservar un horario que ya pasó.",
            HttpStatus.BAD_REQUEST.value()
        );
    }
}
