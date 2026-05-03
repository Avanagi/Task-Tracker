package com.tracker.app.tasktracker.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserCreateDto {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$",
            message = "Username can only contain letters, numbers, and underscores")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Pattern(regexp = ".+@.+\\..+", message = "Email must contain domain (e.g., user@example.com)")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
            message = "Password must be at least 8 characters, contain one uppercase letter and one digit")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;
}