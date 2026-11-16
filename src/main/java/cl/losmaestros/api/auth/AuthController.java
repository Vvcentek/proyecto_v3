package cl.losmaestros.api.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/auth")
@Tag(name = "Autenticacion", description = "Registro, inicio de sesion y consulta del usuario autenticado")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) @SecurityRequirements
    @Operation(summary = "Registrar un cliente; devuelve datos sin contrasena")
    public AuthDtos.UsuarioView registrar(@Valid @RequestBody AuthDtos.Registro datos) { return service.registrar(datos); }

    @PostMapping("/login") @SecurityRequirements
    @Operation(summary = "Iniciar sesion y obtener un JWT firmado")
    public AuthDtos.TokenResponse login(@Valid @RequestBody AuthDtos.Login datos) { return service.login(datos); }

    @GetMapping("/me") @Operation(summary = "Consultar la sesion mediante un JWT valido")
    public AuthDtos.UsuarioView actual(@AuthenticationPrincipal Jwt jwt) { return service.actual(jwt.getSubject()); }
}
