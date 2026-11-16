package cl.losmaestros.api.productos;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public final class ProductoDtos {
    private ProductoDtos() {}
    public record Crear(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 80) String categoria,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal precio,
        @NotNull @Min(0) @Max(1000000) Integer stock,
        @Size(max = 500) String imagen,
        @Size(max = 500) String descripcion
    ) {}
    public record Cambios(
        @Pattern(regexp = "(?s).*\\S.*", message = "No puede quedar en blanco.") @Size(max = 100) String nombre,
        @Pattern(regexp = "(?s).*\\S.*", message = "No puede quedar en blanco.") @Size(max = 80) String categoria,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal precio,
        @Min(0) @Max(1000000) Integer stock,
        @Size(max = 500) String imagen,
        @Size(max = 500) String descripcion
    ) {}
    public record View(UUID id, String nombre, String categoria, BigDecimal precio, int stock, String imagen, String descripcion) {
        public static View from(Producto p) { return new View(p.getId(), p.getNombre(), p.getCategoria(), p.getPrecio(), p.getStock(), p.getImagen(), p.getDescripcion()); }
    }
}
