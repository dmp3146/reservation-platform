package com.udea.reservations.platform.bookings.exception;

import com.udea.reservations.platform.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

public class BookingConflictException extends BusinessRuleException {

    public BookingConflictException() {
        super(
            "BOOKING_UNAVAILABLE",
            "El horario solicitado ya no está disponible.",
            HttpStatus.CONFLICT.value()
        );
    }
}
