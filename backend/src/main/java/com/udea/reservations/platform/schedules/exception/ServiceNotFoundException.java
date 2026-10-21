package com.udea.reservations.platform.schedules.exception;

import com.udea.reservations.platform.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

public class ServiceNotFoundException extends BusinessRuleException {

    public ServiceNotFoundException() {
        super(
            "SERVICE_NOT_FOUND",
            "No se encontró el servicio solicitado.",
            HttpStatus.NOT_FOUND.value()
        );
    }
}
