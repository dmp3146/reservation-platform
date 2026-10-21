package com.udea.reservations.platform.providers.exception;

import com.udea.reservations.platform.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

public class ProviderNotFoundException extends BusinessRuleException {

    public ProviderNotFoundException() {
        super(
            "PROVIDER_NOT_FOUND",
            "No se encontró el proveedor solicitado.",
            HttpStatus.NOT_FOUND.value()
        );
    }
}
