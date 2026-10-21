# ADR-0002: Manejo uniforme de errores HTTP

- **Estado:** propuesta
- **Fecha:** 2026-10-20
- **Responsable:** Pendiente de asignación

## Contexto

**Hechos comprobados**

- `backend/src/main/java/com/udea/reservations/platform/common/dto/ApiErrorResponse.java` define un record con `errorCode`, `message`, `details` y `traceId`; su fábrica `of` asigna un UUID nuevo como `traceId`.
- `backend/src/main/java/com/udea/reservations/platform/common/exception/BusinessRuleException.java` es una excepción de ejecución que contiene `errorCode` y `HttpStatus`, obtenido de un entero recibido en el constructor.
- `backend/src/main/java/com/udea/reservations/platform/common/web/GlobalExceptionHandler.java` usa `@RestControllerAdvice` y maneja excepciones de negocio, validación (`400`), JSON malformado (`400`), método no soportado (`405`), recurso no encontrado (`404`) y excepciones no previstas (`500`).
- En el manejador, los errores de validación se convierten en detalles de campo; el error inesperado devuelve un mensaje genérico y registra la excepción junto con el `traceId`.
- `GlobalExceptionHandlerTest` contiene cinco escenarios relacionados con esas respuestas.
- No se encontró configuración de Spring Security ni manejadores específicos para errores de autenticación/autorización. Tampoco hay código de persistencia funcional del cual observar el contrato de errores de base de datos.

El problema abordado es ofrecer a los clientes HTTP una forma consistente de interpretar errores, sin revelar detalles internos. La implementación actual cubre varios errores MVC, pero no demuestra un contrato completo para todos los errores posibles.

## Alternativas consideradas

Estas alternativas se comparan para esta propuesta; no se afirma que hayan sido evaluadas previamente por el equipo.

1. **Conservar y completar el formato `ApiErrorResponse` mediante el advice global.** Mantiene una forma común y concentra el mapeo de excepciones.
2. **Permitir formatos de error distintos por controlador o módulo.** Da flexibilidad local, pero los clientes deben interpretar respuestas heterogéneas.
3. **Adoptar un formato estándar diferente.** Podría ser apropiado si existe una exigencia de API externa, pero no se encontró tal requisito en los archivos inspeccionados.

## Decisión

**Decisión propuesta, pendiente de aprobación:** conservar `ApiErrorResponse` como formato común de la API y mantener el manejo transversal en `GlobalExceptionHandler`; los controladores no deberían crear formatos ad hoc. Ampliar el mapeo únicamente cuando aparezcan nuevos casos funcionales, como autenticación/autorización o persistencia, y definir sus códigos/status sin filtrar datos sensibles.

La propuesta no afirma que cada respuesta ya use este formato: el código existente cubre los casos enumerados en Contexto y no se observan todavía respuestas de endpoints funcionales.

## Consecuencias

- Positivas:
  - Los consumidores pueden procesar campos de error coherentes para los casos cubiertos.
  - La traducción de excepciones se mantiene centralizada.
  - La respuesta genérica de error inesperado evita enviar el mensaje de excepción interno al cliente.
- Negativas / riesgos:
  - `traceId` se genera por respuesta y no se demuestra que correlacione logs, filtros, trazas distribuidas o peticiones entrantes.
  - No se han definido códigos ni comportamiento para seguridad, acceso denegado, errores de base de datos u otros casos aún no implementados.
  - El manejador de excepciones genérico requiere cuidado para que errores futuros no se conviertan en respuestas incorrectas ni queden ocultos.
- Impacto en calidad, seguridad, pruebas y despliegue:
  - Mantener los escenarios existentes de `GlobalExceptionHandlerTest` y agregar pruebas al ampliar el contrato.
  - Las futuras respuestas 401/403 deben mantener el formato si esa decisión se aprueba, sin crear excepciones a nivel de controlador.
  - Las respuestas y logs no deben incluir contraseñas, tokens, datos personales innecesarios ni detalles internos de persistencia.

## Trazabilidad

HU relacionadas: Pendiente de definir.

## Pendientes de aprobación

- Si el contrato de cuatro campos es el estándar definitivo para todos los errores de API.
- Si `traceId` debe propagarse desde una petición/tracing existente o seguir generándose por respuesta.
- Códigos y mensajes para errores de autenticación/autorización y persistencia.
- Si se desea adoptar un estándar externo de errores en una futura versión.
