package com.tracker.app.tasktracker.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskCreateDto {

    @NotBlank(message = "Type is required")
    @Pattern(regexp = "^(TASK|BUG|EPIC)$",
            message = "Type must be TASK, BUG, or EPIC")
    private String type;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Due date is required")
    @Future(message = "Due date must be in the future")
    private LocalDateTime dueDate;

    @NotNull(message = "Reporter ID is required")
    @Positive(message = "Reporter ID must be positive")
    private Long reporterId;

    @Size(max = 5000, message = "Steps to reproduce must not exceed 5000 characters")
    private String stepsToReproduce;

    @Size(max = 50, message = "Cannot have more than 50 subtasks")
    private List<String> subtaskTitles;

    @Size(max = 20, message = "Cannot assign more than 20 users")
    private List<@Positive(message = "Each assignee ID must be positive") Long> assigneeIds;
}