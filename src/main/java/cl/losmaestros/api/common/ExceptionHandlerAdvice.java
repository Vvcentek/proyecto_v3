package cl.losmaestros.api.common;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class ExceptionHandlerAdvice {
    private static final Logger log = LoggerFactory.getLogger(ExceptionHandlerAdvice.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> api(ApiException ex) { return ResponseEntity.status(ex.getStatus()).body(ApiError.of(ex.getStatus().value(), ex.getMessage())); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "Revisa los datos del formulario.", errors));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> invalidInput(Exception ex) { return ResponseEntity.badRequest().body(ApiError.of(400, "El formato de los datos no es valido.")); }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> duplicate(DataIntegrityViolationException ex) { return ResponseEntity.status(409).body(ApiError.of(409, "Los datos ya existen o estan relacionados con otro registro.")); }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ApiError> lock(PessimisticLockingFailureException ex) { return ResponseEntity.status(409).body(ApiError.of(409, "El inventario esta siendo actualizado. Intenta nuevamente.")); }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> forbidden(AccessDeniedException ex) { return ResponseEntity.status(403).body(ApiError.of(403, "No tienes permisos para esta operacion.")); }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception ex) {
        log.error("Error interno de la API", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(500, "No se pudo completar la operacion."));
    }
}
