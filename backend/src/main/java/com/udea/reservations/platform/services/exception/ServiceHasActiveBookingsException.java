package com.udea.reservations.platform.services.exception;

import com.udea.reservations.platform.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

public class ServiceHasActiveBookingsException extends BusinessRuleException {

    public ServiceHasActiveBookingsException() {
        super(
            "SERVICE_HAS_ACTIVE_BOOKINGS",
            "No se puede retirar el servicio porque tiene reservas activas.",
            HttpStatus.CONFLICT.value()
        );
    }
}
