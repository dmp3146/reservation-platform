package com.udea.reservations.platform.users.exception;

import com.udea.reservations.platform.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyRegisteredException extends BusinessRuleException {

    public EmailAlreadyRegisteredException() {
        super(
            "EMAIL_ALREADY_REGISTERED",
            "El correo electrónico ya está registrado.",
            HttpStatus.CONFLICT.value()
        );
    }
}
