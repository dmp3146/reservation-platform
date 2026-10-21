# Plataforma de Reservas

Aplicación web para que los clientes encuentren servicios y reserven un horario. Los proveedores pueden publicar sus servicios y disponibilidad, y los responsables pueden consultar reportes de ocupación.

El proyecto está dividido en dos partes:

- `backend/`: API REST hecha con Java y Spring Boot.
- `frontend/`: aplicación web hecha con React y TypeScript.

La API guarda la información en PostgreSQL. La interfaz se comunica con el backend mediante `/api/v1`.

## Qué se puede hacer

### Clientes

- Crear una cuenta e iniciar sesión.
- Explorar proveedores y servicios disponibles.
- Consultar los bloques de disponibilidad publicados para un servicio.
- Crear una reserva y consultar sus detalles.
- Ver las reservas activas y el historial.
- Cancelar una reserva cuando las reglas del sistema lo permiten.

### Proveedores

- Registrarse como proveedor e iniciar sesión.
- Crear y consultar sus servicios.
- Publicar bloques de disponibilidad para sus servicios.
- Retirar un servicio de la oferta si no tiene reservas activas asociadas.

### Responsables

- Consultar reportes de ocupación en un rango de fechas.

Los permisos se comprueban en el backend. Ocultar una opción en la interfaz no sustituye la autorización de la API.

## Tecnologías

- **Backend:** Java 25, Spring Boot, Spring MVC, Spring Security, Spring Data JPA y Maven.
- **Base de datos:** PostgreSQL.
- **Frontend:** React, TypeScript y Vite.
- **Pruebas:** JUnit, Mockito, AssertJ y MockMvc en el backend; Vitest y Testing Library en el frontend.

## Estructura del proyecto

```text
backend/
  src/main/java/com/udea/reservations/platform/
    auth/       Inicio de sesión, sesión HTTP y permisos
    bookings/   Creación, consulta y cancelación de reservas
    providers/  Registro y directorio de proveedores
    reports/    Reportes de ocupación
    schedules/  Bloques de disponibilidad
    services/   Servicios ofrecidos por proveedores
    users/      Registro y datos de usuarios
    common/     Respuestas, errores y manejo común de solicitudes
  src/main/resources/
    db/         Esquema y scripts SQL

frontend/
  src/
    api/        Cliente HTTP y tipos de respuesta
    auth/       Estado de la sesión
    components/ Elementos compartidos de la interfaz
    pages/      Pantallas de la aplicación
```

Los controladores reciben solicitudes HTTP, los servicios aplican las reglas del sistema y los repositorios consultan o guardan datos. La API utiliza DTO para sus respuestas; las entidades de base de datos no se exponen directamente.

## Endpoints principales

Todos los endpoints de negocio usan el prefijo `/api/v1`.

| Función | Rutas |
| --- | --- |
| Cuenta y sesión | `POST /users`, `POST /auth/login`, `GET /auth/me`, `POST /auth/logout`, `GET /auth/csrf` |
| Proveedores | `POST /providers`, `GET /providers`, `GET /providers/{providerId}/services` |
| Servicios | `POST /services`, `GET /providers/me/services`, `GET /services/available`, `DELETE /services/{serviceId}` |
| Disponibilidad | `POST /providers/me/schedules`, `GET /services/{serviceId}/schedules`, `GET /services/{serviceId}/schedules/available` |
| Reservas | `POST /bookings`, `GET /bookings/active`, `GET /bookings/history`, `GET /bookings/{bookingId}`, `PATCH /bookings/{bookingId}/cancel` |
| Reportes | `GET /reports/occupancy` |

Las rutas protegidas requieren una sesión válida y, según la operación, el rol correspondiente. Las respuestas de error siguen el formato común de la API y contienen un `traceId` para facilitar el diagnóstico.

## Ejecutar en local

### Requisitos

- JDK 25.
- PostgreSQL local.
- Node.js 20.19 o superior y npm 10 o superior.

No uses una base de producción para desarrollo o pruebas. No guardes contraseñas ni otros secretos en el repositorio.

### 1. Preparar PostgreSQL

Crea una base local vacía y aplica el esquema inicial:

```powershell
psql -h localhost -U postgres -d reservation_platform -f backend/src/main/resources/db/schema.sql
```

El usuario `postgres` y el nombre de la base son ejemplos; cámbialos por los de tu instalación local.

La aplicación requiere `DB_URL` y `DB_PASSWORD`. Puedes definirlos en la terminal antes de iniciar Spring Boot. Si tu usuario de PostgreSQL es distinto del configurado para la aplicación, también puedes indicar `SPRING_DATASOURCE_USERNAME`:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/reservation_platform"
$env:DB_PASSWORD = "tu contraseña local"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
```

No escribas valores reales de producción en estos ejemplos ni los subas a Git.

### 2. Iniciar el backend

Desde la raíz del repositorio:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

El servidor queda disponible normalmente en `http://localhost:8080`.

### 3. Iniciar el frontend

En otra terminal, desde la raíz:

```powershell
cd frontend
Copy-Item .env.example .env.local
npm install
npm run dev
```

Abre la dirección local que indique Vite, normalmente `http://localhost:5173`. El proxy de desarrollo envía las solicitudes `/api` al backend en `http://localhost:8080`. Para usar otro destino local, configura `VITE_API_PROXY_TARGET` en `frontend/.env.local`.

El frontend envía las cookies de sesión y obtiene el token CSRF del backend para las solicitudes que modifican datos. No es necesario habilitar CORS global para el desarrollo con el proxy.

## Base de datos y cambios de esquema

El esquema inicial se encuentra en [backend/src/main/resources/db/schema.sql](./backend/src/main/resources/db/schema.sql). Las tablas y columnas conservan los nombres del modelo PostgreSQL del proyecto; las clases Java usan nombres en inglés.

El proyecto no configura Flyway ni Liquibase. Los scripts dentro de `db/migrations/` son cambios manuales y no se ejecutan automáticamente al iniciar la aplicación. Antes de aplicar uno, revisa su propósito y confirma que la base sea la correcta. No ejecutes scripts de desarrollo contra Supabase o producción sin autorización y respaldo.

Spring valida el esquema existente al arrancar; no modifica automáticamente las tablas.

## Pruebas y validaciones

Backend, desde `backend/`:

```powershell
.\mvnw.cmd clean verify
```

Frontend, desde `frontend/`:

```powershell
npm test
npm run lint
npm run build
```

Algunas pruebas de integración del backend necesitan una base PostgreSQL de pruebas dedicada y variables de entorno propias. Si no están configuradas, esas pruebas pueden quedar omitidas; una prueba con mocks no verifica la integración con PostgreSQL.

## Alcance actual y consideraciones

- Los proveedores publican bloques concretos de disponibilidad por servicio; esos bloques son los que el cliente consulta para preparar una reserva.
- La API vuelve a validar la disponibilidad al crear la reserva. El resultado que se mostró antes en pantalla no garantiza que el horario siga libre si hubo otra solicitud concurrente.
- El esquema también conserva tablas de agenda y horarios del modelo anterior. No todas las tablas tienen una pantalla de administración en el frontend actual.
- La interfaz usa respuestas de prueba en sus pruebas automatizadas. Para comprobar el flujo completo hay que iniciar el backend contra una base local de pruebas.

## Documentación adicional

- [Guía del frontend](./frontend/README.md): configuración del proxy y uso de la interfaz.
- [Documentación del proyecto](./docs/README.md): convenciones y material de apoyo.
