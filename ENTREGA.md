# Verificacion del backend de entrega 3

Implementacion local nueva sobre el repositorio vacio `Vvcentek/proyecto_v3`.

| Exigencia | Evidencia funcional |
| --- | --- |
| Registro e inicio de sesion | Usuario persistido, hash BCrypt, rechazo de duplicados y login que entrega JWT |
| CRUD de producto | GET, POST, PUT, PATCH y DELETE; escritura exclusiva para administrador |
| Otro flujo de la tienda | Pedidos con total del servidor, descuento de existencias, historial y seguimiento |
| Proteccion JWT | Rechazo de ausencia, manipulacion y vencimiento; roles y propiedad de pedidos |
| Swagger | Interfaz interactiva y esquema Bearer disponibles |

Resultados ejecutados:

- Maven Wrapper `verify`: 26 pruebas correctas, cero fallos y cero errores; JAR generado.
- `node scripts/probar-api.mjs`: 25 comprobaciones HTTP correctas contra el servidor iniciado.
- `node scripts/probar-api.mjs --persistencia`: cliente y pedido conservados tras reiniciar el backend.

Las pruebas incluyen compras concurrentes de la ultima unidad, reversion completa cuando falta stock y cancelacion sin reposicion duplicada. Son comprobaciones automatizadas, no una afirmacion de pruebas manuales realizadas por estudiantes.

Los resultados HTTP estan en `evidencia/prueba-http.json` y `evidencia/persistencia.json`. No contienen tokens ni contrasenas. El resumen de Maven esta en `evidencia/pruebas-integracion.txt`.

## Demostracion

Arranca con perfil `demo`, abre `/swagger-ui/index.html` y sigue README.md: registrar cliente, iniciar sesion, usar Authorize, mostrar 401/403, iniciar sesion como administrador, crear/editar/eliminar producto y crear un pedido como cliente.

Datos ficticios del perfil demo: `admin@losmaestros.cl` / `DemoAdmin123!`.

## Alcance

Se verifico la base H2 persistente. El perfil MySQL esta preparado pero no se probo contra un servidor MySQL. El frontend React anterior no fue modificado: sigue pendiente conectar sus servicios de localStorage con esta API. No hay procesamiento de pagos reales. El flujo de pedidos cubre el servicio adicional solicitado.

El repositorio remoto debe contener estos archivos para que el docente los vea; la ejecucion local por si sola no publica el codigo. La calificacion depende de la revision docente.
