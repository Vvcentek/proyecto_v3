package cl.losmaestros.api.auth;

import cl.losmaestros.api.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final String issuer;
    private final long duration;
    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwords, JwtEncoder encoder,
            @Value("${app.jwt.issuer}") String issuer, @Value("${app.jwt.duration-minutes}") long duration) {
        this.usuarios = usuarios; this.passwords = passwords; this.encoder = encoder; this.issuer = issuer; this.duration = duration;
    }

    // El servidor decide el rol; el registro publico nunca crea administradores.
    @Transactional
    public AuthDtos.UsuarioView registrar(AuthDtos.Registro datos) {
        String correo = datos.correo().trim().toLowerCase(Locale.ROOT);
        if (usuarios.existsByCorreo(correo)) throw ApiException.conflict("Este correo ya esta registrado.");
        if (datos.password().getBytes(StandardCharsets.UTF_8).length > 72) throw ApiException.badRequest("La contrasena no puede superar 72 bytes.");
        Usuario usuario = new Usuario(datos.nombre().trim(), datos.apellidos().trim(), correo, passwords.encode(datos.password()), Rol.CLIENTE);
        return AuthDtos.UsuarioView.from(usuarios.saveAndFlush(usuario));
    }

    @Transactional(readOnly = true)
    public AuthDtos.TokenResponse login(AuthDtos.Login datos) {
        Usuario usuario = usuarios.findByCorreo(datos.correo().trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (usuario == null || !usuario.isActivo() || !passwords.matches(datos.password(), usuario.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contrasena incorrectos.");
        }
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject(usuario.getId().toString())
            .issuedAt(ahora).expiresAt(ahora.plus(duration, ChronoUnit.MINUTES)).claim("rol", usuario.getRol().name()).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AuthDtos.TokenResponse(token, "Bearer", duration * 60, AuthDtos.UsuarioView.from(usuario));
    }

    public AuthDtos.UsuarioView actual(String id) {
        return AuthDtos.UsuarioView.from(usuarios.findById(UUID.fromString(id)).orElseThrow(() -> ApiException.notFound("Usuario no encontrado.")));
    }
}
