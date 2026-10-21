# language: es
@HU-1
Característica: Registrarse en la plataforma
  Como usuario que desea reservar servicios
  Quiero crear una cuenta en la plataforma
  Para poder realizar reservas en una historia posterior

  @positivo
  Escenario: Registro exitoso con datos válidos
    Dado que no existe una cuenta con el correo "ana@example.com"
    Cuando me registro con nombre "Ana Pérez", correo "ANA@example.com", contraseña "clave-segura-1" y confirmación "clave-segura-1"
    Entonces la respuesta tiene el código HTTP 201
    Y la cuenta queda guardada con el correo "ana@example.com" y el rol "cliente"
    Y la contraseña queda almacenada como hash BCrypt
    Y la respuesta no contiene la contraseña ni el hash

  @excepcional
  Escenario: Correo previamente registrado
    Dado que existe una cuenta con el correo "ana@example.com"
    Cuando me registro con nombre "Ana Pérez", correo "ANA@example.com", contraseña "clave-segura-1" y confirmación "clave-segura-1"
    Entonces la respuesta tiene el código HTTP 409
    Y el código de error es "EMAIL_ALREADY_REGISTERED"
    Y no se crea otra cuenta

  @excepcional
  Escenario: Campos obligatorios vacíos
    Cuando me registro con nombre "", correo "", contraseña "" y confirmación ""
    Entonces la respuesta tiene el código HTTP 400
    Y el código de error es "VALIDATION_ERROR"
    Y la respuesta identifica los campos inválidos

  @excepcional
  Escenario: Correo inválido
    Cuando me registro con nombre "Ana Pérez", correo "no-es-un-correo", contraseña "clave-segura-1" y confirmación "clave-segura-1"
    Entonces la respuesta tiene el código HTTP 400
    Y el código de error es "VALIDATION_ERROR"
    Y la respuesta identifica el campo "email"

  @excepcional
  Escenario: Contraseña demasiado corta
    Cuando me registro con nombre "Ana Pérez", correo "ana@example.com", contraseña "corta1" y confirmación "corta1"
    Entonces la respuesta tiene el código HTTP 400
    Y el código de error es "VALIDATION_ERROR"
    Y la respuesta identifica el campo "password"

  @excepcional
  Escenario: Contraseña superior al límite de bytes UTF-8
    Cuando me registro con una contraseña formada por 37 veces el carácter "ñ"
    Entonces la respuesta tiene el código HTTP 400
    Y el código de error es "VALIDATION_ERROR"
    Y no se persiste ninguna cuenta

  @excepcional
  Escenario: Confirmación de contraseña diferente
    Cuando me registro con nombre "Ana Pérez", correo "ana@example.com", contraseña "clave-segura-1" y confirmación "otra-clave-2"
    Entonces la respuesta tiene el código HTTP 400
    Y el código de error es "PASSWORD_MISMATCH"
    Y no se persiste ninguna cuenta
