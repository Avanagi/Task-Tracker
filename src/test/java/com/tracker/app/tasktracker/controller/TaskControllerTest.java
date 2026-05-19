package com.tracker.app.tasktracker.controller;

import com.tracker.app.tasktracker.dto.TaskCreateDto;
import com.tracker.app.tasktracker.model.entity.tasks.AbstractTask;
import com.tracker.app.tasktracker.model.entity.tasks.SimpleTask;
import com.tracker.app.tasktracker.model.enums.TaskStatus;
import com.tracker.app.tasktracker.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @Mock
    private Principal principal;

    @InjectMocks
    private TaskController taskController;

    private AbstractTask mockTask;
    private TaskCreateDto dto;
    private final String USERNAME = "TestUser";

    @BeforeEach
    void setUp() {
        mockTask = new SimpleTask();
        mockTask.setId(1L);
        mockTask.setTitle("Test Title");

        dto = new TaskCreateDto();
        dto.setTitle("Test Title");
        dto.setType("TASK");

        when(principal.getName()).thenReturn(USERNAME);
    }

    @Test
    void getAllTasks_Positive_Success() {
        when(taskService.getAllTasks()).thenReturn(List.of(mockTask, new SimpleTask()));

        ResponseEntity<List<AbstractTask>> response = taskController.getAllTasks();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assert response.getBody() != null;
        assertEquals(2, response.getBody().size());
        verify(taskService, times(1)).getAllTasks();
    }

    @Test
    void getAllTasks_Negative_ServiceThrowsException() {
        when(taskService.getAllTasks()).thenThrow(new RuntimeException("DB Error"));

        assertThrows(RuntimeException.class, () -> taskController.getAllTasks());
        verify(taskService, times(1)).getAllTasks();
    }

    @Test
    void getAllTasks_Boundary_EmptyList() {
        when(taskService.getAllTasks()).thenReturn(Collections.emptyList());

        ResponseEntity<List<AbstractTask>> response = taskController.getAllTasks();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assert response.getBody() != null;
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getAllTasks_Boundary_SingleTask() {
        when(taskService.getAllTasks()).thenReturn(List.of(mockTask));

        ResponseEntity<List<AbstractTask>> response = taskController.getAllTasks();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assert response.getBody() != null;
        assertEquals(1, response.getBody().size());
    }

    @Test
    void createTask_Positive_Success() {
        when(taskService.createTask(dto, USERNAME)).thenReturn(mockTask);

        ResponseEntity<AbstractTask> response = taskController.createTask(dto, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockTask, response.getBody());
        verify(taskService, times(1)).createTask(dto, USERNAME);
    }

    @Test
    void createTask_Negative_ServiceThrowsException() {
        when(taskService.createTask(dto, USERNAME)).thenThrow(new IllegalArgumentException("Invalid DTO"));

        assertThrows(IllegalArgumentException.class, () -> taskController.createTask(dto, principal));
    }

    @Test
    void createTask_Boundary_NullPrincipal() {
        assertThrows(NullPointerException.class, () -> taskController.createTask(dto, null));
        verify(taskService, never()).createTask(any(), any());
    }

    @Test
    void createTask_Boundary_EmptyPrincipalName() {
        when(principal.getName()).thenReturn("");
        when(taskService.createTask(dto, "")).thenReturn(mockTask);

        ResponseEntity<AbstractTask> response = taskController.createTask(dto, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(taskService, times(1)).createTask(dto, "");
    }

    @Test
    void changeStatus_Positive_Success() {
        when(taskService.changeStatus(1L, TaskStatus.IN_PROGRESS, USERNAME)).thenReturn(mockTask);

        ResponseEntity<AbstractTask> response = taskController.changeStatus(1L, TaskStatus.IN_PROGRESS, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockTask, response.getBody());
    }

    @Test
    void changeStatus_Negative_Forbidden() {
        when(taskService.changeStatus(1L, TaskStatus.DONE, USERNAME))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class, () -> taskController.changeStatus(1L, TaskStatus.DONE, principal));
    }

    @Test
    void changeStatus_Boundary_NullPrincipal() {
        assertThrows(NullPointerException.class, () -> taskController.changeStatus(1L, TaskStatus.IN_PROGRESS, null));
        verify(taskService, never()).changeStatus(any(), any(), any());
    }

    @Test
    void changeStatus_Boundary_EmptyPrincipalName() {
        when(principal.getName()).thenReturn("");
        when(taskService.changeStatus(1L, TaskStatus.TODO, "")).thenReturn(mockTask);

        ResponseEntity<AbstractTask> response = taskController.changeStatus(1L, TaskStatus.TODO, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void assignUser_Positive_Success() {
        when(taskService.assignUser(1L, 2L, USERNAME)).thenReturn(mockTask);

        ResponseEntity<AbstractTask> response = taskController.assignUser(1L, 2L, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockTask, response.getBody());
    }

    @Test
    void assignUser_Negative_ServiceThrowsException() {
        when(taskService.assignUser(1L, 999L, USERNAME)).thenThrow(new RuntimeException("User not found"));

        assertThrows(RuntimeException.class, () -> taskController.assignUser(1L, 999L, principal));
    }

    @Test
    void assignUser_Boundary_NullPrincipal() {
        assertThrows(NullPointerException.class, () -> taskController.assignUser(1L, 2L, null));
        verify(taskService, never()).assignUser(any(), any(), any());
    }

    @Test
    void assignUser_Boundary_EmptyPrincipalName() {
        when(principal.getName()).thenReturn("");
        when(taskService.assignUser(1L, 2L, "")).thenReturn(mockTask);

        ResponseEntity<AbstractTask> response = taskController.assignUser(1L, 2L, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void assignBulk_Positive_Success() {
        List<Long> ids = List.of(2L, 3L);
        when(taskService.assignBulk(1L, ids, USERNAME)).thenReturn(mockTask);

        ResponseEntity<AbstractTask> response = taskController.assignBulk(1L, ids, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockTask, response.getBody());
    }

    @Test
    void assignBulk_Negative_Forbidden() {
        when(taskService.assignBulk(1L, List.of(2L), USERNAME))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class, () -> taskController.assignBulk(1L, List.of(2L), principal));
    }

    @Test
    void assignBulk_Boundary_NullListPassed() {
        when(taskService.assignBulk(1L, null, USERNAME)).thenReturn(mockTask);

        ResponseEntity<AbstractTask> response = taskController.assignBulk(1L, null, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(taskService, times(1)).assignBulk(1L, null, USERNAME);
    }

    @Test
    void assignBulk_Boundary_EmptyListPassed() {
        when(taskService.assignBulk(1L, Collections.emptyList(), USERNAME)).thenReturn(mockTask);

        ResponseEntity<AbstractTask> response = taskController.assignBulk(1L, Collections.emptyList(), principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(taskService, times(1)).assignBulk(1L, Collections.emptyList(), USERNAME);
    }

    @Test
    void toggleSubtask_Positive_Success() {
        when(taskService.toggleSubtask(1L, 100L, USERNAME)).thenReturn(mockTask);

        ResponseEntity<AbstractTask> response = taskController.toggleSubtask(1L, 100L, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void toggleSubtask_Negative_NotEpic() {
        when(taskService.toggleSubtask(1L, 100L, USERNAME))
                .thenThrow(new IllegalArgumentException("Not an epic"));

        assertThrows(IllegalArgumentException.class, () -> taskController.toggleSubtask(1L, 100L, principal));
    }

    @Test
    void toggleSubtask_Boundary_NullPrincipal() {
        assertThrows(NullPointerException.class, () -> taskController.toggleSubtask(1L, 100L, null));
        verify(taskService, never()).toggleSubtask(any(), any(), any());
    }

    @Test
    void toggleSubtask_Boundary_EmptyPrincipalName() {
        when(principal.getName()).thenReturn("");
        when(taskService.toggleSubtask(1L, 100L, "")).thenReturn(mockTask);

        ResponseEntity<AbstractTask> response = taskController.toggleSubtask(1L, 100L, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateDeadline_Positive_Success() {
        String newDate = "2026-12-31T23:59:00";
        when(taskService.updateDeadline(1L, newDate, USERNAME)).thenReturn(mockTask);

        ResponseEntity<AbstractTask> response = taskController.updateDeadline(1L, newDate, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateDeadline_Negative_ServiceThrowsException() {
        when(taskService.updateDeadline(1L, "InvalidDate", USERNAME))
                .thenThrow(new java.time.format.DateTimeParseException("Text cannot be parsed", "InvalidDate", 0));

        assertThrows(java.time.format.DateTimeParseException.class, () -> taskController.updateDeadline(1L, "InvalidDate", principal));
    }

    @Test
    void updateDeadline_Boundary_NullPrincipal() {
        assertThrows(NullPointerException.class, () -> taskController.updateDeadline(1L, "2026-12-31T23:59:00", null));
        verify(taskService, never()).updateDeadline(any(), any(), any());
    }

    @Test
    void updateDeadline_Boundary_EmptyDateString() {
        when(taskService.updateDeadline(1L, "", USERNAME)).thenThrow(new IllegalArgumentException("Empty date"));

        assertThrows(IllegalArgumentException.class, () -> taskController.updateDeadline(1L, "", principal));
    }

    @Test
    void deleteTask_Positive_Success() {
        doNothing().when(taskService).deleteTask(1L, USERNAME);

        ResponseEntity<Void> response = taskController.deleteTask(1L, principal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(taskService, times(1)).deleteTask(1L, USERNAME);
    }

    @Test
    void deleteTask_Negative_Forbidden() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(taskService).deleteTask(1L, USERNAME);

        assertThrows(ResponseStatusException.class, () -> taskController.deleteTask(1L, principal));
    }

    @Test
    void deleteTask_Boundary_NullPrincipal() {
        assertThrows(NullPointerException.class, () -> taskController.deleteTask(1L, null));
        verify(taskService, never()).deleteTask(any(), any());
    }

    @Test
    void deleteTask_Boundary_EmptyPrincipalName() {
        when(principal.getName()).thenReturn("");
        doNothing().when(taskService).deleteTask(1L, "");

        ResponseEntity<Void> response = taskController.deleteTask(1L, principal);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}