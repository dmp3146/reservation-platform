package com.udea.reservations.platform.users.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({
    ElementType.ANNOTATION_TYPE,
    ElementType.FIELD,
    ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {

    String message() default "El valor supera el máximo de bytes UTF-8 permitido.";

    int value();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
