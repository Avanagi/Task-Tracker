package com.tracker.app.tasktracker.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private MethodArgumentNotValidException validationException;

    @Mock
    private BindingResult bindingResult;


    @Test
    void handleValidationExceptions_Positive_Success() {
        FieldError error1 = new FieldError("userDto", "username", "Username is required");
        FieldError error2 = new FieldError("userDto", "password", "Password too short");

        when(validationException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleValidationExceptions(validationException);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("Username is required", response.getBody().get("username"));
        assertEquals("Password too short", response.getBody().get("password"));
    }

    @Test
    void handleValidationExceptions_Negative_NullException() {
        assertThrows(NullPointerException.class, () -> exceptionHandler.handleValidationExceptions(null));
    }

    @Test
    void handleValidationExceptions_Boundary_EmptyFieldErrors() {
        when(validationException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(Collections.emptyList());

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleValidationExceptions(validationException);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty(), "Тело ответа должно быть пустым Map");
    }

    @Test
    void handleValidationExceptions_Boundary_NullErrorMessage() {
        FieldError errorWithoutMsg = new FieldError("userDto", "email", null);

        when(validationException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(errorWithoutMsg));

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleValidationExceptions(validationException);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("email"));
        assertNull(response.getBody().get("email"), "Значение ключа 'email' должно быть null");
    }

    @Test
    void handleIllegalArgument_Positive_Success() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid task type");

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleIllegalArgument(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid task type", response.getBody().get("message"));
    }

    @Test
    void handleIllegalArgument_Negative_NullException() {
        assertThrows(NullPointerException.class, () -> exceptionHandler.handleIllegalArgument(null));
    }

    @Test
    void handleIllegalArgument_Boundary_NullMessage() {
        IllegalArgumentException exception = new IllegalArgumentException((String) null);

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleIllegalArgument(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertNull(response.getBody().get("message"));
    }

    @Test
    void handleIllegalArgument_Boundary_EmptyMessage() {
        IllegalArgumentException exception = new IllegalArgumentException("");

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleIllegalArgument(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("", response.getBody().get("message"));
    }
}