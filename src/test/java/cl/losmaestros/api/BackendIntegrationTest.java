package cl.losmaestros.api;

import cl.losmaestros.api.auth.*;
import cl.losmaestros.api.pedidos.*;
import cl.losmaestros.api.productos.*;
import com.fasterxml.jackson.databind.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class BackendIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProductoRepository productos;
    @Autowired PedidoRepository pedidos;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder encoder;
    String admin, cliente, vendedor;
    UUID clienteId;

    @BeforeEach void preparar() throws Exception {
        pedidos.deleteAll(); productos.deleteAll(); usuarios.deleteAll();
        crearUsuario("admin@gmail.com", Rol.ADMIN);
        clienteId = crearUsuario("cliente@gmail.com", Rol.CLIENTE).getId();
        crearUsuario("vendedor@gmail.com", Rol.VENDEDOR);
        admin = login("admin@gmail.com"); cliente = login("cliente@gmail.com"); vendedor = login("vendedor@gmail.com");
    }

    Usuario crearUsuario(String correo, Rol rol) {
        return usuarios.save(new Usuario("Nombre", "Prueba", correo, passwords.encode("Prueba123!"), rol));
    }
    String login(String correo) throws Exception {
        return body(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("correo", correo, "password", "Prueba123!"))))
            .andExpect(status().isOk()).andReturn()).get("token").asText();
    }
    JsonNode body(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()); }
    MockHttpServletRequestBuilder bearer(MockHttpServletRequestBuilder request, String token) { return request.header("Authorization", "Bearer " + token); }
    Producto producto(int stock) { return productos.save(new Producto(new ProductoDtos.Crear("Martillo", "Herramientas", new BigDecimal("8990"), stock, "/img/productos/martillo_img.jpg", "Martillo de prueba."))); }
    String compra(Producto p, int cantidad) throws Exception { return json.writeValueAsString(Map.of("tipoEntrega", "RETIRO", "items", List.of(Map.of("productoId", p.getId(), "cantidad", cantidad)))); }
    JsonNode pedir(Producto p, int cantidad) throws Exception {
        return body(mvc.perform(bearer(post("/api/v1/pedidos"), cliente).contentType(MediaType.APPLICATION_JSON).content(compra(p, cantidad))).andExpect(status().isCreated()).andReturn());
    }

    @Test void registroGuardaHashSinExponerPassword() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"nombre\":\"Ana\",\"apellidos\":\"Prueba\",\"correo\":\"ANA@gmail.com\",\"password\":\"Segura123!\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("CLIENTE"))
            .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        Usuario u = usuarios.findByCorreo("ana@gmail.com").orElseThrow();
        assertThat(u.getPasswordHash()).isNotEqualTo("Segura123!").startsWith("$2");
        assertThat(passwords.matches("Segura123!", u.getPasswordHash())).isTrue();
    }

    @Test void rechazaCorreoDuplicadoSinDistinguirMayusculas() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"nombre\":\"Ana\",\"apellidos\":\"Prueba\",\"correo\":\"CLIENTE@gmail.com\",\"password\":\"Segura123!\"}"))
            .andExpect(status().isConflict());
    }

    @Test void rechazaFormularioInvalido() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"nombre\":\"\",\"apellidos\":\"\",\"correo\":\"mal\",\"password\":\"12\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.nombre").exists()).andExpect(jsonPath("$.errors.password").exists());
    }

    @Test void registroNoPermiteEnviarRolAdministrador() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"nombre\":\"Ana\",\"apellidos\":\"Prueba\",\"correo\":\"otra@gmail.com\",\"password\":\"Segura123!\",\"rol\":\"ADMIN\"}"))
            .andExpect(status().isBadRequest());
        assertThat(usuarios.findByCorreo("otra@gmail.com")).isEmpty();
    }

    @Test void loginCorrectoEmiteTokenYPermiteConsultarPerfil() throws Exception {
        mvc.perform(bearer(get("/api/v1/auth/me"), cliente)).andExpect(status().isOk()).andExpect(jsonPath("$.correo").value("cliente@gmail.com"));
        assertThat(cliente.split("\\.")).hasSize(3);
    }

    @Test void loginIncorrectoResponde401() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"cliente@gmail.com\",\"password\":\"erronea\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Correo o contrasena incorrectos."));
    }

    @Test void rutaProtegidaSinTokenRespondeJson401() throws Exception {
        mvc.perform(get("/api/v1/pedidos")).andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test void rechazaTokenManipulado() throws Exception {
        String[] partes = cliente.split("\\.");
        partes[1] = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"manipulado\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mvc.perform(bearer(get("/api/v1/pedidos"), String.join(".", partes))).andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test void rechazaTokenExpirado() throws Exception {
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("ferreteria-los-maestros").subject(clienteId.toString())
            .issuedAt(Instant.now().minusSeconds(600)).expiresAt(Instant.now().minusSeconds(120)).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        mvc.perform(bearer(get("/api/v1/auth/me"), token)).andExpect(status().isUnauthorized());
    }

    @Test void tokenDeUsuarioEliminadoYaNoDaAcceso() throws Exception {
        usuarios.deleteById(clienteId);
        mvc.perform(bearer(get("/api/v1/auth/me"), cliente)).andExpect(status().isUnauthorized());
    }

    @Test void adminPuedeCrearLeerActualizarYEliminarProducto() throws Exception {
        var request = new ProductoDtos.Crear("Taladro", "Electricas", new BigDecimal("49990"), 5, "/img/taladro.jpg", "Prueba");
        JsonNode creado = body(mvc.perform(bearer(post("/api/v1/productos"), admin).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
            .andExpect(status().isCreated()).andReturn());
        String ruta = "/api/v1/productos/" + creado.get("id").asText();
        mvc.perform(get(ruta)).andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Taladro"));
        mvc.perform(bearer(patch(ruta), admin).contentType(MediaType.APPLICATION_JSON).content("{\"precio\":45990}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.precio").value(45990)).andExpect(jsonPath("$.stock").value(5)).andExpect(jsonPath("$.imagen").value("/img/taladro.jpg"));
        mvc.perform(bearer(put(ruta), admin).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andExpect(status().isOk());
        mvc.perform(bearer(delete(ruta), admin)).andExpect(status().isNoContent());
        mvc.perform(get(ruta)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/productos")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test void clienteYVendedorNoPuedenModificarCatalogo() throws Exception {
        Producto p = producto(5);
        for (String token : List.of(cliente, vendedor)) {
            mvc.perform(bearer(patch("/api/v1/productos/" + p.getId()), token).contentType(MediaType.APPLICATION_JSON).content("{\"stock\":9}")).andExpect(status().isForbidden());
            mvc.perform(bearer(delete("/api/v1/productos/" + p.getId()), token)).andExpect(status().isForbidden());
        }
    }

    @Test void escrituraSinTokenResponde401() throws Exception {
        mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest @ValueSource(strings = {"{\"precio\":-1}", "{\"stock\":-1}", "{\"stock\":1.5}", "{\"nombre\":\"  \"}"})
    void rechazaProductoInvalido(String cambios) throws Exception {
        Producto p = producto(5);
        mvc.perform(bearer(patch("/api/v1/productos/" + p.getId()), admin).contentType(MediaType.APPLICATION_JSON).content(cambios)).andExpect(status().isBadRequest());
        assertThat(productos.findById(p.getId()).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test void buscaPorNombreYCategoriaYRetorna404ParaIdAusente() throws Exception {
        producto(5);
        mvc.perform(get("/api/v1/productos").param("buscar", "MARTILLO")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/productos").param("buscar", "Herramientas")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/productos").param("buscar", "ausente")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/productos/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test void pedidoCalculaTotalConPrecioDelServidorYDescuentaStock() throws Exception {
        Producto p = producto(5);
        JsonNode pedido = pedir(p, 2);
        assertThat(pedido.get("total").decimalValue()).isEqualByComparingTo("17980");
        assertThat(pedido.get("usuarioId").asText()).isEqualTo(clienteId.toString());
        assertThat(productos.findById(p.getId()).orElseThrow().getStock()).isEqualTo(3);
        mvc.perform(bearer(get("/api/v1/pedidos/" + pedido.get("id").asText()), cliente)).andExpect(status().isOk());
    }

    @Test void pedidoSinStockRevierteTodosLosCambios() throws Exception {
        Producto a = producto(5), b = producto(0);
        String compra = json.writeValueAsString(Map.of("tipoEntrega", "RETIRO", "items", List.of(Map.of("productoId", a.getId(), "cantidad", 1), Map.of("productoId", b.getId(), "cantidad", 1))));
        mvc.perform(bearer(post("/api/v1/pedidos"), cliente).contentType(MediaType.APPLICATION_JSON).content(compra)).andExpect(status().isConflict());
        assertThat(productos.findById(a.getId()).orElseThrow().getStock()).isEqualTo(5);
        assertThat(pedidos.count()).isZero();
    }

    @Test void rechazaLineasRepetidasCantidadesInvalidasYDespachoSinDireccion() throws Exception {
        Producto p = producto(5);
        String doble = json.writeValueAsString(Map.of("tipoEntrega", "RETIRO", "items", List.of(Map.of("productoId", p.getId(), "cantidad", 1), Map.of("productoId", p.getId(), "cantidad", 1))));
        mvc.perform(bearer(post("/api/v1/pedidos"), cliente).contentType(MediaType.APPLICATION_JSON).content(doble)).andExpect(status().isBadRequest());
        mvc.perform(bearer(post("/api/v1/pedidos"), cliente).contentType(MediaType.APPLICATION_JSON).content(compra(p, 0))).andExpect(status().isBadRequest());
        mvc.perform(bearer(post("/api/v1/pedidos"), cliente).contentType(MediaType.APPLICATION_JSON).content(compra(p, 1).replace("RETIRO", "DESPACHO"))).andExpect(status().isBadRequest());
        assertThat(pedidos.count()).isZero();
    }

    @Test void clienteNoVePedidosAjenosPeroAdminSi() throws Exception {
        String id = pedir(producto(5), 1).get("id").asText();
        crearUsuario("otro@gmail.com", Rol.CLIENTE); String otro = login("otro@gmail.com");
        mvc.perform(bearer(get("/api/v1/pedidos/" + id), otro)).andExpect(status().isNotFound());
        mvc.perform(bearer(get("/api/v1/pedidos"), otro)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(bearer(get("/api/v1/pedidos/" + id), admin)).andExpect(status().isOk());
    }

    @Test void personalPuedeCambiarEstadoYCancelacionReponeUnaSolaVez() throws Exception {
        Producto p = producto(5); String id = pedir(p, 2).get("id").asText();
        String ruta = "/api/v1/pedidos/" + id + "/estado";
        mvc.perform(bearer(patch(ruta), cliente).contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADO\"}")).andExpect(status().isForbidden());
        mvc.perform(bearer(patch(ruta), vendedor).contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"PREPARACION\"}")).andExpect(status().isOk());
        for (int i = 0; i < 2; i++) mvc.perform(bearer(patch(ruta), admin).contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADO\"}")).andExpect(status().isOk());
        assertThat(productos.findById(p.getId()).orElseThrow().getStock()).isEqualTo(5);
        mvc.perform(bearer(patch(ruta), admin).contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"PREPARACION\"}")).andExpect(status().isConflict());
    }

    @Test void dosComprasSimultaneasNoSobrevendenLaUltimaUnidad() throws Exception {
        Producto p = producto(1); String datos = compra(p, 1);
        ExecutorService pool = Executors.newFixedThreadPool(2); CountDownLatch inicio = new CountDownLatch(1);
        try {
            Callable<Integer> comprar = () -> { inicio.await(); return mvc.perform(bearer(post("/api/v1/pedidos"), cliente).contentType(MediaType.APPLICATION_JSON).content(datos)).andReturn().getResponse().getStatus(); };
            Future<Integer> a = pool.submit(comprar), b = pool.submit(comprar); inicio.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(201, 409);
            assertThat(productos.findById(p.getId()).orElseThrow().getStock()).isZero(); assertThat(pedidos.count()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    @Test void swaggerPublicaEndpointsYEsquemaBearer() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
            .andExpect(jsonPath("$.paths['/api/v1/auth/login']").exists()).andExpect(jsonPath("$.paths['/api/v1/productos/{id}']").exists()).andExpect(jsonPath("$.paths['/api/v1/pedidos']").exists());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test void corsPermiteElFrontendConfigurado() throws Exception {
        mvc.perform(options("/api/v1/pedidos").header("Origin", "http://localhost:5173").header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "authorization,content-type")).andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
