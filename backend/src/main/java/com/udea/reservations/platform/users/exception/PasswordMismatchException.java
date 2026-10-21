package com.udea.reservations.platform.users.exception;

import com.udea.reservations.platform.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

public class PasswordMismatchException extends BusinessRuleException {

    public PasswordMismatchException() {
        super(
            "PASSWORD_MISMATCH",
            "Las contraseñas no coinciden.",
            HttpStatus.BAD_REQUEST.value()
        );
    }
}
