package cl.losmaestros.api.productos;

import cl.losmaestros.api.common.ApiException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class ProductoService {
    private final ProductoRepository productos;
    public ProductoService(ProductoRepository productos) { this.productos = productos; }
    @Transactional(readOnly = true)
    public List<ProductoDtos.View> listar(String buscar) {
        String texto = buscar == null ? "" : buscar.trim().toLowerCase(Locale.ROOT);
        return productos.findByActivoTrueOrderByNombreAsc().stream()
            .filter(p -> (p.getNombre() + " " + p.getCategoria()).toLowerCase(Locale.ROOT).contains(texto))
            .map(ProductoDtos.View::from).toList();
    }
    @Transactional(readOnly = true)
    public ProductoDtos.View obtener(UUID id) { return ProductoDtos.View.from(productos.findById(id).filter(Producto::isActivo).orElseThrow(() -> ApiException.notFound("Producto no encontrado."))); }
    public ProductoDtos.View crear(ProductoDtos.Crear datos) { return ProductoDtos.View.from(productos.save(new Producto(datos))); }
    public ProductoDtos.View actualizar(UUID id, ProductoDtos.Cambios cambios) {
        Producto producto = productos.bloquear(id).filter(Producto::isActivo).orElseThrow(() -> ApiException.notFound("Producto no encontrado."));
        producto.actualizar(cambios); return ProductoDtos.View.from(producto);
    }
    public void eliminar(UUID id) {
        // Baja logica para conservar el historial de los pedidos existentes.
        Producto producto = productos.bloquear(id).filter(Producto::isActivo).orElseThrow(() -> ApiException.notFound("Producto no encontrado."));
        producto.desactivar();
    }
}
