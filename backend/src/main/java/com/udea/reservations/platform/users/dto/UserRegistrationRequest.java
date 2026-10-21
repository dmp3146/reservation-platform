package com.udea.reservations.platform.users.dto;

import com.udea.reservations.platform.users.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record UserRegistrationRequest(
    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres.")
    String name,

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "El correo electrónico no es válido.")
    @Size(
        max = 150,
        message = "El correo electrónico no puede superar los 150 caracteres."
    )
    String email,

    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.")
    @MaxUtf8Bytes(
        value = 72,
        message = "La contraseña no puede superar los 72 bytes UTF-8."
    )
    String password,

    @NotBlank(message = "La confirmación de contraseña es obligatoria.")
    String confirmPassword
) {

    public UserRegistrationRequest {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
