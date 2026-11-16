# Backend de Ferreteria Los Maestros

API REST de entrega 3 con Java 17, Spring Boot 3.5, Spring Security, JWT, JPA y Swagger. El repositorio recibido estaba vacio; esta implementacion es nueva y esta separada del frontend React.

## Requisitos de la entrega

| Requisito | Implementacion |
| --- | --- |
| Registro e inicio de sesion | `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, contrasenas BCrypt y correos unicos |
| CRUD de productos | Listar, consultar, crear, reemplazar, editar parcialmente y eliminar del catalogo |
| Otro flujo de tienda | Pedidos con retiro o despacho, calculo del total, descuento de stock, historial y estados |
| Creacion y validacion de JWT | Firma HS256, vencimiento, emisor, usuario vigente y permisos por rol |
| Swagger | `/swagger-ui/index.html`, esquema Bearer y documento `/v3/api-docs` |

## Ejecutar en Windows

Necesitas un JDK 17 o posterior. El Maven Wrapper esta incluido; no es necesario instalar Maven aparte.

Abre esta carpeta en IntelliJ o VS Code y en una terminal PowerShell ejecuta:

```powershell
# Solo si JAVA_HOME no apunta a tu JDK:
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

La primera ejecucion descarga dependencias. En Linux/macOS: `sh mvnw spring-boot:run -Dspring-boot.run.profiles=demo`.

Con el servidor iniciado:

- Swagger: <http://localhost:8088/swagger-ui/index.html>
- Estado: <http://localhost:8088/api/v1/salud>
- Catalogo: <http://localhost:8088/api/v1/productos>
- OpenAPI: <http://localhost:8088/v3/api-docs>

Para cambiar el puerto, define `PORT` antes de ejecutar. Si ejecutas el JAR generado: `java -jar target/ferreteria-api-1.0.0.jar --spring.profiles.active=demo`.

## Datos de demostracion

Solo el perfil `demo` crea cinco productos y esta cuenta ficticia si no existe:

```text
Correo: admin@losmaestros.cl
Contrasena: DemoAdmin123!
Rol: ADMIN
```

Puedes personalizarla definiendo `DEMO_ADMIN_PASSWORD` antes del primer inicio. El arranque no reemplaza las cuentas ni las contrasenas ya guardadas. Sin perfil `demo` no se crean administradores automaticamente. No actives `demo` junto con `prod`.

## Como demostrar los cinco puntos en Swagger

1. En Autenticacion, abre `POST /api/v1/auth/register`, pulsa Try it out y registra un cliente con este cuerpo:

```json
{"nombre":"Ana","apellidos":"Prueba","correo":"ana@gmail.com","password":"Cliente123!"}
```

2. Abre `POST /api/v1/auth/login` y envia correo y password. Copia el valor `token` de la respuesta.
3. Pulsa Authorize en Swagger y pega solo ese token. No agregues comillas ni el prefijo Bearer: Swagger lo agrega.
4. Ejecuta `GET /api/v1/auth/me` y muestra los datos del cliente sin contrasena.
5. Intenta crear un producto como cliente. Debe responder 403.
6. Inicia sesion como administrador demo y reemplaza el token en Authorize. Crea un producto, copia su `id`, consultalo y cambia solo el precio con PATCH. Los otros campos deben conservarse.
7. Vuelve al token del cliente. Crea un pedido usando un id del catalogo y una cantidad disponible:

```json
{"tipoEntrega":"RETIRO","items":[{"productoId":"REEMPLAZAR_POR_UUID_DEL_PRODUCTO","cantidad":2}]}
```

8. Consulta el producto: el stock debe disminuir. El total del pedido se calcula con el precio del servidor; el cliente no lo envia.
9. Prueba pedir mas unidades que el stock. Debe responder 409 sin crear pedido ni alterar inventario.
10. Consulta `GET /api/v1/pedidos`. Un cliente solo ve sus pedidos. ADMIN y VENDEDOR pueden consultar todos.
11. Como administrador, cambia el estado del pedido a PREPARACION y luego CANCELADO. El stock se repone una sola vez.
12. En Authorize, elimina la autorizacion y consulta pedidos. Debe responder 401. Un token manipulado o expirado tambien se rechaza.
13. Elimina el producto como administrador y comprueba que ya no esta en el catalogo. Se conserva internamente para no romper pedidos anteriores.

`requests.http` incluye ejemplos para una extension REST Client o para copiar los cuerpos en Postman.

## Rutas y permisos

| Metodo y ruta | Permiso |
| --- | --- |
| `POST /api/v1/auth/register` | Publico; siempre crea CLIENTE |
| `POST /api/v1/auth/login` | Publico |
| `GET /api/v1/auth/me` | JWT valido |
| `GET /api/v1/productos?buscar=texto` | Publico |
| `GET /api/v1/productos/{id}` | Publico |
| `POST /api/v1/productos` | ADMIN |
| `PUT /api/v1/productos/{id}` | ADMIN; datos completos |
| `PATCH /api/v1/productos/{id}` | ADMIN; campos parciales |
| `DELETE /api/v1/productos/{id}` | ADMIN |
| `POST /api/v1/pedidos` | JWT valido |
| `GET /api/v1/pedidos` | Propios para CLIENTE; todos para personal |
| `GET /api/v1/pedidos/{id}` | Propietario o personal |
| `PATCH /api/v1/pedidos/{id}/estado` | ADMIN o VENDEDOR |

