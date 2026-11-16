package cl.losmaestros.api.config;

import cl.losmaestros.api.auth.UsuarioRepository;
import cl.losmaestros.api.common.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean SecretKey jwtKey(@Value("${app.jwt.secret}") String secret, Environment environment) {
        byte[] bytes;
        if (secret.isBlank()) {
            if (environment.matchesProfiles("prod")) throw new IllegalStateException("JWT_SECRET es obligatorio en produccion.");
            bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
            LoggerFactory.getLogger(SecurityConfig.class).warn("Clave JWT local temporal: inicia sesion nuevamente tras reiniciar. Configura JWT_SECRET para conservarla.");
        } else {
            try { bytes = Base64.getDecoder().decode(secret); }
            catch (IllegalArgumentException ex) { throw new IllegalStateException("JWT_SECRET debe estar codificado en Base64."); }
            if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET debe contener al menos 32 bytes.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean JwtEncoder jwtEncoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key)); }

    @Bean JwtDecoder jwtDecoder(SecretKey key, @Value("${app.jwt.issuer}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> requiredClaims = jwt -> jwt.getExpiresAt() != null && jwt.getSubject() != null
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Faltan datos del token.", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), requiredClaims));
        return decoder;
    }

    // Spring Security verifica la firma, emisor y vigencia antes de ejecutar los controladores.
    @Bean SecurityFilterChain security(HttpSecurity http, JwtDecoder decoder, UsuarioRepository usuarios, ObjectMapper json,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource corsSource) throws Exception {
        http.csrf(csrf -> csrf.disable()).cors(cors -> cors.configurationSource(corsSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(rules -> rules
                .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/api/v1/salud").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/productos", "/api/v1/productos/*").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth
                .authenticationEntryPoint((request, response, ex) -> {
                    response.setStatus(401); response.setContentType("application/json"); response.setHeader("WWW-Authenticate", "Bearer");
                    json.writeValue(response.getOutputStream(), ApiError.of(401, "El JWT es invalido o ha expirado."));
                })
                .jwt(jwt -> jwt.decoder(decoder).jwtAuthenticationConverter(token -> {
                // Consulta el rol vigente en la base de datos, no un permiso enviado por el cliente.
                try {
                    var usuario = usuarios.findById(UUID.fromString(token.getSubject())).filter(u -> u.isActivo())
                        .orElseThrow(() -> new InvalidBearerTokenException("Usuario no disponible."));
                    return new JwtAuthenticationToken(token, List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())));
                } catch (IllegalArgumentException ex) { throw new InvalidBearerTokenException("Identificador de usuario invalido."); }
            })))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, ex) -> {
                    response.setStatus(401); response.setContentType("application/json"); response.setHeader("WWW-Authenticate", "Bearer");
                    json.writeValue(response.getOutputStream(), ApiError.of(401, "Necesitas un JWT valido para acceder."));
                })
                .accessDeniedHandler((request, response, ex) -> {
                    response.setStatus(403); response.setContentType("application/json");
                    json.writeValue(response.getOutputStream(), ApiError.of(403, "No tienes permisos para esta operacion."));
                }));
        return http.build();
    }

    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origins}") String origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); return source;
    }
}
