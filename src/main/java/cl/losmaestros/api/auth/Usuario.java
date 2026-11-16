package cl.losmaestros.api.auth;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(name = "uk_usuario_correo", columnNames = "correo"))
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, length = 50) private String nombre;
    @Column(nullable = false, length = 100) private String apellidos;
    @Column(nullable = false, length = 100) private String correo;
    @Column(nullable = false, length = 100) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15) private Rol rol;
    @Column(nullable = false) private boolean activo = true;

    protected Usuario() {}
    public Usuario(String nombre, String apellidos, String correo, String passwordHash, Rol rol) {
        this.nombre = nombre; this.apellidos = apellidos; this.correo = correo; this.passwordHash = passwordHash; this.rol = rol;
    }
    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getApellidos() { return apellidos; }
    public String getCorreo() { return correo; }
    public String getPasswordHash() { return passwordHash; }
    public Rol getRol() { return rol; }
    public boolean isActivo() { return activo; }
}
