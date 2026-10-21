# ADR-0001: Seguridad y autenticación

- **Estado:** propuesta
- **Fecha:** 2026-10-20
- **Responsable:** Pendiente de asignación

## Contexto

**Hechos comprobados**

- El código inspeccionado no contiene configuración de Spring Security, flujo de autenticación, autorización, `PasswordEncoder` ni implementación BCrypt.
- `backend/src/main/resources/db/schema.sql` define `usuario.password_hash`, pero no se encontró código de negocio que registre usuarios o codifique/verifique contraseñas.
- `.github/copilot-instructions.md` y `docs/architecture/conventions.md` mencionan BCrypt mediante `PasswordEncoder`, mensajes de login genéricos y la prohibición de registrar contraseñas o tokens. Estas son instrucciones documentadas; por sí solas no prueban que la seguridad esté implementada ni que esas decisiones hayan sido aprobadas formalmente.
- El proyecto usa Java 25 y Spring Boot 4.1.1 según `backend/pom.xml`. No se encontró un mecanismo de identidad que permita decidir entre sesiones, cookies o tokens.

El problema que debe resolverse antes de incorporar autenticación es definir cómo se verifican las credenciales, cómo se representa la identidad autenticada y cómo se autorizan las operaciones protegidas. Crear un codificador aislado no implementa registro, autenticación, autorización, almacenamiento seguro ni persistencia.

## Alternativas consideradas

Estas alternativas se comparan para esta propuesta; no se afirma que hayan sido evaluadas previamente por el equipo.

1. **Adoptar BCrypt con `PasswordEncoder` al implementar un flujo real de registro/autenticación.** La dependencia y las pruebas se incorporan junto con su consumidor y reglas de negocio. Evita mantener configuración sin uso y permite probar el ciclo completo.
2. **Agregar ahora un bean BCrypt preparatorio.** Hace disponible una herramienta antes de existir un flujo que la use, pero agrega una dependencia y configuración inactiva, y no protege por sí sola ningún dato.
3. **Elegir otro mecanismo de autenticación o hash.** No se recomienda ni descarta aquí: requiere requisitos de seguridad, plataforma y operación que aún no están documentados.

## Decisión

**Decisión propuesta, pendiente de aprobación:** posponer la incorporación de BCrypt y de cualquier mecanismo de autenticación hasta que una historia de usuario defina el registro o login y su contexto de seguridad. En esa historia se debe evaluar `PasswordEncoder` con BCrypt como opción de almacenamiento y verificación de contraseñas, y añadir solo la dependencia necesaria; no incorporar `spring-boot-starter-security` salvo que también se aprueben requisitos de filtros, autorización, sesiones, tokens u otras capacidades de seguridad.

Esta propuesta no selecciona sesiones, cookies, JWT, roles ni estrategia de autorización. Tampoco autoriza cambios a dependencias o configuración.

## Consecuencias

- Positivas:
  - La seguridad se implementa en conjunto con el flujo funcional que la necesita.
  - Se pueden probar registro, verificación de contraseña y respuestas de autenticación sin asumir un consumidor inexistente.
  - Se evita presentar un bean PasswordEncoder como si resolviera autenticación o autorización.
- Negativas / riesgos:
  - Hasta que se apruebe e implemente el flujo, no existe autenticación ni protección de endpoints en el backend inspeccionado.
  - La columna `password_hash` existe en el esquema, pero el código actual no demuestra cómo se poblará o validará.
- Impacto en calidad, seguridad, pruebas y despliegue:
  - La futura implementación deberá probar que solo se persisten hashes, que la contraseña original puede verificarse y que una incorrecta se rechaza, sin exponer contraseñas ni hashes en respuestas o logs.
  - La codificación de contraseñas no sustituye autorización, control de acceso, protección CSRF, gestión de sesiones/tokens ni políticas de RLS.
  - La versión de cualquier dependencia nueva debe resolverse desde Spring Boot 4.1.1 salvo justificación y aprobación distintas.

## Trazabilidad

HU relacionadas: Pendiente de definir.

## Pendientes de aprobación

- Mecanismo de autenticación y fuente de identidad.
- Modelo de autorización, roles y reglas de acceso.
- Política de contraseñas, recuperación y bloqueo/rate limiting, si aplica.
- Momento de incorporación de BCrypt y dependencia asociada.
- Responsabilidad del backend frente a RLS y propagación de identidad a PostgreSQL, si aplica.
