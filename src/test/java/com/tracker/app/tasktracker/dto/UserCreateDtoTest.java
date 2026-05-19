package com.tracker.app.tasktracker.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserCreateDtoTest {

    private static Validator validator;
    private UserCreateDto dto;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @BeforeEach
    void setUp() {
        dto = new UserCreateDto();
        dto.setUsername("ValidUser_1");
        dto.setEmail("user@example.com");
        dto.setPassword("StrongPass1!");
    }

    @Test
    void validate_Positive_ValidUserDto() {
        Set<ConstraintViolation<UserCreateDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Ожидалось 0 ошибок валидации");
    }

    @Test
    void validate_Negative_InvalidFormats() {
        dto.setEmail("invalid-email");
        dto.setPassword("weak");

        Set<ConstraintViolation<UserCreateDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());

        assertTrue(violations.size() >= 2);
    }

    @Test
    void validate_Boundary_UsernameLength() {
        dto.setUsername("usr");
        assertTrue(validator.validate(dto).isEmpty());

        dto.setUsername("us");
        assertFalse(validator.validate(dto).isEmpty());

        dto.setUsername("a".repeat(30));
        assertTrue(validator.validate(dto).isEmpty());

        dto.setUsername("a".repeat(31));
        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void validate_Boundary_UsernameRegex() {
        dto.setUsername("aZ_9");
        assertTrue(validator.validate(dto).isEmpty());

        dto.setUsername("User Name");
        Set<ConstraintViolation<UserCreateDto>> violationsSpace = validator.validate(dto);
        assertFalse(violationsSpace.isEmpty());

        dto.setUsername("User-Name");
        Set<ConstraintViolation<UserCreateDto>> violationsDash = validator.validate(dto);
        assertFalse(violationsDash.isEmpty());
    }
}