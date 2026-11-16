package cl.losmaestros.api.pedidos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class PedidoDtos {
    private PedidoDtos() {}
    public record Item(@NotNull UUID productoId, @NotNull @Min(1) @Max(9999) Integer cantidad) {}
    public record Crear(@NotNull TipoEntrega tipoEntrega, @Size(max = 300) String direccion, @NotEmpty @Size(max = 50) List<@NotNull @Valid Item> items) {}
    public record CambiarEstado(@NotNull EstadoPedido estado) {}
    public record ItemView(UUID productoId, String nombre, BigDecimal precioUnitario, int cantidad, BigDecimal subtotal) {}
    public record View(UUID id, UUID usuarioId, Instant fecha, EstadoPedido estado, TipoEntrega tipoEntrega, String direccion, List<ItemView> items, BigDecimal total) {
        public static View from(Pedido p) {
            return new View(p.getId(), p.getUsuario().getId(), p.getFecha(), p.getEstado(), p.getTipoEntrega(), p.getDireccion(),
                p.getItems().stream().map(i -> new ItemView(i.getProducto().getId(), i.getNombre(), i.getPrecioUnitario(), i.getCantidad(), i.getSubtotal())).toList(), p.getTotal());
        }
    }
}
