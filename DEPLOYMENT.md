# Despliegue web — OmniGuard Core Java

Consulte el plan maestro en el repositorio central:

[WEB-DEPLOYMENT-PLAN.md](../WEB-DEPLOYMENT-PLAN.md)

## Rol

Core Java expone la API REST, autentica usuarios PASETO, administra activos, alertas, usuarios y configuración de integraciones.

## Variables críticas

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
SPRING_DATA_REDIS_HOST
SPRING_DATA_REDIS_PORT
INTERNAL_API_TOKEN
PASETO_LOCAL_KEY
PASETO_PUBLIC_KEY
PASETO_PRIVATE_KEY
ADMIN_INITIAL_PASSWORD
```

## Producción

- PostgreSQL y Redis no deben publicarse al host.
- Exponer el Core únicamente detrás del proxy y `/api/*`.
- Mantener Actuator 8081 interno.
- Configurar cookies Secure y HTTPS.
- Usar secretos reales y rotar valores conocidos.

## Inicio

```powershell
docker compose up -d --build
```
