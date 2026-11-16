package cl.losmaestros.api.productos;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/productos") @Tag(name = "Productos")
public class ProductoController {
    private final ProductoService service;
    public ProductoController(ProductoService service) { this.service = service; }
    @GetMapping @SecurityRequirements @Operation(summary = "Consultar el catalogo; buscar por nombre o categoria")
    public List<ProductoDtos.View> listar(@RequestParam(required = false) String buscar) { return service.listar(buscar); }
    @GetMapping("/{id}") @SecurityRequirements @Operation(summary = "Consultar el detalle de un producto")
    public ProductoDtos.View obtener(@PathVariable UUID id) { return service.obtener(id); }
    @PostMapping @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.CREATED) @Operation(summary = "Crear producto (ADMIN)")
    public ProductoDtos.View crear(@Valid @RequestBody ProductoDtos.Crear datos) { return service.crear(datos); }
    @PatchMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Editar campos de un producto (ADMIN)")
    public ProductoDtos.View actualizar(@PathVariable UUID id, @Valid @RequestBody ProductoDtos.Cambios cambios) { return service.actualizar(id, cambios); }
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Reemplazar todos los datos de un producto (ADMIN)")
    public ProductoDtos.View reemplazar(@PathVariable UUID id, @Valid @RequestBody ProductoDtos.Crear datos) {
        return service.actualizar(id, new ProductoDtos.Cambios(datos.nombre(), datos.categoria(), datos.precio(), datos.stock(), datos.imagen(), datos.descripcion()));
    }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.NO_CONTENT) @Operation(summary = "Eliminar producto del catalogo (ADMIN)")
    public void eliminar(@PathVariable UUID id) { service.eliminar(id); }
}
