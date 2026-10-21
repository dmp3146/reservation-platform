package com.udea.reservations.platform.bookings.exception;

import com.udea.reservations.platform.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

public class BookingAlreadyCancelledException extends BusinessRuleException {

    public BookingAlreadyCancelledException() {
        super(
            "BOOKING_ALREADY_CANCELLED",
            "La reserva ya fue cancelada.",
            HttpStatus.CONFLICT.value()
        );
    }
}
