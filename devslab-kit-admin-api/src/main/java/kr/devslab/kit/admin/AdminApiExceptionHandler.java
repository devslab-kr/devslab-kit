package kr.devslab.kit.admin;

import jakarta.validation.ConstraintViolationException;
import kr.devslab.kit.access.PermissionDeniedException;
import kr.devslab.kit.identity.AccountLoginException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates every admin-API exception into an RFC 7807 {@link ProblemDetail}
 * ({@code application/problem+json}).
 *
 * <p>The standard members are {@code type} / {@code title} / {@code status} /
 * {@code detail}; validation failures additionally carry a non-standard
 * {@code errors} extension (a list of per-field messages). Clients should read the
 * human-readable message from {@code detail} (falling back to {@code title}).
 */
@RestControllerAdvice(basePackages = "kr.devslab.kit.admin")
public class AdminApiExceptionHandler {

    @ExceptionHandler(AccountLoginException.class)
    public ProblemDetail handleLogin(AccountLoginException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ProblemDetail handleDenied(PermissionDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleConflict(IllegalStateException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Bad Request", "Validation failed");
        problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList());
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Bad Request", "Constraint violation");
        problem.setProperty("errors", ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList());
        return problem;
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        if (detail != null) {
            problem.setDetail(detail);
        }
        return problem;
    }
}
