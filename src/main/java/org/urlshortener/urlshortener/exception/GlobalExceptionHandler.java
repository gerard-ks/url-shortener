package org.urlshortener.urlshortener.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.urlshortener.urlshortener.constants.ErrorCode;
import org.urlshortener.urlshortener.exception.base.UrlShortenerException;
import org.urlshortener.urlshortener.exception.business.BusinessRuleException;
import org.urlshortener.urlshortener.exception.business.UrlExpiredException;
import org.urlshortener.urlshortener.exception.ratelimit.RateLimitExceededException;
import org.urlshortener.urlshortener.exception.resource.ResourceNotFoundException;
import org.urlshortener.urlshortener.exception.security.IpBlockedException;
import org.urlshortener.urlshortener.exception.security.UnauthorizedOperationException;
import org.urlshortener.urlshortener.exception.system.CodeGenerationException;
import org.urlshortener.urlshortener.exception.validation.ShortCodeAlreadyExistsException;
import org.urlshortener.urlshortener.exception.validation.ValidationException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {} error(s)", ex.getBindingResult().getErrorCount());

        ProblemDetail problem = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                null
        );

        List<FieldErrorDetail> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .toList();

        problem.setProperty("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(problem);
    }


    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());

        ProblemDetail problem = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                null
        );

        List<FieldErrorDetail> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> new FieldErrorDetail(
                        extractFieldName(violation.getPropertyPath().toString()),
                        violation.getInvalidValue(),
                        violation.getMessage()
                ))
                .toList();

        problem.setProperty("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(problem);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMostSpecificCause().getMessage());

        ProblemDetail problem = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                ErrorCode.MALFORMED_REQUEST,
                null
        );

        return ResponseEntity.badRequest().body(problem);
    }


    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(MissingServletRequestParameterException ex) {
        log.warn("Missing parameter: {}", ex.getParameterName());

        ProblemDetail problem = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                ErrorCode.MISSING_PARAMETER,
                new Object[]{ex.getParameterName()}
        );

        return ResponseEntity.badRequest().body(problem);
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getValue());

        String expectedType = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "unknown";

        ProblemDetail problem = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                ErrorCode.TYPE_MISMATCH,
                new Object[]{ex.getName(), expectedType}
        );

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidation(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        ProblemDetail problem = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                ex.getErrorCode(),
                ex.getArgs()
        );

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ShortCodeAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ShortCodeAlreadyExistsException ex) {
        log.warn("Conflict: {}", ex.getErrorCode());

        ProblemDetail problem = createProblemDetail(
                HttpStatus.CONFLICT,
                ex.getErrorCode(),
                ex.getArgs()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getErrorCode());

        ProblemDetail problem = createProblemDetail(
                HttpStatus.NOT_FOUND,
                ex.getErrorCode(),
                ex.getArgs()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(UrlExpiredException.class)
    public ResponseEntity<ProblemDetail> handleExpired(UrlExpiredException ex) {
        log.warn("URL expired: {}", ex.getShortCode());

        ProblemDetail problem = createProblemDetail(
                HttpStatus.GONE,
                ex.getErrorCode(),
                ex.getArgs()
        );

        if (ex.getExpiredAt() != null) {
            problem.setProperty("expiredAt", ex.getExpiredAt());
        }

        return ResponseEntity.status(HttpStatus.GONE).body(problem);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ProblemDetail> handleBusinessRule(BusinessRuleException ex) {
        log.warn("Business rule violation: {}", ex.getErrorCode());

        ProblemDetail problem = createProblemDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getErrorCode(),
                ex.getArgs()
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<ProblemDetail> handleUnauthorized(UnauthorizedOperationException ex) {
        log.warn("Unauthorized operation: {}", ex.getErrorCode());

        HttpStatus status = ex.getErrorCode().equals(ErrorCode.TOKEN_MISSING)
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.FORBIDDEN;

        ProblemDetail problem = createProblemDetail(
                status,
                ex.getErrorCode(),
                ex.getArgs()
        );

        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(IpBlockedException.class)
    public ResponseEntity<ProblemDetail> handleIpBlocked(IpBlockedException ex) {
        log.warn("IP blocked, retry after: {}s", ex.getRetryAfterSeconds());

        ProblemDetail problem = createProblemDetail(
                HttpStatus.FORBIDDEN,
                ex.getErrorCode(),
                ex.getArgs()
        );

        problem.setProperty("retryAfter", ex.getRetryAfterSeconds());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .headers(headers)
                .body(problem);
    }

    @ExceptionHandler(org.urlshortener.urlshortener.exception.security.SecurityException.class)
    public ResponseEntity<ProblemDetail> handleSecurity(org.urlshortener.urlshortener.exception.security.SecurityException ex) {
        log.warn("Security exception: {}", ex.getErrorCode());

        ProblemDetail problem = createProblemDetail(
                HttpStatus.FORBIDDEN,
                ex.getErrorCode(),
                ex.getArgs()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimit(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded, retry after: {}s", ex.getRetryAfterSeconds());

        ProblemDetail problem = createProblemDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                ex.getErrorCode(),
                ex.getArgs()
        );

        problem.setProperty("retryAfter", ex.getRetryAfterSeconds());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .body(problem);
    }

    @ExceptionHandler(CodeGenerationException.class)
    public ResponseEntity<ProblemDetail> handleCodeGeneration(CodeGenerationException ex) {
        log.error("Code generation failed after {} attempts", ex.getArgs()[0], ex);

        ProblemDetail problem = createProblemDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                ex.getErrorCode(),
                ex.getArgs()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    @ExceptionHandler(UrlShortenerException.class)
    public ResponseEntity<ProblemDetail> handleUrlShortenerException(UrlShortenerException ex) {
        log.error("Unhandled UrlShortenerException: {}", ex.getErrorCode(), ex);

        ProblemDetail problem = createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getErrorCode(),
                ex.getArgs()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException ex) {
        log.debug("Static resource not found: {}", ex.getResourcePath());
        return ResponseEntity.notFound().build();
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);

        ProblemDetail problem = createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }



    private ProblemDetail createProblemDetail(HttpStatus status, String errorCode, Object[] args) {
        String message = getMessage(errorCode, args);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setProperty("code", errorCode);
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    private String getMessage(String code, Object[] args) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            log.warn("Message not found for code '{}' with locale '{}', using code as fallback", code, locale);
            return code;
        }
    }


    private FieldErrorDetail mapFieldError(FieldError error) {
        return new FieldErrorDetail(
                error.getField(),
                error.getRejectedValue(),
                resolveFieldErrorMessage(error)
        );
    }


    private String resolveFieldErrorMessage(FieldError error) {
        Locale locale = LocaleContextHolder.getLocale();

        // 1. Essayer nos codes personnalisés d'abord
        if (error.getCodes() != null) {
            for (String code : error.getCodes()) {
                try {
                    return messageSource.getMessage(code, error.getArguments(), locale);
                } catch (Exception ignored) {
                    // Continuer avec le code suivant
                }
            }
        }

        // 2. Fallback sur le message par défaut de l'annotation
        return error.getDefaultMessage();
    }


    private String extractFieldName(String propertyPath) {
        if (propertyPath == null || propertyPath.isEmpty()) {
            return "unknown";
        }
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot >= 0 ? propertyPath.substring(lastDot + 1) : propertyPath;
    }
}
