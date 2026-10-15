# Reservation Platform

Proyecto académico para desarrollar una plataforma de reservas de servicios. La idea es que permita gestionar usuarios, proveedores, recursos, horarios y reservas.

## Estado del proyecto

Por ahora, el repositorio contiene la estructura inicial y la configuración Maven del backend. Aún no hay clases Java, endpoints ni pruebas de la aplicación, así que el servicio todavía no se puede iniciar ni conectar a una base de datos.

## Tecnologías previstas

- Java 25 (LTS) y Spring Boot
- Maven
- Spring Data JPA y PostgreSQL
- JUnit 5

La configuración de dependencias está en `backend/pom.xml`.

## Carpetas

| Ruta | Contenido actual |
|---|---|
| `backend/` | Proyecto Maven y carpetas preparadas para los módulos del backend. |
| `frontend/` | Carpeta vacía; la interfaz aún está por definir. |
| `deploy/` | Carpeta reservada para archivos de despliegue. |
| `scripts/` | Carpeta reservada para utilidades. |
| `.github/` | Instrucciones para Copilot y plantilla de pull request. No hay un workflow de GitHub Actions configurado. |
| `docs/` | Notas y plantillas de trabajo. Está en `.gitignore` para mantenerla local; si ya se había subido a GitHub, ignorarla no la quita por sí solo. |

## Backend

El backend requiere un JDK 25 LTS configurado en `JAVA_HOME`.

Cuando se agregue la clase principal de Spring Boot y la configuración que necesita la aplicación, se podrá ejecutar desde PowerShell:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Para ejecutar las pruebas cuando estén disponibles:

```powershell
.\mvnw.cmd test
```

No subas contraseñas, tokens ni archivos `.env` al repositorio.
