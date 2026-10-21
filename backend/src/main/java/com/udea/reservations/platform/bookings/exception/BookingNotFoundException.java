package com.udea.reservations.platform.bookings.exception;

import com.udea.reservations.platform.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

public class BookingNotFoundException extends BusinessRuleException {

    public BookingNotFoundException() {
        super(
            "BOOKING_NOT_FOUND",
            "No se encontró la reserva solicitada.",
            HttpStatus.NOT_FOUND.value()
        );
    }
}
