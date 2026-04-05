package com.project.ecommerce_backend.core.exceptions;

import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.AuthorizationBusinessException;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.exceptions.types.InvalidRefreshTokenException;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.utils.result.ErrorDataResult;
import com.project.ecommerce_backend.core.utils.result.ErrorResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.NoSuchMessageException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Uygulama genelinde fırlatılan tüm istisnaları merkezi bir noktada yakalayarak istemciye standart ve anlaşılır HTTP yanıtları üreten hata yönetim bileşenidir.
 * Aspect-Oriented Programming yaklaşımıyla çalışan bu sınıf; iş kuralları ihlalleri, veri doğrulama hataları, güvenlik kısıtlamaları ve beklenmedik sistemsel arızaları kategorize eder. Ham hata yığınlarını maskeleyerek son kullanıcıya çoklu dil desteğine sahip, güvenli ve JSON formatında yapılandırılmış hata raporları sunar.
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageService messageService;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResult> handleBusinessException(BusinessException exception) {
        log.warn("BusinessException occurred: {}", exception.getMessage());
        String translatedMessage = messageService.getMessage(exception.getMessage());
        ErrorResult errorResult = new ErrorResult(translatedMessage);
        return new ResponseEntity<>(errorResult, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthorizationBusinessException.class)
    public ResponseEntity<ErrorResult> handleAuthorizationBusinessException(AuthorizationBusinessException exception) {
        log.warn("AuthorizationBusinessException occurred: {}", exception.getMessage());
        String translatedMessage = messageService.getMessage(exception.getMessage());
        ErrorResult errorResult = new ErrorResult(translatedMessage);
        return new ResponseEntity<>(errorResult, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResult> handleNotFoundException(NotFoundException exception) {
        log.warn("NotFoundException occurred: {}", exception.getMessage());
        String message = exception.getMessage();
        ErrorResult errorResult = new ErrorResult(message);
        return new ResponseEntity<>(errorResult, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NoSuchMessageException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResult handleNoSuchMessageException(NoSuchMessageException exception) {
        log.error("Missing Message Key: {}", exception.getMessage());
        String message = messageService.getMessage(Messages.Errors.UNEXPECTED_ERROR_OCCURRED);
        return new ErrorResult(message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDataResult<Map<String, String>>> handleValidationException(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            String translatedMessage = messageService.getMessage(fieldError.getDefaultMessage());
            errors.put(fieldError.getField(), translatedMessage);
        }
        String errorDetails = errors.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
        log.warn("MethodArgumentNotValidException occurred: Fields [{}]", errorDetails);
        ErrorDataResult<Map<String, String>> errorDataResult = new ErrorDataResult<>(
                errors,
                messageService.getMessage(Messages.Errors.VALIDATION_ERROR)
        );
        return new ResponseEntity<>(errorDataResult, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ErrorResult> handleConstraintViolationException(jakarta.validation.ConstraintViolationException exception) {
        log.warn("ConstraintViolationException occurred: {}", exception.getMessage());
        ErrorResult errorResult = new ErrorResult(exception.getMessage());
        return new ResponseEntity<>(errorResult, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResult> handleDataIntegrityViolationException(DataIntegrityViolationException exception) {
        String message;
        if (exception.getMostSpecificCause().getMessage().contains("category_name_key")) {
            message = messageService.getMessage(Messages.Category.CATEGORY_ALREADY_EXIST);
        } else {
            message = messageService.getMessage(Messages.Errors.DATA_INTEGRITY_VIOLATION);
        }
        log.warn("DataIntegrityViolationException occurred: Constraint [{}], Message: {}",
                exception.getMostSpecificCause().getMessage(), message);
        ErrorResult errorResult = new ErrorResult(message);
        return new ResponseEntity<>(errorResult, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResult> handleInvalidRefreshTokenException(InvalidRefreshTokenException exception) {
        log.warn("InvalidRefreshTokenException occurred: {}", exception.getMessage());
        String translatedMessage = messageService.getMessage(exception.getMessage());
        ErrorResult errorResult = new ErrorResult(translatedMessage);
        return new ResponseEntity<>(errorResult, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResult> handleAuthenticationException(AuthenticationException exception) {
        log.warn("AuthenticationException occurred: {}", exception.getMessage());
        ErrorResult errorResult = new ErrorResult(messageService.getMessage(
                Messages.Errors.AUTHENTICATION_FAILED));
        return new ResponseEntity<>(errorResult, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResult> handleAccessDeniedException(AccessDeniedException exception) {
        log.warn("AccessDeniedException occurred: {}", exception.getMessage());
        ErrorResult errorResult = new ErrorResult(messageService.getMessage(
                Messages.Errors.ACCESS_DENIED)); // Keep generic message for user
        return new ResponseEntity<>(errorResult, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResult> handleOptimisticLockingFailureException(ObjectOptimisticLockingFailureException exception) {
        log.warn("Optimistic Locking conflict detected: {}", exception.getMessage());
        ErrorResult errorResult = new ErrorResult(messageService.getMessage(
                Messages.Errors.CONCURRENT_UPDATE_DETECTED));
        return new ResponseEntity<>(errorResult, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResult> handleAllUncaughtExceptions(Exception exception) {
        log.error(messageService.getMessage(Messages.Errors.UNEXPECTED_ERROR_OCCURRED), exception);
        ErrorResult errorResult = new ErrorResult(messageService.getMessage(
                Messages.Errors.UNEXPECTED_ERROR_OCCURRED));
        return new ResponseEntity<>(errorResult, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}