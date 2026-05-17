package com.tracker.app.tasktracker.service.impl;

import com.tracker.app.tasktracker.dto.TaskCreateDto;
import com.tracker.app.tasktracker.event.*;
import com.tracker.app.tasktracker.exception.*;
import com.tracker.app.tasktracker.model.entity.tasks.*;
import com.tracker.app.tasktracker.model.entity.users.User;
import com.tracker.app.tasktracker.model.enums.TaskStatus;
import com.tracker.app.tasktracker.repository.TaskRepository;
import com.tracker.app.tasktracker.repository.UserRepository;
import com.tracker.app.tasktracker.service.factory.TaskFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private TaskFactory taskFactory;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private TaskServiceImpl taskService;

    private User author;
    private User assigneeUser;
    private SimpleTask task;
    private EpicTask epicTask;

    @BeforeEach
    void setUp() {
        author = User.builder().id(1L).username("Author").build();
        assigneeUser = User.builder().id(2L).username("Assignee").build();

        task = new SimpleTask();
        task.setId(10L);
        task.setReporter(author);
        task.setStatus(TaskStatus.TODO);
        task.setDueDate(LocalDateTime.now().plusDays(5));
        task.setAssignees(new HashSet<>(List.of(assigneeUser)));

        epicTask = new EpicTask();
        epicTask.setId(20L);
        epicTask.setReporter(author);
        epicTask.setStatus(TaskStatus.TODO);
        epicTask.setDueDate(LocalDateTime.now().plusDays(5));
        epicTask.setAssignees(new HashSet<>());
        Subtask subtask = new Subtask(100L, "Test Subtask", false);
        epicTask.setSubtasks(List.of(subtask));
    }

    // ==========================================
    // 1. МЕТОД: createTask
    // ==========================================

    @Test // ПОЗИТИВНЫЙ
    void createTask_Positive_Success() {
        TaskCreateDto dto = new TaskCreateDto();
        dto.setType("TASK");

        when(userRepository.findByUsername("Author")).thenReturn(Optional.of(author));
        when(taskFactory.createTask(dto, author)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);

        AbstractTask result = taskService.createTask(dto, "Author");

        assertNotNull(result);
        verify(eventPublisher, times(1)).publishEvent(any(TaskCreatedEvent.class));
    }

    @Test // НЕГАТИВНЫЙ (Пользователь не найден)
    void createTask_Negative_UserNotFound() {
        when(userRepository.findByUsername("Ghost")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> taskService.createTask(new TaskCreateDto(), "Ghost"));
        verify(taskRepository, never()).save(any());
    }

    @Test // ГРАНИЧНЫЙ 1 (Создание с экстремально далеким дедлайном)
    void createTask_Boundary_FarFutureDeadline() {
        TaskCreateDto dto = new TaskCreateDto();
        task.setDueDate(LocalDateTime.of(3000, 1, 1, 0, 0)); // 3000 год

        when(userRepository.findByUsername("Author")).thenReturn(Optional.of(author));
        when(taskFactory.createTask(dto, author)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);

        assertDoesNotThrow(() -> taskService.createTask(dto, "Author"));
    }

    @Test // ГРАНИЧНЫЙ 2 (Пустой DTO - проверка отработки фабрики)
    void createTask_Boundary_EmptyDto() {
        when(userRepository.findByUsername("Author")).thenReturn(Optional.of(author));
        when(taskFactory.createTask(any(), eq(author))).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);

        assertDoesNotThrow(() -> taskService.createTask(new TaskCreateDto(), "Author"));
    }

    // ==========================================
    // 2. МЕТОД: changeStatus
    // ==========================================

    @Test // ПОЗИТИВНЫЙ
    void changeStatus_Positive_AuthorChangesToInProgress() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("Author")).thenReturn(Optional.of(author));
        when(taskRepository.save(any())).thenReturn(task);

        AbstractTask result = taskService.changeStatus(10L, TaskStatus.IN_PROGRESS, "Author");

        assertEquals(TaskStatus.IN_PROGRESS, result.getStatus());
        verify(eventPublisher, times(1)).publishEvent(any(TaskStatusChangedEvent.class));
    }

    @Test // НЕГАТИВНЫЙ (Исполнитель пытается поставить DONE)
    void changeStatus_Negative_AssigneeCannotSetDone() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("Assignee")).thenReturn(Optional.of(assigneeUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> taskService.changeStatus(10L, TaskStatus.DONE, "Assignee"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test // ГРАНИЧНЫЙ 1 (Смена статуса у просроченной задачи АВТОРОМ - разрешено)
    void changeStatus_Boundary_OverdueTaskByAuthor() {
        task.setDueDate(LocalDateTime.now().minusDays(1)); // Просрочено
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("Author")).thenReturn(Optional.of(author));
        when(taskRepository.save(task)).thenReturn(task);

        assertDoesNotThrow(() -> taskService.changeStatus(10L, TaskStatus.REVIEW, "Author"));
    }

    @Test // ГРАНИЧНЫЙ 2 (Смена статуса у DONE задачи на REVIEW - запрещено)
    void changeStatus_Boundary_DoneTaskToReview() {
        task.setStatus(TaskStatus.DONE);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("Author")).thenReturn(Optional.of(author));

        assertThrows(ResponseStatusException.class, () -> taskService.changeStatus(10L, TaskStatus.REVIEW, "Author"));
    }

    // ==========================================
    // 3. МЕТОД: assignUser
    // ==========================================

    @Test // ПОЗИТИВНЫЙ
    void assignUser_Positive_AuthorAssignsNewUser() {
        User newUser = User.builder().id(3L).username("NewUser").build();
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("Author")).thenReturn(Optional.of(author));
        when(userRepository.findById(3L)).thenReturn(Optional.of(newUser));
        when(taskRepository.save(task)).thenReturn(task);

        AbstractTask result = taskService.assignUser(10L, 3L, "Author");
        assertTrue(result.getAssignees().contains(newUser));
    }

    @Test // НЕГАТИВНЫЙ (Исполнитель пытается назначить другого пользователя)
    void assignUser_Negative_AssigneeCannotAssign() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("Assignee")).thenReturn(Optional.of(assigneeUser));

        assertThrows(ResponseStatusException.class, () -> taskService.assignUser(10L, 3L, "Assignee"));
    }

    @Test // ГРАНИЧНЫЙ 1 (Назначение уже назначенного пользователя)
    void assignUser_Boundary_AssignAlreadyAssignedUser() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("Author")).thenReturn(Optional.of(author));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assigneeUser)); // ID 2 уже есть
        when(taskRepository.save(task)).thenReturn(task);

        AbstractTask result = taskService.assignUser(10L, 2L, "Author");
        assertEquals(1, result.getAssignees().size()); // Размер Set не увеличился
    }

    @Test // ГРАНИЧНЫЙ 2 (Назначение пользователя в задачу со статусом DONE)
    void assignUser_Boundary_AssignToDoneTask() {
        task.setStatus(TaskStatus.DONE);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThrows(ResponseStatusException.class, () -> taskService.assignUser(10L, 3L, "Author"));
    }

    // ==========================================
    // 4. МЕТОД: assignBulk
    // ==========================================

    @Test // ПОЗИТИВНЫЙ
    void assignBulk_Positive_Success() {
        User u3 = User.builder().id(3L).username("U3").build();
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("Author")).thenReturn(Optional.of(author));
        when(userRepository.findAllById(List.of(3L))).thenReturn(List.of(u3));
        when(taskRepository.save(task)).thenReturn(task);

        taskService.assignBulk(10L, List.of(3L), "Author");
        verify(eventPublisher, atLeastOnce()).publishEvent(any(TaskAssigneesChangedEvent.class));
    }

    @Test // НЕГАТИВНЫЙ (Попытка чужим юзером)
    void assignBulk_Negative_Forbidden() {
        User stranger = User.builder().id(99L).build();
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("Stranger")).thenReturn(Optional.of(stranger));

        assertThrows(ResponseStatusException.class, () -> taskService.assignBulk(10L, List.of(1L), "Stranger"));
    }

    @Test // ГРАНИЧНЫЙ 1 (Пустой список - очистка исполнителей)
    void assignBulk_Boundary_EmptyList() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("Author")).thenReturn(Optional.of(author));
        when(userRepository.findAllById(Collections.emptyList())).thenReturn(Collections.emptyList());
        when(taskRepository.save(task)).thenReturn(task);

        AbstractTask result = taskService.assignBulk(10L, Collections.emptyList(), "Author");
        assertTrue(result.getAssignees().isEmpty());
    }

    @Test // ГРАНИЧНЫЙ 2 (Передача списка с дубликатами ID)
    void assignBulk_Boundary_DuplicateIdsInList() {
        User u3 = User.builder().id(3L).build();
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("Author")).thenReturn(Optional.of(author));
        // JPA вернет один объект, даже если запросили ID дважды
        when(userRepository.findAllById(List.of(3L, 3L))).thenReturn(List.of(u3));
        when(taskRepository.save(task)).thenReturn(task);

        AbstractTask result = taskService.assignBulk(10L, List.of(3L, 3L), "Author");
        assertEquals(1, result.getAssignees().size()); // Set отфильтровал
    }

    // ==========================================
    // 5. МЕТОД: deleteTask
    // ==========================================

    @Test // ПОЗИТИВНЫЙ
    void deleteTask_Positive_Success() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("Author")).thenReturn(Optional.of(author));

        assertDoesNotThrow(() -> taskService.deleteTask(10L, "Author"));
        verify(taskRepository, times(1)).deleteById(10L);
        verify(eventPublisher, times(1)).publishEvent(any(TaskDeletedEvent.class));
    }

    @Test // НЕГАТИВНЫЙ (Удаление не автором)
    void deleteTask_Negative_Forbidden() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("Assignee")).thenReturn(Optional.of(assigneeUser));

        assertThrows(ResponseStatusException.class, () -> taskService.deleteTask(10L, "Assignee"));
        verify(taskRepository, never()).deleteById(any());
    }

    @Test // ГРАНИЧНЫЙ 1 (Удаление несуществующей задачи)
    void deleteTask_Boundary_TaskNotFound() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.deleteTask(999L, "Author"));
    }

    @Test // ГРАНИЧНЫЙ 2 (Удаление DONE задачи автором - должно быть разрешено)
    void deleteTask_Boundary_DeleteDoneTask() {
        task.setStatus(TaskStatus.DONE);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("Author")).thenReturn(Optional.of(author));

        assertDoesNotThrow(() -> taskService.deleteTask(10L, "Author"));
        verify(taskRepository, times(1)).deleteById(10L);
    }

    // ==========================================
    // 6. МЕТОД: toggleSubtask
    // ==========================================

    @Test // ПОЗИТИВНЫЙ
    void toggleSubtask_Positive_Success() {
        when(taskRepository.findById(20L)).thenReturn(Optional.of(epicTask));
        when(userRepository.findByUsername("Author")).thenReturn(Optional.of(author));
        when(taskRepository.save(epicTask)).thenReturn(epicTask);

        AbstractTask result = taskService.toggleSubtask(20L, 100L, "Author");
        assertTrue(((EpicTask) result).getSubtasks().get(0).isCompleted());
    }

    @Test // НЕГАТИВНЫЙ (Задача не является эпиком)
    void toggleSubtask_Negative_NotAnEpic() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task)); // ID 10 это SimpleTask
        when(userRepository.findByUsername("Author")).thenReturn(Optional.of(author));

        assertThrows(InvalidTaskOperationException.class, () -> taskService.toggleSubtask(10L, 100L, "Author"));
    }

    @Test // ГРАНИЧНЫЙ 1 (Подзадача не найдена в эпике)
    void toggleSubtask_Boundary_SubtaskNotFound() {
        when(taskRepository.findById(20L)).thenReturn(Optional.of(epicTask));
        when(userRepository.findByUsername("Author")).thenReturn(Optional.of(author));

        assertThrows(SubtaskNotFoundException.class, () -> taskService.toggleSubtask(20L, 999L, "Author"));
    }

    @Test // ГРАНИЧНЫЙ 2 (Попытка изменить подзадачу у DONE эпика)
    void toggleSubtask_Boundary_EpicIsDone() {
        epicTask.setStatus(TaskStatus.DONE);
        when(taskRepository.findById(20L)).thenReturn(Optional.of(epicTask));

        assertThrows(ResponseStatusException.class, () -> taskService.toggleSubtask(20L, 100L, "Author"));
    }

    // ==========================================
    // 7. МЕТОД: updateDeadline
    // ==========================================

    @Test // ПОЗИТИВНЫЙ
    void updateDeadline_Positive_Success() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("Author")).thenReturn(Optional.of(author));
        when(taskRepository.save(task)).thenReturn(task);

        AbstractTask result = taskService.updateDeadline(10L, "2026-12-31T23:59:00", "Author");
        assertEquals(LocalDateTime.of(2026, 12, 31, 23, 59, 0), result.getDueDate());
    }

    @Test // НЕГАТИВНЫЙ (Изменение не автором)
    void updateDeadline_Negative_Forbidden() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("Assignee")).thenReturn(Optional.of(assigneeUser));

        assertThrows(ResponseStatusException.class, () -> taskService.updateDeadline(10L, "2026-12-31T23:59:00", "Assignee"));
    }

    @Test // ГРАНИЧНЫЙ 1 (Изменение дедлайна у DONE задачи)
    void updateDeadline_Boundary_TaskIsDone() {
        task.setStatus(TaskStatus.DONE);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThrows(ResponseStatusException.class, () -> taskService.updateDeadline(10L, "2026-12-31T23:59:00", "Author"));
    }

    @Test // ГРАНИЧНЫЙ 2 (Ошибка парсинга даты)
    void updateDeadline_Boundary_InvalidDateFormat() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("Author")).thenReturn(Optional.of(author));

        assertThrows(java.time.format.DateTimeParseException.class, () -> {
            taskService.updateDeadline(10L, "INVALID_DATE", "Author");
        });
    }

    // ==========================================
    // 8. МЕТОД: getAllTasks
    // ==========================================

    @Test // ПОЗИТИВНЫЙ
    void getAllTasks_Positive_Success() {
        when(taskRepository.findAll()).thenReturn(List.of(task, epicTask));
        List<AbstractTask> result = taskService.getAllTasks();
        assertEquals(2, result.size());
    }

    @Test // НЕГАТИВНЫЙ (Ошибка БД)
    void getAllTasks_Negative_DatabaseError() {
        when(taskRepository.findAll()).thenThrow(new RuntimeException("DB down"));
        assertThrows(RuntimeException.class, () -> taskService.getAllTasks());
    }

    @Test // ГРАНИЧНЫЙ 1 (Пустая база данных)
    void getAllTasks_Boundary_EmptyList() {
        when(taskRepository.findAll()).thenReturn(Collections.emptyList());
        List<AbstractTask> result = taskService.getAllTasks();
        assertTrue(result.isEmpty());
    }

    @Test // ГРАНИЧНЫЙ 2 (Ровно 1 задача в БД)
    void getAllTasks_Boundary_SingleTask() {
        when(taskRepository.findAll()).thenReturn(List.of(task));
        List<AbstractTask> result = taskService.getAllTasks();
        assertEquals(1, result.size());
    }
}