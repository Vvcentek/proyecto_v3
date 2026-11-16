package cl.losmaestros.api.pedidos;

import cl.losmaestros.api.auth.Usuario;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity @Table(name = "pedidos")
public class Pedido {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id", nullable = false) private Usuario usuario;
    @Column(nullable = false) private Instant fecha;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EstadoPedido estado;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15) private TipoEntrega tipoEntrega;
    @Column(length = 300) private String direccion;
    @Column(nullable = false, precision = 16, scale = 2) private BigDecimal total = BigDecimal.ZERO;
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("nombre asc") private List<ItemPedido> items = new ArrayList<>();
    protected Pedido() {}
    public Pedido(Usuario usuario, TipoEntrega tipoEntrega, String direccion) {
        this.usuario = usuario; this.tipoEntrega = tipoEntrega; this.direccion = direccion;
        this.fecha = Instant.now(); this.estado = EstadoPedido.PENDIENTE;
    }
    public void agregar(ItemPedido item) { items.add(item); total = total.add(item.getSubtotal()); }
    public void cambiarEstado(EstadoPedido estado) { this.estado = estado; }
    public UUID getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Instant getFecha() { return fecha; }
    public EstadoPedido getEstado() { return estado; }
    public TipoEntrega getTipoEntrega() { return tipoEntrega; }
    public String getDireccion() { return direccion; }
    public BigDecimal getTotal() { return total; }
    public List<ItemPedido> getItems() { return items; }
}
