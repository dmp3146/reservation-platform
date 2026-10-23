# Frontend de Reserva

Aplicación React + TypeScript + Vite para probar las funcionalidades reales del
backend Spring Boot. Usa cookies de sesión, el endpoint CSRF y los permisos del
servidor; no mantiene una identidad paralela.

## Requisitos

- Node.js 20.19+ (probado con Node 24).
- npm 10+.
- Backend ejecutándose en `http://localhost:8080` y una base local configurada
  mediante las variables requeridas por el backend. No configures aquí una URL
  ni credenciales de una base de producción.

## Inicio local

Desde `frontend/`:

```powershell
Copy-Item .env.example .env.local
npm install
npm run dev
```

Abre la URL que muestra Vite, normalmente `http://localhost:5173`. El proxy de
desarrollo reenvía `/api/*` a `http://localhost:8080`; se puede cambiar el
destino con `VITE_API_PROXY_TARGET`. Esto mantiene las llamadas same-origin
desde el navegador y no requiere habilitar CORS global en Spring.

`VITE_API_BASE_URL` queda vacío para el proxy local. En una instalación
desplegada, el frontend y `/api` deben publicarse bajo el mismo origen o el
servidor debe configurar un origen explícito y limitado; no uses `*` para
solucionar CORS. La variable puede apuntar a una API remota solo cuando el
servidor de esa API haya configurado CORS, credenciales y CSRF de forma segura.

## Flujos disponibles

- Registro de cliente/proveedor, inicio de sesión, restauración de sesión y
  cierre de sesión.
- Directorio paginado de proveedores y servicios disponibles; la reserva se
  construye únicamente con bloques devueltos por el backend. Los horarios que
  ya tienen una reserva activa se muestran atenuados y no se pueden seleccionar.
- Gestión de servicios y bloques de disponibilidad para proveedores.
- Agenda mensual para proveedores con sus reservas activas y detalle por día;
  los datos de clientes no se exponen en esta vista.
- Reservas activas, historial, detalle y cancelación para clientes.
- Reportes de ocupación para usuarios autorizados como responsable.

La disponibilidad por servicio representa bloques declarados y no garantiza
que no exista una reserva concurrente; los conflictos que devuelva el backend
se muestran en la interfaz.

## Comprobaciones

```powershell
npm test
npm run lint
npm run build
```

Las pruebas del frontend usan respuestas controladas y no sustituyen una
verificación de integración con un backend y PostgreSQL locales.
