package cl.losmaestros.api.pedidos;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/pedidos") @Tag(name = "Pedidos", description = "Compra, historial y seguimiento con JWT")
public class PedidoController {
    private final PedidoService service;
    public PedidoController(PedidoService service) { this.service = service; }
    private boolean esCliente(Authentication auth) { return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE")); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Operation(summary = "Crear pedido y descontar stock; precios calculados por el servidor")
    public PedidoDtos.View crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PedidoDtos.Crear datos) { return service.crear(UUID.fromString(jwt.getSubject()), datos); }
    @GetMapping @Operation(summary = "Cliente ve sus pedidos; ADMIN y VENDEDOR ven todos")
    public List<PedidoDtos.View> listar(@AuthenticationPrincipal Jwt jwt, Authentication auth) { return service.listar(UUID.fromString(jwt.getSubject()), esCliente(auth)); }
    @GetMapping("/{id}") @Operation(summary = "Consultar un pedido respetando su propietario")
    public PedidoDtos.View obtener(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt, Authentication auth) { return service.obtener(id, UUID.fromString(jwt.getSubject()), esCliente(auth)); }
    @PatchMapping("/{id}/estado") @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')") @Operation(summary = "Actualizar seguimiento o cancelar y reponer stock (personal)")
    public PedidoDtos.View estado(@PathVariable UUID id, @Valid @RequestBody PedidoDtos.CambiarEstado datos) { return service.cambiarEstado(id, datos.estado()); }
}
