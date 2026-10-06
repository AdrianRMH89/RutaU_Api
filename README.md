# RutaU API – Carpooling Universitario

Web Services (backend) de **RutaU**, la plataforma de carpooling para estudiantes universitarios.
Proyecto del curso 1ACC0236 Ingeniería de Software – UPC – Grupo 5.

## Tecnologías
- Java 21 + Spring Boot 4 (API REST)
- Spring Data JPA + PostgreSQL (persistencia)
- Spring Security + JWT (autenticación y autorización por roles `USER` / `ADMIN`)
- MapStruct, Lombok, Jakarta Validation
- springdoc-openapi (Swagger UI)
- Docker / Docker Compose
- i18n: español latinoamericano (`es-419`, por defecto) e inglés (`en-US`)

## Cómo ejecutar

### Opción 1: con Docker (recomendado)
```bash
docker compose up --build
```
Levanta PostgreSQL y la API. La API queda en `http://localhost:8081`.

### Opción 2: local (IntelliJ)
1. Crear la base de datos `RutaU_DB` en PostgreSQL (usuario `postgres`).
2. Revisar usuario y contraseña en `src/main/resources/application.yaml`.
3. Ejecutar `RutauApiApplication`.

## Documentación de la API
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI (JSON): `http://localhost:8081/v3/api-docs`
- Colecciones de Postman: carpeta `postman/` (Bloques 1 a 4)

## Autenticación
1. `POST /api/auth/register` (correo institucional `.edu.pe`) y `POST /api/auth/login`.
2. Enviar el token en cada petición: `Authorization: Bearer <token>`.
3. Cuenta de administrador creada al iniciar: `admin@rutau.edu.pe` / `admin12345`
   (se cambia con las variables `ADMIN_EMAIL` y `ADMIN_PASSWORD`).

## Idioma de las respuestas (i18n)
Enviar el encabezado `Accept-Language: en-US` para recibir los mensajes en inglés.
Sin encabezado (o con `es-419`) los mensajes se devuelven en español.

## Módulos (User Stories US01–US20)
| Módulo | Endpoints principales |
|---|---|
| Autenticación | `/api/auth/register`, `/api/auth/login`, `/api/users/me` |
| Vehículos | `/api/vehicles`, `/api/vehicles/me` |
| Viajes | `/api/trips`, `/api/trips/search`, `/api/trips/{id}/stops`, `/api/trips/{id}/complete`, `/api/trips/{id}/cancel` |
| Solicitudes de asiento | `/api/seat-requests`, `/api/seat-requests/{id}/accept`, `/reject`, `/cancel` |
| Calificaciones y perfil | `/api/ratings`, `/api/ratings/me`, `/api/users/{id}/profile` |
| Historial | `/api/history/trips` |
| Estadísticas | `/api/stats/schedule-comparison` |
| Notificaciones | `/api/notifications/me`, `/api/notifications/{id}/read` |
| Reportes de usuarios | `/api/reports` |
| Administración (ADMIN) | `/api/admin/reports/demand`, `/api/admin/reports/demand/export`, `/api/admin/moderation/**` |

## Flujo de trabajo (GitFlow)
- `main`: versiones publicadas (SemVer, por ejemplo `v1.0.0`).
- `develop`: integración del sprint.
- `feature/USxx-descripcion`: una rama por User Story, integrada a `develop` mediante Pull Request.
- Mensajes de commit con Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`...).
