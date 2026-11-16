package cl.losmaestros.api.pedidos;

import cl.losmaestros.api.productos.Producto;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name = "items_pedido")
public class ItemPedido {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "pedido_id", nullable = false) private Pedido pedido;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "producto_id", nullable = false) private Producto producto;
    @Column(nullable = false, length = 100) private String nombre;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal precioUnitario;
    @Column(nullable = false) private int cantidad;
    protected ItemPedido() {}
    // Guarda nombre y precio de la compra para que editar el catalogo no cambie pedidos pasados.
    public ItemPedido(Pedido pedido, Producto producto, int cantidad) {
        this.pedido = pedido; this.producto = producto; this.nombre = producto.getNombre();
        this.precioUnitario = producto.getPrecio(); this.cantidad = cantidad;
    }
    public Producto getProducto() { return producto; }
    public String getNombre() { return nombre; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public int getCantidad() { return cantidad; }
    public BigDecimal getSubtotal() { return precioUnitario.multiply(BigDecimal.valueOf(cantidad)); }
}
