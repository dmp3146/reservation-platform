package com.udea.reservations.platform.auth.exception;

import com.udea.reservations.platform.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends BusinessRuleException {

    public InvalidCredentialsException() {
        super(
            "INVALID_CREDENTIALS",
            "El correo electrónico o la contraseña son incorrectos.",
            HttpStatus.UNAUTHORIZED.value()
        );
    }
}
