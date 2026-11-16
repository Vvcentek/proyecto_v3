package cl.losmaestros.api.common;

import java.util.Map;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

@RestController
public class SaludController {
    @GetMapping("/api/v1/salud") @SecurityRequirements
    public Map<String, String> salud() { return Map.of("estado", "ok", "servicio", "ferreteria-api"); }
}
