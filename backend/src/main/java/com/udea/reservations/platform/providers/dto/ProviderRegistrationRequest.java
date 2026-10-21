package com.udea.reservations.platform.providers.dto;

import com.udea.reservations.platform.users.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record ProviderRegistrationRequest(
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
    String confirmPassword,

    @NotBlank(message = "El nombre del negocio es obligatorio.")
    @Size(
        max = 150,
        message = "El nombre del negocio no puede superar los 150 caracteres."
    )
    String businessName,

    @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres.")
    String phone
) {

    public ProviderRegistrationRequest {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
