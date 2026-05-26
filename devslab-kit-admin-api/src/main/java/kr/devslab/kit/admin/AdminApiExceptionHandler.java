package kr.devslab.kit.admin;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import kr.devslab.kit.access.PermissionDeniedException;
import kr.devslab.kit.identity.AccountLoginException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "kr.devslab.kit.admin")
public class AdminApiExceptionHandler {

    @ExceptionHandler(AccountLoginException.class)
    public ResponseEntity<ApiError> handleLogin(AccountLoginException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(401, "Unauthorized", ex.getMessage()));
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ApiError> handleDenied(PermissionDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "Bad Request", "Validation failed", details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "Bad Request", "Constraint violation", details));
    }
}
