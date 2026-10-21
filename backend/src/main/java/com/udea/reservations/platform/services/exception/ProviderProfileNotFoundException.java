package com.udea.reservations.platform.services.exception;

import com.udea.reservations.platform.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

public class ProviderProfileNotFoundException extends BusinessRuleException {

    public ProviderProfileNotFoundException() {
        super(
            "PROVIDER_PROFILE_NOT_FOUND",
            "No se encontró el perfil de proveedor asociado a la cuenta.",
            HttpStatus.NOT_FOUND.value()
        );
    }
}
