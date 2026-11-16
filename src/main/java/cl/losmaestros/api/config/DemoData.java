package cl.losmaestros.api.config;

import cl.losmaestros.api.auth.*;
import cl.losmaestros.api.productos.*;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component @Profile("demo")
public class DemoData implements CommandLineRunner {
    private final UsuarioRepository usuarios;
    private final ProductoRepository productos;
    private final PasswordEncoder passwords;
    private final Environment environment;
    private final String adminPassword;
    public DemoData(UsuarioRepository usuarios, ProductoRepository productos, PasswordEncoder passwords, Environment environment,
            @Value("${DEMO_ADMIN_PASSWORD:DemoAdmin123!}") String adminPassword) {
        this.usuarios = usuarios; this.productos = productos; this.passwords = passwords; this.environment = environment; this.adminPassword = adminPassword;
    }
    // Datos ficticios solo cuando se activa explicitamente el perfil demo.
    @Override @Transactional public void run(String... args) {
        if (environment.matchesProfiles("prod")) throw new IllegalStateException("No combines los perfiles demo y prod.");
        if (!usuarios.existsByCorreo("admin@losmaestros.cl")) usuarios.save(new Usuario("Admin", "Demo", "admin@losmaestros.cl", passwords.encode(adminPassword), Rol.ADMIN));
        if (productos.count() == 0) {
            agregar("Martillo", "Herramientas manuales", "8990", 10, "martillo_img.jpg");
            agregar("Taladro electrico", "Herramientas electricas", "49990", 5, "taladro_img.jpg");
            agregar("Destornillador", "Herramientas manuales", "5990", 15, "destornillador_img.jpg");
            agregar("Cinta aisladora", "Accesorios", "2490", 20, "cinta_img.jpg");
            agregar("Caja de tornillos", "Fijaciones", "3990", 30, "tornillos_img.jpg");
        }
    }
    private void agregar(String nombre, String categoria, String precio, int stock, String imagen) {
        productos.save(new Producto(new ProductoDtos.Crear(nombre, categoria, new BigDecimal(precio), stock, "/img/productos/" + imagen, "Producto de demostracion de la ferreteria.")));
    }
}
