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

    @Test
    void register_Positive_Success() {
        when(userService.createUser(createDto)).thenReturn(responseDto);

        ResponseEntity<UserResponseDto> response = authController.register(createDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(userService, times(1)).createUser(createDto);
    }

    @Test
    void register_Negative_ServiceThrowsException() {
        when(userService.createUser(createDto)).thenThrow(new RuntimeException("Email already exists"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authController.register(createDto));
        assertEquals("Email already exists", exception.getMessage());

        verify(userService, times(1)).createUser(createDto);
    }

    @Test
    void register_Boundary_ServiceReturnsNull() {
        when(userService.createUser(createDto)).thenReturn(null);

        ResponseEntity<UserResponseDto> response = authController.register(createDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService, times(1)).createUser(createDto);
    }

    @Test
    void register_Boundary_NullDtoPassed() {
        assertThrows(NullPointerException.class, () -> {
            authController.register(null);
        });

        verify(userService, never()).createUser(any());
    }

    @Test
    void me_Positive_Success() {
        when(principal.getName()).thenReturn("TestUser");
        when(userService.getUserByUsername("TestUser")).thenReturn(responseDto);

        ResponseEntity<UserResponseDto> response = authController.me(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(userService, times(1)).getUserByUsername("TestUser");
    }

    @Test
    void me_Negative_UserNotFoundInService() {
        when(principal.getName()).thenReturn("DeletedUser");
        when(userService.getUserByUsername("DeletedUser")).thenThrow(new RuntimeException("User not found"));

        assertThrows(RuntimeException.class, () -> authController.me(principal));
        verify(userService, times(1)).getUserByUsername("DeletedUser");
    }

    @Test
    void me_Boundary_EmptyPrincipalName() {
        when(principal.getName()).thenReturn("");
        when(userService.getUserByUsername("")).thenReturn(null);

        ResponseEntity<UserResponseDto> response = authController.me(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService, times(1)).getUserByUsername("");
    }

    @Test
    void me_Boundary_NullPrincipal() {
        assertThrows(NullPointerException.class, () -> authController.me(null));

        verify(userService, never()).getUserByUsername(any());
    }
}