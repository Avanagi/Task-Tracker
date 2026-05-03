package com.tracker.app.tasktracker.model.entity.tasks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "subtasks")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@JsonIgnoreProperties({"epic"})
@Setter
public class Subtask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Subtask title is required")
    @Size(min = 3, max = 255, message = "Subtask title must be between 3 and 255 characters")
    @Column(nullable = false)
    private String title;

    @NotNull(message = "Completion status is required")
    private boolean completed;
}