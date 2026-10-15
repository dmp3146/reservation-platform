# Instrucciones permanentes para Copilot — reservation-platform

Proyecto: monolito modular Spring Boot (Java 25 LTS, Maven, JPA, PostgreSQL). Paquete base: `com.udea.reservations.platform`.
El usuario es estudiante: explica brevemente (en español) cada clase que generes.

## Reglas que SIEMPRE se cumplen
1. Código en inglés: clases PascalCase, métodos/variables camelCase. Mensajes al usuario final en español.
2. Un módulo por dominio: users, providers, catalog, resources, schedules, bookings, reports, auth, audit, common, config.
   Cada módulo: `controller`, `service` (interfaz + `Impl`), `repository`, `model`, `dto`, `exception`.
3. Controller sin lógica de negocio. Entidades JPA nunca salen por la API: usa DTOs (records) con Bean Validation.
4. Endpoints: `/api/v1/<plural-resource>`; JSON en camelCase.
5. Errores: SOLO mediante `ApiErrorResponse {errorCode, message, details, traceId}`. Lanza `BusinessRuleException` (o subclases). El único manejador es `common/web/GlobalExceptionHandler`; no crear `@ExceptionHandler` en controladores.
6. Operaciones de escritura responden `ApiSuccessResponse<T> {message, data}`.
7. La base de datos es el contrato: tablas y columnas en español (`usuario`, `nombre_negocio`, …). Mapea con `@Table`/`@Column`. `ddl-auto=validate`. No cambies el esquema sin pedirlo.
8. Seguridad: contraseñas con `PasswordEncoder` (BCrypt); nunca en logs ni respuestas; mensajes de login genéricos; sin secretos en el código.
9. Logs con SLF4J (`@Slf4j`), incluir `traceId`, sin datos personales.
10. Pruebas: JUnit 5 + Mockito + AssertJ, patrón AAA, un test por escenario Gherkin; MockMvc `standaloneSetup` con `GlobalExceptionHandler`. Cobertura ≥ 65 %.
11. No agregues dependencias, no renombres ni muevas archivos, no formatees archivos que no cambies.
12. Si el requerimiento es ambiguo, pregunta (máximo 3 preguntas) antes de implementar.
