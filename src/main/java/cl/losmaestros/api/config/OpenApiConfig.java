package cl.losmaestros.api.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
    @Bean OpenAPI openApi() {
        return new OpenAPI().info(new Info().title("Ferreteria Los Maestros API").version("1.0.0")
            .description("Backend de entrega 3. Registra un cliente, inicia sesion y pega el token en Authorize. Las escrituras de productos requieren ADMIN."))
            .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
