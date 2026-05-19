package com.tracker.app.tasktracker.controller;

import com.tracker.app.tasktracker.dto.UserResponseDto;
import com.tracker.app.tasktracker.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserResponseDto userDto;

    @BeforeEach
    void setUp() {
        userDto = UserResponseDto.builder()
                .id(1L)
                .username("TestUser")
                .email("test@example.com")
                .build();
    }


    @Test
    void getAllUsers_Positive_Success() {
        when(userService.getAllUsers()).thenReturn(List.of(userDto, UserResponseDto.builder().id(2L).build()));

        ResponseEntity<List<UserResponseDto>> response = userController.getAllUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(userService, times(1)).getAllUsers();
    }

    @Test
    void getAllUsers_Negative_ServiceThrowsException() {
        when(userService.getAllUsers()).thenThrow(new RuntimeException("Database connection timeout"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userController.getAllUsers());

        assertEquals("Database connection timeout", exception.getMessage());
        verify(userService, times(1)).getAllUsers();
    }

    @Test
    void getAllUsers_Boundary_EmptyList() {
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());

        ResponseEntity<List<UserResponseDto>> response = userController.getAllUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getAllUsers_Boundary_ServiceReturnsNull() {
        when(userService.getAllUsers()).thenReturn(null);

        ResponseEntity<List<UserResponseDto>> response = userController.getAllUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
    }
}