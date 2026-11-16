package cl.losmaestros.api.productos;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name = "productos")
public class Producto {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, length = 100) private String nombre;
    @Column(nullable = false, length = 80) private String categoria;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal precio;
    @Column(nullable = false) private int stock;
    @Column(length = 500) private String imagen;
    @Column(length = 500) private String descripcion;
    @Column(nullable = false) private boolean activo = true;
    @Version private long version;
    protected Producto() {}
    public Producto(ProductoDtos.Crear datos) {
        nombre = datos.nombre().trim(); categoria = datos.categoria().trim(); precio = datos.precio(); stock = datos.stock();
        imagen = datos.imagen(); descripcion = datos.descripcion();
    }
    // Aplica solo campos presentes: un PATCH de precio conserva el resto del producto.
    public void actualizar(ProductoDtos.Cambios datos) {
        if (datos.nombre() != null) nombre = datos.nombre().trim();
        if (datos.categoria() != null) categoria = datos.categoria().trim();
        if (datos.precio() != null) precio = datos.precio();
        if (datos.stock() != null) stock = datos.stock();
        if (datos.imagen() != null) imagen = datos.imagen();
        if (datos.descripcion() != null) descripcion = datos.descripcion();
    }
    public void descontar(int cantidad) { stock -= cantidad; }
    public void reponer(int cantidad) { stock += cantidad; }
    public void desactivar() { activo = false; }
    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public BigDecimal getPrecio() { return precio; }
    public int getStock() { return stock; }
    public String getImagen() { return imagen; }
    public String getDescripcion() { return descripcion; }
    public boolean isActivo() { return activo; }
}
