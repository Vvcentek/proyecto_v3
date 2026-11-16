package cl.losmaestros.api.auth;

import jakarta.validation.constraints.*;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}
    public record Registro(
        @NotBlank @Size(max = 50) String nombre,
        @NotBlank @Size(max = 100) String apellidos,
        @NotBlank @Email @Size(max = 100) String correo,
        @NotBlank @Size(min = 8, max = 72) String password
    ) {}
    public record Login(@NotBlank @Email @Size(max = 100) String correo, @NotBlank @Size(max = 72) String password) {}
    public record UsuarioView(UUID id, String nombre, String apellidos, String correo, Rol rol) {
        public static UsuarioView from(Usuario u) { return new UsuarioView(u.getId(), u.getNombre(), u.getApellidos(), u.getCorreo(), u.getRol()); }
    }
    public record TokenResponse(String token, String tipo, long expiresIn, UsuarioView usuario) {}
}
