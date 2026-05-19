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



    @Test
    void loadUserByUsername_Positive_Success() {
        when(userRepository.findByUsername("SecurityAdmin")).thenReturn(Optional.of(testUser));
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("SecurityAdmin");

        assertNotNull(userDetails);
        assertEquals("SecurityAdmin", userDetails.getUsername());
        assertEquals("HashedPassword123!", userDetails.getPassword());

        assertTrue(userDetails.isEnabled());

        verify(userRepository, times(1)).findByUsername("SecurityAdmin");
    }

    @Test
    void loadUserByUsername_Negative_UserNotFoundThrowsException() {
        when(userRepository.findByUsername("Hacker123")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("Hacker123");
        });

        assertEquals("User not found: Hacker123", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("Hacker123");
    }

    @Test
    void loadUserByUsername_Boundary_EmptyUsername() {
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("");
        });

        assertEquals("User not found: ", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("");
    }

    @Test
    void loadUserByUsername_Boundary_ExtremelyLongUsername() {
        String hugeUsername = "A".repeat(1000);

        when(userRepository.findByUsername(hugeUsername)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername(hugeUsername);
        });

        assertTrue(exception.getMessage().contains("User not found"));
        verify(userRepository, times(1)).findByUsername(hugeUsername);
    }
}