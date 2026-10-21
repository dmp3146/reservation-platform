package com.udea.reservations.platform.bookings.exception;

import com.udea.reservations.platform.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

public class BookingCompletedCannotBeCancelledException
    extends BusinessRuleException {

    public BookingCompletedCannotBeCancelledException() {
        super(
            "BOOKING_ALREADY_COMPLETED",
            "La reserva ya fue completada y no puede cancelarse.",
            HttpStatus.CONFLICT.value()
        );
    }
}
