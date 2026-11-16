package cl.losmaestros.api.pedidos;

import cl.losmaestros.api.auth.*;
import cl.losmaestros.api.common.ApiException;
import cl.losmaestros.api.productos.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class PedidoService {
    private final PedidoRepository pedidos;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;
    public PedidoService(PedidoRepository pedidos, ProductoRepository productos, UsuarioRepository usuarios) {
        this.pedidos = pedidos; this.productos = productos; this.usuarios = usuarios;
    }

    public PedidoDtos.View crear(UUID usuarioId, PedidoDtos.Crear datos) {
        if (datos.tipoEntrega() == TipoEntrega.DESPACHO && (datos.direccion() == null || datos.direccion().isBlank())) throw ApiException.badRequest("La direccion es obligatoria para despacho.");
        Usuario usuario = usuarios.findById(usuarioId).orElseThrow(() -> ApiException.notFound("Usuario no encontrado."));
        Set<UUID> ids = new HashSet<>();
        for (var item : datos.items()) if (!ids.add(item.productoId())) throw ApiException.badRequest("Un producto no puede repetirse en el pedido. Usa una sola cantidad.");
        Pedido pedido = new Pedido(usuario, datos.tipoEntrega(), datos.direccion());
        // Bloquea el inventario en orden fijo; una venta concurrente debe esperar y releer el stock.
        for (var item : datos.items().stream().sorted(Comparator.comparing(PedidoDtos.Item::productoId)).toList()) {
            Producto producto = productos.bloquear(item.productoId()).filter(Producto::isActivo).orElseThrow(() -> ApiException.notFound("Producto no encontrado."));
            if (producto.getStock() < item.cantidad()) throw ApiException.conflict("Stock insuficiente para " + producto.getNombre() + ".");
            producto.descontar(item.cantidad());
            pedido.agregar(new ItemPedido(pedido, producto, item.cantidad()));
        }
        // La transaccion confirma pedido y stock juntos; cualquier error revierte ambos cambios.
        return PedidoDtos.View.from(pedidos.saveAndFlush(pedido));
    }

    @Transactional(readOnly = true)
    public List<PedidoDtos.View> listar(UUID usuarioId, boolean personal) {
        return (personal ? pedidos.findByUsuarioIdOrderByFechaDesc(usuarioId) : pedidos.findAllByOrderByFechaDesc()).stream().map(PedidoDtos.View::from).toList();
    }

    @Transactional(readOnly = true)
    public PedidoDtos.View obtener(UUID id, UUID usuarioId, boolean personal) {
        Pedido pedido = pedidos.findById(id).filter(p -> !personal || p.getUsuario().getId().equals(usuarioId)).orElseThrow(() -> ApiException.notFound("Pedido no encontrado."));
        return PedidoDtos.View.from(pedido);
    }

    public PedidoDtos.View cambiarEstado(UUID id, EstadoPedido nuevo) {
        Pedido pedido = pedidos.bloquear(id).orElseThrow(() -> ApiException.notFound("Pedido no encontrado."));
        EstadoPedido actual = pedido.getEstado();
        if (actual == nuevo) return PedidoDtos.View.from(pedido);
        boolean permitido = switch (actual) {
            case PENDIENTE -> nuevo == EstadoPedido.PREPARACION || nuevo == EstadoPedido.CANCELADO;
            case PREPARACION -> nuevo == EstadoPedido.CANCELADO || (pedido.getTipoEntrega() == TipoEntrega.RETIRO ? nuevo == EstadoPedido.ENTREGADO : nuevo == EstadoPedido.DESPACHADO);
            case DESPACHADO -> nuevo == EstadoPedido.ENTREGADO;
            case ENTREGADO, CANCELADO -> false;
        };
        if (!permitido) throw ApiException.conflict("Transicion de estado no permitida.");
        if (nuevo == EstadoPedido.CANCELADO) {
            // Una cancelacion repetida no repone dos veces: el estado se comprueba bajo bloqueo.
            pedido.getItems().stream().sorted(Comparator.comparing(i -> i.getProducto().getId())).forEach(item -> {
                Producto producto = productos.bloquear(item.getProducto().getId()).orElseThrow(() -> ApiException.notFound("Producto no encontrado."));
                producto.reponer(item.getCantidad());
            });
        }
        pedido.cambiarEstado(nuevo); return PedidoDtos.View.from(pedido);
    }
}
