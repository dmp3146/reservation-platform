# ADR-0001: Seguridad y autenticación

- **Estado:** aceptada
- **Fecha:** 2026-10-21
- **Responsable:** Pendiente de asignación

## Contexto

El backend ya implementa autenticación mediante sesiones HTTP de Spring
Security. El inicio de sesión verifica las credenciales con un
`AuthenticationManager` y carga al usuario desde PostgreSQL mediante
`DatabaseUserDetailsService`. El `SecurityContext` se persiste en sesión y
`UserPrincipal` proporciona la identidad autenticada a los controladores.

Las operaciones mutables conservan protección CSRF. La cookie de sesión se
configura como HttpOnly, Secure y SameSite=Lax. Las respuestas de seguridad
usan el formato estándar de error de la API.

No hay un mecanismo de identidad de producción basado en `X-User-Id`, un
`HandlerInterceptor` de identidad ni una anotación `@RequiresRole`. El
encabezado `X-User-Id` aparece en pruebas para comprobar que no permite
suplantar la identidad autenticada. Los permisos se aplican mediante
autoridades de Spring Security.

Los roles persistidos son `cliente`, `proveedor` y `responsable`.
`UserPrincipal` los convierte en `ROLE_CLIENTE`, `ROLE_PROVEEDOR` y
`ROLE_RESPONSABLE`. Además, las cuentas `responsable` reciben
`ROLE_MANAGER` para autorizar los reportes.

JWT no está implementado. Adoptarlo reemplazaría o ampliaría el contrato de
autenticación, afectaría al login, a los clientes y a la protección CSRF, y
requeriría soporte criptográfico específico. No se debe deducir su adopción
del diseño original de identidad interina.

## Decisión

- Mantener la autenticación por sesión HTTP y la protección CSRF existentes.
- Obtener la identidad exclusivamente del `UserPrincipal` autenticado; no
  aceptar `X-User-Id` como fuente de identidad.
- No introducir JWT ni un segundo mecanismo de autenticación en paralelo hasta
  que se apruebe explícitamente un nuevo contrato y su estrategia de transición.
- Considerar sustituida la tarea de identidad interina con `X-User-Id`: la
  autenticación de sesión ya cubre ese propósito de forma más segura y está
  integrada con la autorización existente.

## Consecuencias

- Los endpoints protegidos conservan las reglas de autorización por autoridad.
- Las pruebas de seguridad deben verificar que la sesión autenticada es la
  fuente de identidad y que una cabecera manipulada no concede acceso.
- Cualquier adopción futura de JWT deberá especificar la emisión del token, su
  formato de transporte, firma, expiración, claims, claves de configuración,
  tratamiento de CSRF y transición de clientes; no deberá confiar en claims no
  validados ni mantener mecanismos alternativos accidentalmente.
- La configuración local describe RLS en el esquema, pero no confirma las
  políticas, grants ni el rol de conexión de una base remota. Esta decisión no
  modifica ni verifica la configuración de Supabase.

## Trazabilidad

- HU 6: inicio de sesión con sesión HTTP.
- T1 de identidad interina: sustituida por la autenticación existente.
- T2 de JWT: diferida; requiere una decisión de contrato posterior.
