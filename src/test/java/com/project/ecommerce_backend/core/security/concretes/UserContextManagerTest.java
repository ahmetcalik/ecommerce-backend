package com.project.ecommerce_backend.core.security.concretes;

import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.security.details.CustomerDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserContextManagerTest {

    @Mock
    private MessageService messageService;

    @InjectMocks
    private UserContextManager userContextManager;

    private SecurityContext securityContext;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAuthenticatedUserId_shouldReturnUserId_whenPrincipalIsCustomerDetails() {
        // Given
        Long expectedUserId = 123L;
        CustomerDetails customerDetails = mock(CustomerDetails.class);
        when(customerDetails.getId()).thenReturn(expectedUserId);
        when(authentication.getPrincipal()).thenReturn(customerDetails);

        // When
        Long actualUserId = userContextManager.getAuthenticatedUserId();

        // Then
        assertEquals(expectedUserId, actualUserId);
    }

    @Test
    void getAuthenticatedUserId_shouldThrowBusinessException_whenPrincipalIsNotCustomerDetails() {
        // Given
        Object invalidPrincipal = new Object(); // Not an instance of CustomerDetails
        when(authentication.getPrincipal()).thenReturn(invalidPrincipal);
        when(messageService.getMessage(Messages.Auth.AUTHENTICATION_PRINCIPAL_INVALID))
                .thenReturn("Authentication principal is invalid");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userContextManager.getAuthenticatedUserId();
        });

        assertEquals("Authentication principal is invalid", exception.getMessage());
    }

    @Test
    void getAuthenticatedUserId_shouldThrowBusinessException_whenPrincipalIsString() {
        // Given
        // Spring Security often returns "anonymousUser" as a string for unauthenticated users
        String anonymousUserPrincipal = "anonymousUser";
        when(authentication.getPrincipal()).thenReturn(anonymousUserPrincipal);
        when(messageService.getMessage(Messages.Auth.AUTHENTICATION_PRINCIPAL_INVALID))
                .thenReturn("Authentication principal is invalid");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userContextManager.getAuthenticatedUserId();
        });

        assertEquals("Authentication principal is invalid", exception.getMessage());
    }
}