Estados: PENDIENTE -> PREPARACION -> ENTREGADO para retiro; PENDIENTE -> PREPARACION -> DESPACHADO -> ENTREGADO para despacho. Se permite cancelar desde PENDIENTE o PREPARACION. Repetir el mismo estado no produce efectos duplicados.

## Persistencia, JWT y conexion de React

Por defecto se usa una base H2 real en archivos `data/ferreteria.mv.db`, no localStorage. Los datos sobreviven al reinicio. No se versiona ese directorio ni se habilita la consola H2.

La duracion del JWT es 60 minutos. Si no defines `JWT_SECRET`, el perfil local crea una clave aleatoria temporal y los tokens anteriores dejan de ser validos al reiniciar; puedes volver a iniciar sesion. En produccion la clave es obligatoria. Debe ser Base64 de al menos 32 bytes aleatorios. Nunca publiques una clave real en Git.

La API admite CORS desde los puertos locales 5173 y 5178. Ajusta `CORS_ORIGINS` si tu React utiliza otro origen. Las peticiones autenticadas llevan `Authorization: Bearer <token>`.

El frontend anterior sigue usando sus servicios de localStorage: aun no se cambiaron por llamadas HTTP. Para conectarlo, los servicios React deben apuntar a esta API, guardar el token del login y enviarlo en las operaciones protegidas. Las contrasenas del backend tienen un minimo de 8 caracteres; los ids son UUID generados por la base de datos. Las imagenes son rutas/URLs devueltas como texto; este backend no contiene ni aloja las fotos del frontend.

## MySQL opcional

El perfil `prod` permite usar una base MySQL existente. Configura estas variables con tus propios datos:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'prod'
$env:DB_URL = 'jdbc:mysql://localhost:3306/ferreteria'
$env:DB_USERNAME = 'usuario_de_la_app'
$env:DB_PASSWORD = 'TU_CONTRASENA'
$env:JWT_SECRET = 'TU_CLAVE_BASE64_DE_32_BYTES_O_MAS'
.\mvnw.cmd spring-boot:run
```

No se requieren esos datos para demostrar la entrega con H2. El perfil MySQL debe comprobarse en el entorno donde se disponga de esa base; las pruebas automatizadas de este proyecto usan H2. `ddl-auto=update` facilita la entrega academica; una puesta en produccion requiere migraciones de esquema, HTTPS y configuracion operativa.

## Compilar y probar

```powershell
.\mvnw.cmd verify
```

Las pruebas levantan Spring y una H2 aislada en memoria. Comprueban autenticacion, permisos, CRUD, datos invalidos, JWT alterado/expirado, pedidos ajenos, transacciones, cancelaciones y compras concurrentes.

Con el servidor demo iniciado, tambien puedes ejecutar estas comprobaciones HTTP reales con Node.js 18 o posterior:

```powershell
node scripts/probar-api.mjs
```

El resultado se guarda en `evidencia/prueba-http.json` sin JWT ni contrasenas. Si despues reinicias el servidor, puedes verificar persistencia con `node scripts/probar-api.mjs --persistencia`.

## Organizacion del codigo

- `auth`: usuarios, registro, login y generacion del token.
- `productos`: entidad, validaciones, CRUD y acceso al inventario.
- `pedidos`: compras, historial, seguimiento y actualizacion transaccional de stock.
- `config`: seguridad, JWT, CORS, Swagger y datos de demostracion.
- `common`: respuestas de error JSON y estado del servicio.

Los comentarios cortos en las funciones explican las decisiones que pueden preguntarse en una exposicion: rol de cliente fijo al registrarse, precios del servidor, bloqueo del inventario y reposicion al cancelar.

## Referencias tecnicas

- [Requisitos de Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- [Validacion JWT de Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Compatibilidad de springdoc con Spring Boot](https://springdoc.org/v2/)

El nombre del commit de entrega se acuerda con el estudiante antes de publicar los cambios.

## Si Java falla al abrir sockets en Windows

Si el arranque muestra `Unable to establish loopback connection` y `Invalid argument: connect`, prueba una carpeta temporal existente con una ruta corta. Este entorno de Windows requirio ese ajuste para iniciar Tomcat; no fue necesario cambiar el codigo de la API.

```powershell
java "-Djdk.net.unixdomain.tmpdir=C:\ruta\corta\existente" -jar target/ferreteria-api-1.0.0.jar --spring.profiles.active=demo
```

Java limita la longitud de estas direcciones locales. Referencia: [propiedad de directorio temporal de sockets](https://docs.oracle.com/en/java/javase/16/core/networking-properties.html).
