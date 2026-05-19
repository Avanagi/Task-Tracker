package com.tracker.app.tasktracker.service.factory;

import com.tracker.app.tasktracker.dto.TaskCreateDto;
import com.tracker.app.tasktracker.exception.IllegalTaskArgumentException;
import com.tracker.app.tasktracker.model.entity.tasks.AbstractTask;
import com.tracker.app.tasktracker.model.entity.tasks.BugTask;
import com.tracker.app.tasktracker.model.entity.users.User;
import com.tracker.app.tasktracker.repository.UserRepository;
import com.tracker.app.tasktracker.service.strategies.TaskCreationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskFactoryTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskCreationStrategy bugStrategy;

    private TaskFactory taskFactory;

    private User reporter;
    private TaskCreateDto dto;
    private AbstractTask mockTask;

    @BeforeEach
    void setUp() {
        taskFactory = new TaskFactory(userRepository, List.of(bugStrategy));

        reporter = User.builder().id(1L).username("Author").build();

        dto = new TaskCreateDto();
        dto.setType("BUG");
        dto.setTitle("Test Title");
        dto.setDescription("Test Description");
        dto.setDueDate(LocalDateTime.now().plusDays(2));
        dto.setStepsToReproduce("Step 1");

        mockTask = new BugTask();
    }


    @Test
    void createTask_Positive_SuccessWithAssignees() {
        dto.setAssigneeIds(List.of(2L, 3L));

        User assignee = User.builder().id(2L).username("Dev").build();

        when(bugStrategy.getType()).thenReturn("BUG");
        when(bugStrategy.create(dto)).thenReturn(mockTask);
        when(userRepository.findAllById(dto.getAssigneeIds())).thenReturn(List.of(assignee));

        AbstractTask result = taskFactory.createTask(dto, reporter);

        assertNotNull(result);
        assertEquals("Test Title", result.getTitle());
        assertEquals("Test Description", result.getDescription());
        assertEquals(reporter, result.getReporter());
        assertEquals(1, result.getAssignees().size());

        verify(bugStrategy, times(1)).create(dto);
        verify(userRepository, times(1)).findAllById(dto.getAssigneeIds());
    }

    @Test
    void createTask_Negative_UnknownTypeThrowsException() {
        dto.setType("UNKNOWN_TYPE");

        when(bugStrategy.getType()).thenReturn("BUG");

        IllegalTaskArgumentException exception = assertThrows(IllegalTaskArgumentException.class, () -> {
            taskFactory.createTask(dto, reporter);
        });

        assertEquals("Unknown task's type UNKNOWN_TYPE", exception.getMessage());
        verify(bugStrategy, never()).create(any());
    }

    @Test
    void createTask_Boundary_EmptyAssigneesList() {
        dto.setAssigneeIds(Collections.emptyList());

        when(bugStrategy.getType()).thenReturn("BUG");
        when(bugStrategy.create(dto)).thenReturn(mockTask);

        AbstractTask result = taskFactory.createTask(dto, reporter);

        assertNotNull(result);
        assertTrue(result.getAssignees().isEmpty());
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    void createTask_Boundary_NullAssigneesList() {
        dto.setAssigneeIds(null);
        when(bugStrategy.getType()).thenReturn("BUG");
        when(bugStrategy.create(dto)).thenReturn(mockTask);

        AbstractTask result = taskFactory.createTask(dto, reporter);

        assertNotNull(result);

        verify(userRepository, never()).findAllById(any());
    }
}