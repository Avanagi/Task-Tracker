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
    private TaskCreationStrategy bugStrategy; // Мокаем конкретную стратегию

    private TaskFactory taskFactory;

    private User reporter;
    private TaskCreateDto dto;
    private AbstractTask mockTask;

    @BeforeEach
    void setUp() {
        // Чтобы Spring в тесте корректно собрал List<TaskCreationStrategy>,
        // мы вручную передаем список моков в конструктор фабрики.
        taskFactory = new TaskFactory(userRepository, List.of(bugStrategy));

        reporter = User.builder().id(1L).username("Author").build();

        dto = new TaskCreateDto();
        dto.setType("BUG");
        dto.setTitle("Test Title");
        dto.setDescription("Test Description");
        dto.setDueDate(LocalDateTime.now().plusDays(2));
        dto.setStepsToReproduce("Step 1");

        mockTask = new BugTask(); // Пустая болванка, которую "создаст" стратегия
    }

    // ==========================================
    // МЕТОД: createTask
    // ==========================================

    @Test // ПОЗИТИВНЫЙ (Создание задачи с назначенными исполнителями)
    void createTask_Positive_SuccessWithAssignees() {
        dto.setAssigneeIds(List.of(2L, 3L));

        User assignee = User.builder().id(2L).username("Dev").build();

        // Учим стратегию отзываться на тип "BUG" и возвращать болванку
        when(bugStrategy.getType()).thenReturn("BUG");
        when(bugStrategy.create(dto)).thenReturn(mockTask);
        // Имитируем поиск пользователей БД
        when(userRepository.findAllById(dto.getAssigneeIds())).thenReturn(List.of(assignee));

        AbstractTask result = taskFactory.createTask(dto, reporter);

        assertNotNull(result);
        assertEquals("Test Title", result.getTitle());
        assertEquals("Test Description", result.getDescription());
        assertEquals(reporter, result.getReporter());
        assertEquals(1, result.getAssignees().size()); // Убеждаемся, что исполнитель добавился

        verify(bugStrategy, times(1)).create(dto);
        verify(userRepository, times(1)).findAllById(dto.getAssigneeIds());
    }

    @Test // НЕГАТИВНЫЙ (Передан неизвестный тип задачи)
    void createTask_Negative_UnknownTypeThrowsException() {
        dto.setType("UNKNOWN_TYPE"); // Передаем плохой тип

        // Наша единственная стратегия - BUG. Она не отзовется на UNKNOWN_TYPE.
        when(bugStrategy.getType()).thenReturn("BUG");

        IllegalTaskArgumentException exception = assertThrows(IllegalTaskArgumentException.class, () -> {
            taskFactory.createTask(dto, reporter);
        });

        assertEquals("Unknown task's type UNKNOWN_TYPE", exception.getMessage());
        verify(bugStrategy, never()).create(any()); // Стратегия не должна вызываться
    }

    @Test // ГРАНИЧНЫЙ 1 (Список исполнителей пуст, но не null)
    void createTask_Boundary_EmptyAssigneesList() {
        dto.setAssigneeIds(Collections.emptyList()); // Передаем пустой список []

        when(bugStrategy.getType()).thenReturn("BUG");
        when(bugStrategy.create(dto)).thenReturn(mockTask);

        AbstractTask result = taskFactory.createTask(dto, reporter);

        assertNotNull(result);
        assertTrue(result.getAssignees().isEmpty()); // Исполнителей быть не должно

        // ВАЖНО: Проверяем, что мы не дергали базу данных вхолостую!
        verify(userRepository, never()).findAllById(any());
    }

    @Test // ГРАНИЧНЫЙ 2 (Список исполнителей равен null)
    void createTask_Boundary_NullAssigneesList() {
        dto.setAssigneeIds(null); // Передаем null

        when(bugStrategy.getType()).thenReturn("BUG");
        when(bugStrategy.create(dto)).thenReturn(mockTask);

        AbstractTask result = taskFactory.createTask(dto, reporter);

        assertNotNull(result);

        // ВАЖНО: Проверяем, что фабрика не упала с NullPointerException
        verify(userRepository, never()).findAllById(any());
    }
}