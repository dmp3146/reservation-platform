# ADR-0003: Arquitectura modular y organización del backend

- **Estado:** propuesta
- **Fecha:** 2026-10-20
- **Responsable:** Pendiente de asignación

## Contexto

**Hechos comprobados**

- `backend/src/main/java/com/udea/reservations/platform/` contiene la clase de arranque `ReservationPlatformApplication` y los paquetes `common/dto`, `common/exception` y `common/web`.
- El código Java de producción inspeccionado no contiene módulos de negocio con controladores, servicios, repositorios o modelos JPA.
- La estructura de directorios incluye paquetes por dominio (`users`, `providers`, `catalog`, `resources`, `schedules`, `bookings`, `reports`, `auth`, `audit`, `common`, `config`), pero varios directorios son solo marcadores `.gitkeep`; su existencia no prueba que las capas estén implementadas.
- `.github/copilot-instructions.md` y `docs/architecture/conventions.md` describen un monolito modular y proponen organización por capas. Esos documentos son lineamientos; no se encontró un ADR ni evidencia suficiente para afirmar que el diseño completo esté implementado o haya sido aprobado formalmente.

El problema es separar responsabilidades y facilitar el crecimiento del backend sin confundir una estructura objetivo con componentes ya funcionales. Se requiere una decisión explícita sobre qué organización se espera para las nuevas funcionalidades.

## Alternativas consideradas

Estas alternativas se comparan para esta propuesta; no se afirma que hayan sido evaluadas previamente por el equipo.

1. **Monolito modular organizado por dominio con capas internas.** Mantiene un despliegue backend único y agrupa responsabilidades por dominio.
2. **Organización principalmente por capas globales.** Puede ser simple al inicio, aunque los componentes de un mismo dominio quedan dispersos en paquetes distintos.
3. **Separar dominios en servicios/despliegues independientes.** Aumenta independencia operativa, pero también complejidad de despliegue, comunicación y observabilidad; no hay evidencia de que esos requisitos existan actualmente.

## Decisión

**Decisión propuesta, pendiente de aprobación:** adoptar como dirección para el código nuevo un monolito modular por dominio, usando capas controller, service, repository, model, dto y exception cuando el caso de uso las requiera. Los controladores no contendrán lógica de negocio y las entidades de persistencia no se expondrán directamente por la API. No se afirma que todas las capas o módulos ya existan, ni se obliga a crear clases vacías o capas que no aporten al caso de uso.

La propuesta no convierte la estructura de carpetas actual ni las instrucciones de Copilot en evidencia de implementación terminada.

## Consecuencias

- Positivas:
  - Los límites por dominio hacen más localizables las responsabilidades y cambios.
  - Separar transporte, negocio y persistencia facilita pruebas y evita acoplar entidades JPA al contrato HTTP.
  - Permite comenzar en una sola aplicación sin introducir despliegues distribuidos.
- Negativas / riesgos:
  - Mantener interfaces/implementaciones y capas para toda operación, aunque sean triviales, puede introducir ceremonia sin valor.
  - Los límites entre módulos pueden degradarse si se permiten dependencias circulares o acceso directo a modelos ajenos.
  - La dirección propuesta no define límites operativos ni estrategia de extracción futura.
- Impacto en calidad, seguridad, pruebas y despliegue:
  - Las pruebas deben cubrir reglas en servicios y contratos en controladores cuando existan.
  - Los DTOs deben controlar los datos expuestos; las entidades permanecen en la persistencia.
  - Se conserva un único backend desplegable salvo que una decisión futura justifique separar componentes.

## Trazabilidad

HU relacionadas: Pendiente de definir.

## Pendientes de aprobación

- Si el monolito modular es una decisión formal del equipo o solo una dirección provisional.
- Qué módulos tienen límites y reglas de dependencia obligatorios.
- Si la interfaz de servicio más implementación es obligatoria en todos los casos.
- Qué excepciones se permiten para módulos transversales como `common`, `audit` y `reports`.
- Criterios que podrían justificar cambiar el límite de despliegue en el futuro.
