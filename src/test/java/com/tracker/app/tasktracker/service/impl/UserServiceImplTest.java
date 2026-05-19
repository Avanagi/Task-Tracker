package com.tracker.app.tasktracker.service.impl;

import com.tracker.app.tasktracker.dto.UserCreateDto;
import com.tracker.app.tasktracker.dto.UserResponseDto;
import com.tracker.app.tasktracker.exception.UserNotFoundException;
import com.tracker.app.tasktracker.mapper.UserMapper;
import com.tracker.app.tasktracker.model.entity.users.User;
import com.tracker.app.tasktracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;

    @InjectMocks private UserServiceImpl userService;

    private UserCreateDto createDto;
    private User testUser;
    private UserResponseDto responseDto;

    @BeforeEach
    void setUp() {
        createDto = new UserCreateDto();
        createDto.setUsername("Ivan");
        createDto.setEmail("ivan@test.com");
        createDto.setPassword("Pass123!");

        testUser = User.builder().id(1L).username("Ivan").email("ivan@test.com").password("Encoded!").build();
        responseDto = UserResponseDto.builder().id(1L).username("Ivan").email("ivan@test.com").build();
    }

    @Test
    void createUser_Positive_Success() {
        when(passwordEncoder.encode(anyString())).thenReturn("Encoded!");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toDto(any(User.class))).thenReturn(responseDto);

        UserResponseDto result = userService.createUser(createDto);

        assertNotNull(result);
        assertEquals("Ivan", result.getUsername());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUser_Negative_DatabaseConflictThrowsException() {
        when(passwordEncoder.encode(anyString())).thenReturn("Encoded!");
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("Duplicate key"));

        assertThrows(DataIntegrityViolationException.class, () -> userService.createUser(createDto));
        verify(userMapper, never()).toDto(any());
    }

    @Test
    void createUser_Boundary_MaxLengthUsername() {
        String maxUsername = "A".repeat(30);
        createDto.setUsername(maxUsername);

        when(passwordEncoder.encode(anyString())).thenReturn("Encoded!");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toDto(any(User.class))).thenReturn(responseDto);

        assertDoesNotThrow(() -> userService.createUser(createDto));
        verify(userRepository, times(1)).save(argThat(u -> u.getUsername().length() == 30));
    }

    @Test
    void createUser_Boundary_MaxPasswordLength() {
        String hugePassword = "P".repeat(128) + "1!";
        createDto.setPassword(hugePassword);

        when(passwordEncoder.encode(hugePassword)).thenReturn("HugeEncodedString");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toDto(any(User.class))).thenReturn(responseDto);

        assertDoesNotThrow(() -> userService.createUser(createDto));
        verify(passwordEncoder, times(1)).encode(hugePassword);
    }

    @Test
    void getUserByUsername_Positive_UserFound() {
        when(userRepository.findByUsername("Ivan")).thenReturn(Optional.of(testUser));
        when(userMapper.toDto(testUser)).thenReturn(responseDto);

        UserResponseDto result = userService.getUserByUsername("Ivan");

        assertEquals("Ivan", result.getUsername());
        verify(userMapper, times(1)).toDto(testUser);
    }

    @Test
    void getUserByUsername_Negative_UserNotFoundThrowsException() {
        when(userRepository.findByUsername("Ghost")).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.getUserByUsername("Ghost"));

        assertEquals("User not found", exception.getMessage());
        verify(userMapper, never()).toDto(any());
    }

    @Test
    void getUserByUsername_Boundary_EmptyString() {
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserByUsername(""));
        verify(userRepository, times(1)).findByUsername("");
    }

    @Test
    void getUserByUsername_Boundary_WhitespaceString() {
        when(userRepository.findByUsername("   ")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserByUsername("   "));
        verify(userRepository, times(1)).findByUsername("   ");
    }

    @Test
    void getAllUsers_Positive_ReturnsMultipleUsers() {
        User user2 = User.builder().id(2L).username("Anna").build();
        UserResponseDto dto2 = UserResponseDto.builder().id(2L).username("Anna").build();

        when(userRepository.findAll()).thenReturn(List.of(testUser, user2));
        when(userMapper.toDto(testUser)).thenReturn(responseDto);
        when(userMapper.toDto(user2)).thenReturn(dto2);

        List<UserResponseDto> result = userService.getAllUsers();

        assertEquals(2, result.size());
        assertEquals("Anna", result.get(1).getUsername());
    }

    @Test
    void getAllUsers_Negative_DatabaseFailure() {
        when(userRepository.findAll()).thenThrow(new RuntimeException("DB Connection failed"));

        assertThrows(RuntimeException.class, () -> userService.getAllUsers());
        verify(userMapper, never()).toDto(any());
    }

    @Test
    void getAllUsers_Boundary_EmptyList() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        List<UserResponseDto> result = userService.getAllUsers();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userMapper, never()).toDto(any());
    }

    @Test
    void getAllUsers_Boundary_SingleElementList() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(userMapper.toDto(testUser)).thenReturn(responseDto);

        List<UserResponseDto> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userMapper, times(1)).toDto(testUser);
    }
}