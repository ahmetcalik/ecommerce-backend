package com.project.ecommerce_backend.core.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.AuthorizationBusinessException;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.exceptions.types.InvalidRefreshTokenException;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.NoSuchMessageException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.ConstraintViolationException;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    // A mock DTO for validation testing
    @Data
    public static class TestRequest {
        @NotBlank(message = "FIELD_CANNOT_BE_BLANK")
        private String name;
    }

    // A mock controller to throw exceptions
    @RestController
    public static class TestController {
        @GetMapping("/business")
        public void throwBusinessException() {
            throw new BusinessException("BUSINESS_ERROR");
        }

        @GetMapping("/authorization")
        public void throwAuthorizationBusinessException() {
            throw new AuthorizationBusinessException("AUTH_ERROR");
        }

        @GetMapping("/notfound")
        public void throwNotFoundException() {
            throw new NotFoundException("NOT_FOUND_ERROR");
        }

        @GetMapping("/nosuchmessage")
        public void throwNoSuchMessageException() {
            throw new NoSuchMessageException("MISSING_KEY");
        }

        @PostMapping("/validation")
        public void triggerValidation(@Valid @RequestBody TestRequest request) {
            if (request == null) {
                throw new IllegalArgumentException();
            }
        }

        @GetMapping("/constraint")
        public void throwConstraintViolationException() {
            throw new ConstraintViolationException("CONSTRAINT_ERROR", Collections.emptySet());
        }

        @GetMapping("/dataintegrity")
        public void throwDataIntegrityViolationException() {
            throw new DataIntegrityViolationException("general db error", new RuntimeException("some root cause"));
        }

        @GetMapping("/dataintegrity-category")
        public void throwDataIntegrityViolationExceptionCategory() {
            throw new DataIntegrityViolationException("category error", new RuntimeException("duplicate key value violates unique constraint \"category_name_key\""));
        }

        @GetMapping("/invalidrefresh")
        public void throwInvalidRefreshTokenException() {
            throw new InvalidRefreshTokenException("INVALID_TOKEN");
        }

        @GetMapping("/authentication")
        public void throwAuthenticationException() {
            throw new BadCredentialsException("AUTH_FAIL");
        }

        @GetMapping("/access-denied")
        public void throwAccessDeniedException() {
            throw new AccessDeniedException("ACCESS_DENIED_ERROR");
        }

        @GetMapping("/optimistic")
        public void throwOptimisticLockingException() {
            throw new ObjectOptimisticLockingFailureException(Object.class, "id");
        }

        @GetMapping("/general")
        public void throwGeneralException() {
            throw new RuntimeException("GENERAL_ERROR");
        }
    }

    @BeforeEach
    void setUp() {
        JacksonTester.initFields(this, new ObjectMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(globalExceptionHandler)
                .build();
    }

    @Test
    void handleBusinessException_shouldReturnBadRequest() throws Exception {
        when(messageService.getMessage("BUSINESS_ERROR")).thenReturn("A business error occurred.");

        mockMvc.perform(get("/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("A business error occurred."));
    }

    @Test
    void handleAuthorizationBusinessException_shouldReturnForbidden() throws Exception {
        when(messageService.getMessage("AUTH_ERROR")).thenReturn("Authorization failed.");

        mockMvc.perform(get("/authorization"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authorization failed."));
    }

    @Test
    void handleNotFoundException_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/notfound"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("NOT_FOUND_ERROR"));
    }

    @Test
    void handleNoSuchMessageException_shouldReturnInternalServerError() throws Exception {
        when(messageService.getMessage(Messages.Errors.UNEXPECTED_ERROR_OCCURRED))
                .thenReturn("An unexpected error occurred.");

        mockMvc.perform(get("/nosuchmessage"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."));
    }

    @Test
    void handleValidationException_shouldReturnBadRequestWithFieldErrors() throws Exception {
        when(messageService.getMessage("FIELD_CANNOT_BE_BLANK")).thenReturn("Field cannot be blank.");
        when(messageService.getMessage(Messages.Errors.VALIDATION_ERROR)).thenReturn("Validation Error");

        String requestBody = "{\"name\":\"\"}";

        mockMvc.perform(post("/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation Error"))
                .andExpect(jsonPath("$.data.name").value("Field cannot be blank."));
    }

    @Test
    void handleConstraintViolationException_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("CONSTRAINT_ERROR"));
    }

    @Test
    void handleDataIntegrityViolationException_shouldReturnGeneralError() throws Exception {
        when(messageService.getMessage(Messages.Errors.DATA_INTEGRITY_VIOLATION))
                .thenReturn("Data integrity violation.");

        mockMvc.perform(get("/dataintegrity"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Data integrity violation."));
    }

    @Test
    void handleDataIntegrityViolationException_shouldReturnCategoryExistsError() throws Exception {
        when(messageService.getMessage(Messages.Category.CATEGORY_ALREADY_EXIST))
                .thenReturn("Category already exists.");

        mockMvc.perform(get("/dataintegrity-category"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Category already exists."));
    }

    @Test
    void handleInvalidRefreshTokenException_shouldReturnUnauthorized() throws Exception {
        when(messageService.getMessage("INVALID_TOKEN")).thenReturn("Invalid refresh token.");

        mockMvc.perform(get("/invalidrefresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid refresh token."));
    }

    @Test
    void handleAuthenticationException_shouldReturnUnauthorized() throws Exception {
        when(messageService.getMessage(Messages.Errors.AUTHENTICATION_FAILED))
                .thenReturn("Authentication failed.");

        mockMvc.perform(get("/authentication"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication failed."));
    }

    @Test
    void handleAccessDeniedException_shouldReturnForbidden() throws Exception {
        when(messageService.getMessage(Messages.Errors.ACCESS_DENIED)).thenReturn("Access Denied");

        mockMvc.perform(get("/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access Denied"));
    }

    @Test
    void handleOptimisticLockingFailureException_shouldReturnConflict() throws Exception {
        when(messageService.getMessage(Messages.Errors.CONCURRENT_UPDATE_DETECTED))
                .thenReturn("Concurrent update detected.");

        mockMvc.perform(get("/optimistic"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Concurrent update detected."));
    }

    @Test
    void handleAllUncaughtExceptions_shouldReturnInternalServerError() throws Exception {
        when(messageService.getMessage(Messages.Errors.UNEXPECTED_ERROR_OCCURRED))
                .thenReturn("An unexpected error occurred.");

        mockMvc.perform(get("/general"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."));
    }
}