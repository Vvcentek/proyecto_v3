import assert from 'node:assert/strict'
import fs from 'node:fs/promises'

const base = process.env.API_URL || 'http://127.0.0.1:8088'
const comprobaciones = []
// Espera el arranque de Spring antes de comenzar las peticiones de prueba.
let disponible = false
for (let intento = 0; intento < 45; intento++) {
  try { disponible = (await fetch(base + '/api/v1/salud', { signal: AbortSignal.timeout(1500) })).ok } catch { /* Spring puede estar iniciando. */ }
  if (disponible) break
  await new Promise((resolve) => setTimeout(resolve, 1000))
}
assert.ok(disponible, 'El backend no responde. Inicialo con el perfil demo antes de probar.')
async function llamar(nombre, ruta, { method = 'GET', token, body, status = 200 } = {}) {
  const response = await fetch(base + ruta, {
    method,
    headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
    signal: AbortSignal.timeout(15000),
  })
  const texto = await response.text()
  assert.equal(response.status, status, `${nombre}: HTTP ${response.status}; ${texto.slice(0, 200)}`)
  comprobaciones.push({ nombre, metodo: method, ruta, estado: response.status, correcto: true })
  return response.headers.get('content-type')?.includes('application/json') && texto ? JSON.parse(texto) : texto
}

if (process.argv.includes('--persistencia')) {
  const anterior = JSON.parse(await fs.readFile('evidencia/prueba-http.json', 'utf8'))
  const sesion = await llamar('Login del cliente despues de reiniciar', '/api/v1/auth/login', { method: 'POST', body: { correo: anterior.correoPrueba, password: 'Cliente123!' } })
  const pedido = await llamar('Pedido conservado despues de reiniciar', `/api/v1/pedidos/${anterior.pedidoId}`, { token: sesion.token })
  assert.equal(pedido.estado, 'CANCELADO')
  assert.equal(pedido.total, 19980)
  await fs.writeFile('evidencia/persistencia.json', JSON.stringify({ correcto: true, comprobaciones }, null, 2))
  console.log('Persistencia comprobada: cliente y pedido siguen disponibles despues de reiniciar.')
  process.exit(0)
}

const correo = `cliente.${Date.now()}@gmail.com`
await llamar('Servidor disponible', '/api/v1/salud')
const swagger = await llamar('Swagger y JWT documentados', '/v3/api-docs')
assert.equal(swagger.components.securitySchemes.bearerAuth.scheme, 'bearer')
await llamar('Swagger UI accesible', '/swagger-ui/index.html')
await llamar('Pedidos sin JWT rechazados', '/api/v1/pedidos', { status: 401 })
const registro = { nombre: 'Cliente', apellidos: 'Prueba HTTP', correo, password: 'Cliente123!' }
await llamar('Registro de cliente', '/api/v1/auth/register', { method: 'POST', body: registro, status: 201 })
await llamar('Correo duplicado rechazado', '/api/v1/auth/register', { method: 'POST', body: { ...registro, correo: correo.toUpperCase() }, status: 409 })
await llamar('Credenciales incorrectas rechazadas', '/api/v1/auth/login', { method: 'POST', body: { correo, password: 'Incorrecta123' }, status: 401 })
const cliente = await llamar('Login de cliente', '/api/v1/auth/login', { method: 'POST', body: { correo, password: registro.password } })
await llamar('JWT permite consultar perfil', '/api/v1/auth/me', { token: cliente.token })
await llamar('JWT invalido rechazado', '/api/v1/auth/me', { token: 'no-es-un-jwt', status: 401 })
const admin = await llamar('Login de administrador demo', '/api/v1/auth/login', { method: 'POST', body: { correo: 'admin@losmaestros.cl', password: process.env.DEMO_ADMIN_PASSWORD || 'DemoAdmin123!' } })
const producto = await llamar('Crear producto como administrador', '/api/v1/productos', { method: 'POST', token: admin.token, status: 201,
  body: { nombre: 'Martillo de prueba HTTP', categoria: 'Herramientas', precio: 8990, stock: 3, descripcion: 'Datos ficticios de verificacion', imagen: '/img/productos/martillo_img.jpg' } })
await llamar('Consultar producto', `/api/v1/productos/${producto.id}`)
await llamar('Cliente sin permiso de edicion', `/api/v1/productos/${producto.id}`, { method: 'PATCH', token: cliente.token, body: { stock: 100 }, status: 403 })
const editado = await llamar('Actualizar precio sin perder stock', `/api/v1/productos/${producto.id}`, { method: 'PATCH', token: admin.token, body: { precio: 9990 } })
assert.equal(editado.stock, 3)
const pedido = await llamar('Crear pedido con total del servidor', '/api/v1/pedidos', { method: 'POST', token: cliente.token, status: 201,
  body: { tipoEntrega: 'RETIRO', items: [{ productoId: producto.id, cantidad: 2 }] } })
assert.equal(pedido.total, 19980)
const inventario = await llamar('Stock descontado', `/api/v1/productos/${producto.id}`)
assert.equal(inventario.stock, 1)
await llamar('Pedido sin stock rechazado', '/api/v1/pedidos', { method: 'POST', token: cliente.token, status: 409,
  body: { tipoEntrega: 'RETIRO', items: [{ productoId: producto.id, cantidad: 2 }] } })
await llamar('Consultar pedido propio', `/api/v1/pedidos/${pedido.id}`, { token: cliente.token })
await llamar('Consultar historial', '/api/v1/pedidos', { token: cliente.token })
await llamar('Preparar pedido', `/api/v1/pedidos/${pedido.id}/estado`, { method: 'PATCH', token: admin.token, body: { estado: 'PREPARACION' } })
await llamar('Cancelar pedido', `/api/v1/pedidos/${pedido.id}/estado`, { method: 'PATCH', token: admin.token, body: { estado: 'CANCELADO' } })
const repuesto = await llamar('Inventario repuesto al cancelar', `/api/v1/productos/${producto.id}`)
assert.equal(repuesto.stock, 3)
await llamar('Eliminar producto', `/api/v1/productos/${producto.id}`, { method: 'DELETE', token: admin.token, status: 204 })
await llamar('Producto eliminado ya no es visible', `/api/v1/productos/${producto.id}`, { status: 404 })
await fs.mkdir('evidencia', { recursive: true })
// No se guardan JWT ni contrasenas en el reporte.
await fs.writeFile('evidencia/prueba-http.json', JSON.stringify({ correcto: true, correoPrueba: correo, pedidoId: pedido.id, comprobaciones }, null, 2))
console.log(`${comprobaciones.length} comprobaciones HTTP correctas. No se guardaron tokens en el reporte.`)
