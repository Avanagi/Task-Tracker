package com.tracker.app.tasktracker.controller;

import com.tracker.app.tasktracker.dto.UserCreateDto;
import com.tracker.app.tasktracker.dto.UserResponseDto;
import com.tracker.app.tasktracker.service.UserService;
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

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private Principal principal;

    @InjectMocks
    private AuthController authController;

    private UserCreateDto createDto;
    private UserResponseDto responseDto;

    @BeforeEach
    void setUp() {
        createDto = new UserCreateDto();
        createDto.setUsername("TestUser");
        createDto.setEmail("test@example.com");
        createDto.setPassword("StrongPass1!");

        responseDto = UserResponseDto.builder()
                .id(1L)
                .username("TestUser")
                .email("test@example.com")
                .build();
    }

    // ==========================================
    // 1. МЕТОД: register
    // ==========================================

    @Test // ПОЗИТИВНЫЙ
    void register_Positive_Success() {
        // Учим сервис возвращать подготовленный DTO
        when(userService.createUser(createDto)).thenReturn(responseDto);

        // Вызываем метод контроллера
        ResponseEntity<UserResponseDto> response = authController.register(createDto);

        // Проверяем: контроллер должен вернуть статус 200 OK и тело с пользователем
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(userService, times(1)).createUser(createDto);
    }

    @Test // НЕГАТИВНЫЙ (Сервис выбрасывает ошибку при регистрации)
    void register_Negative_ServiceThrowsException() {
        // Имитируем ошибку (например, такой email уже есть)
        when(userService.createUser(createDto)).thenThrow(new RuntimeException("Email already exists"));

        // Убеждаемся, что контроллер не гасит ошибку, а "пробрасывает" её дальше (для GlobalExceptionHandler)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> authController.register(createDto));
        assertEquals("Email already exists", exception.getMessage());

        verify(userService, times(1)).createUser(createDto);
    }

    @Test // ГРАНИЧНЫЙ 1 (Сервис возвращает null)
    void register_Boundary_ServiceReturnsNull() {
        // Проверка ситуации: сервис отработал без ошибок, но почему-то вернул null
        when(userService.createUser(createDto)).thenReturn(null);

        ResponseEntity<UserResponseDto> response = authController.register(createDto);

        // Ожидаем статус 200, но с пустым телом
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService, times(1)).createUser(createDto);
    }

    @Test // ГРАНИЧНЫЙ 2 (Передача null вместо DTO)
    void register_Boundary_NullDtoPassed() {
        // В реальном приложении Spring MVC заблокирует null-тело до вызова контроллера,
        // но юнит-тест изолирован. Проверяем реакцию на null.
        assertThrows(NullPointerException.class, () -> {
            authController.register(null);
        });

        // Сервис не должен быть вызван, так как контроллер упадет на строке логгера (dto.getUsername())
        verify(userService, never()).createUser(any());
    }

    // ==========================================
    // 2. МЕТОД: me
    // ==========================================

    @Test // ПОЗИТИВНЫЙ
    void me_Positive_Success() {
        // Имитируем авторизованного пользователя
        when(principal.getName()).thenReturn("TestUser");
        when(userService.getUserByUsername("TestUser")).thenReturn(responseDto);

        ResponseEntity<UserResponseDto> response = authController.me(principal);

        // Проверяем 200 OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(userService, times(1)).getUserByUsername("TestUser");
    }

    @Test // НЕГАТИВНЫЙ (Пользователь удален из БД, но его сессия еще жива)
    void me_Negative_UserNotFoundInService() {
        when(principal.getName()).thenReturn("DeletedUser");
        when(userService.getUserByUsername("DeletedUser")).thenThrow(new RuntimeException("User not found"));

        assertThrows(RuntimeException.class, () -> authController.me(principal));
        verify(userService, times(1)).getUserByUsername("DeletedUser");
    }

    @Test // ГРАНИЧНЫЙ 1 (Имя в Principal - пустая строка)
    void me_Boundary_EmptyPrincipalName() {
        // Проверка случая, если Spring Security сформирует пустой объект авторизации
        when(principal.getName()).thenReturn("");
        when(userService.getUserByUsername("")).thenReturn(null);

        ResponseEntity<UserResponseDto> response = authController.me(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService, times(1)).getUserByUsername("");
    }

    @Test // ГРАНИЧНЫЙ 2 (Principal отсутствует / равен null)
    void me_Boundary_NullPrincipal() {
        // Если кто-то попытается вызвать метод без авторизации напрямую
        // (в обход фильтров Spring Security), метод упадет на вызове principal.getName()

        assertThrows(NullPointerException.class, () -> authController.me(null));

        // До сервиса дело дойти не должно
        verify(userService, never()).getUserByUsername(any());
    }
}