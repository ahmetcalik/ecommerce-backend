package com.project.ecommerce_backend.core.internationalization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageManagerTest {

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private MessageManager messageManager;

    @BeforeEach
    void setUp() {
        // Set a default locale for tests
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @Test
    void getMessage_shouldReturnTranslatedMessage_whenKeyExists() {
        // Given
        String key = "test.key";
        String expectedMessage = "This is a test message.";
        when(messageSource.getMessage(eq(key), any(), eq(Locale.ENGLISH))).thenReturn(expectedMessage);

        // When
        String actualMessage = messageManager.getMessage(key);

        // Then
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void getMessage_shouldReturnKey_whenKeyDoesNotExist() {
        // Given
        String key = "non.existent.key";
        when(messageSource.getMessage(eq(key), any(), eq(Locale.ENGLISH)))
                .thenThrow(new NoSuchMessageException(key, Locale.ENGLISH));

        // When
        String actualMessage = messageManager.getMessage(key);

        // Then
        assertEquals(key, actualMessage);
    }

    @Test
    void getMessageWithParams_shouldReturnFormattedMessage_whenKeyExists() {
        // Given
        String key = "welcome.user";
        Object[] params = {"John"};
        String expectedMessage = "Welcome, John!";
        when(messageSource.getMessage(eq(key), eq(params), eq(Locale.ENGLISH))).thenReturn(expectedMessage);

        // When
        String actualMessage = messageManager.getMessageWithParams(key, params);

        // Then
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void getMessageWithParams_shouldReturnKey_whenKeyDoesNotExist() {
        // Given
        String key = "non.existent.key.with.params";
        Object[] params = {"param1"};
        when(messageSource.getMessage(eq(key), eq(params), eq(Locale.ENGLISH)))
                .thenThrow(new NoSuchMessageException(key, Locale.ENGLISH));

        // When
        String actualMessage = messageManager.getMessageWithParams(key, params);

        // Then
        assertEquals(key, actualMessage);
    }
}
