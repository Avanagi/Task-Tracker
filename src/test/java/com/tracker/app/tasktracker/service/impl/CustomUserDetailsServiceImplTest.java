package com.tracker.app.tasktracker.service.impl;

import com.tracker.app.tasktracker.model.entity.users.User;
import com.tracker.app.tasktracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsServiceImpl customUserDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("SecurityAdmin")
                .email("admin@test.com")
                .password("HashedPassword123!")
                .build();
    }

    // ==========================================
    // МЕТОД: loadUserByUsername
    // ==========================================

    @Test // ПОЗИТИВНЫЙ
    void loadUserByUsername_Positive_Success() {
        // Устанавливаем поведение мока: если ищут "SecurityAdmin", возвращаем нашего юзера
        when(userRepository.findByUsername("SecurityAdmin")).thenReturn(Optional.of(testUser));

        // Вызов тестируемого метода
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("SecurityAdmin");

        // Проверки
        assertNotNull(userDetails);
        assertEquals("SecurityAdmin", userDetails.getUsername());
        assertEquals("HashedPassword123!", userDetails.getPassword());

        // Проверяем, что Spring Security по умолчанию (при отсутствии ролей в вашей БД)
        // может создать пустой или дефолтный список Authorities, но объект собирается корректно
        assertTrue(userDetails.isEnabled());

        verify(userRepository, times(1)).findByUsername("SecurityAdmin");
    }

    @Test // НЕГАТИВНЫЙ (Попытка входа с несуществующим логином)
    void loadUserByUsername_Negative_UserNotFoundThrowsException() {
        // Имитируем отсутствие пользователя в БД
        when(userRepository.findByUsername("Hacker123")).thenReturn(Optional.empty());

        // Проверяем, что выбрасывается специфичное исключение Spring Security
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("Hacker123");
        });

        // Проверяем сообщение об ошибке
        assertEquals("User not found: Hacker123", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("Hacker123");
    }

    @Test // ГРАНИЧНЫЙ 1 (Пустая строка в качестве логина)
    void loadUserByUsername_Boundary_EmptyUsername() {
        // Граничный случай: попытка авторизации с абсолютно пустой строкой
        // Метод должен корректно передать пустую строку в БД и обработать отказ
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("");
        });

        assertEquals("User not found: ", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("");
    }

    @Test // ГРАНИЧНЫЙ 2 (Сверхдлинная строка, превышающая лимиты БД)
    void loadUserByUsername_Boundary_ExtremelyLongUsername() {
        // Граничный случай: хакер отправляет строку длиной в 1000 символов,
        // чтобы проверить систему на переполнение буфера памяти (Buffer Overflow)
        String hugeUsername = "A".repeat(1000);

        // БД гарантированно не найдет такого юзера, так как у нас лимит 30 символов в схеме
        when(userRepository.findByUsername(hugeUsername)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername(hugeUsername);
        });

        assertTrue(exception.getMessage().contains("User not found"));
        verify(userRepository, times(1)).findByUsername(hugeUsername);
    }
}