package com.tracker.app.tasktracker.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TaskCreateDtoTest {

    private static Validator validator;
    private TaskCreateDto dto;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @BeforeEach
    void setUp() {
        dto = new TaskCreateDto();
        dto.setType("BUG");
        dto.setTitle("Valid Title");
        dto.setDueDate(LocalDateTime.now().plusDays(5));
        dto.setReporterId(1L);
    }

    @Test
    void validate_Positive_ValidDto() {
        Set<ConstraintViolation<TaskCreateDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Ожидалось 0 ошибок валидации");
    }

    @Test
    void validate_Negative_MultipleInvalidFields() {
        dto.setType("UNKNOWN");
        dto.setTitle("");
        dto.setDueDate(LocalDateTime.now().minusDays(1));
        dto.setReporterId(-5L);

        Set<ConstraintViolation<TaskCreateDto>> violations = validator.validate(dto);

        assertEquals(4, violations.size());
    }

    @Test
    void validate_Boundary_TitleMaxLength() {
        dto.setTitle("A".repeat(255));
        assertTrue(validator.validate(dto).isEmpty());

        dto.setTitle("A".repeat(256));
        Set<ConstraintViolation<TaskCreateDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertEquals("Title must not exceed 255 characters", violations.iterator().next().getMessage());
    }

    @Test
    void validate_Boundary_AssigneesMaxListSize() {
        List<Long> validAssignees = Collections.nCopies(20, 1L);
        dto.setAssigneeIds(validAssignees);
        assertTrue(validator.validate(dto).isEmpty());

        List<Long> invalidAssignees = Collections.nCopies(21, 1L);
        dto.setAssigneeIds(invalidAssignees);

        Set<ConstraintViolation<TaskCreateDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertEquals("Cannot assign more than 20 users", violations.iterator().next().getMessage());
    }
}