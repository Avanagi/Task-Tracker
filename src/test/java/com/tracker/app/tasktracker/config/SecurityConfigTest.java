package com.tracker.app.tasktracker.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SecurityConfigTest {

    @InjectMocks
    private SecurityConfig securityConfig;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private HttpSecurity httpSecurity;

    @Mock
    private SecurityFilterChain filterChainMock;

    @BeforeEach
    void setUp() {
        doReturn(filterChainMock).when(httpSecurity).build();
    }

    @Test
    void passwordEncoder_Positive_ReturnsBCryptInstance() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        assertNotNull(encoder);
        String rawPassword = "MySecretPassword123";
        String encodedPassword = encoder.encode(rawPassword);

        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encoder.matches(rawPassword, encodedPassword));
    }

    @Test
    void passwordEncoder_Negative_MatchesFailsOnWrongPassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String encoded = encoder.encode("CorrectPassword");

        assertFalse(encoder.matches("WrongPassword", encoded));
    }

    @Test
    void passwordEncoder_Boundary_DefaultStrengthAndFormat() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String encoded = encoder.encode("BoundaryTest");

        assert encoded != null;
        assertEquals(60, encoded.length(), "Длина хэша BCrypt должна быть 60 символов");
        assertTrue(encoded.startsWith("$2a$") || encoded.startsWith("$2y$"),
                "Хэш должен содержать стандартный префикс BCrypt");
    }

    @Test
    void passwordEncoder_Boundary_MalformedHash() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        boolean isMatch = encoder.matches("ValidPassword", "Invalid_Hash_String_From_Database");

        assertFalse(isMatch, "Сверка с некорректным хэшем должна возвращать false, не вызывая падения");
    }

    @Test
    void filterChain_Positive_BuildsSuccessfully() throws Exception {
        SecurityFilterChain result = securityConfig.filterChain(httpSecurity);

        assertNotNull(result);
        assertEquals(filterChainMock, result);
        verify(httpSecurity, times(1)).build();
    }

    @Test
    void filterChain_Negative_NullHttpSecurity() {
        assertThrows(NullPointerException.class, () -> securityConfig.filterChain(null));
    }

    @Test
    void filterChain_Boundary_BuildFails() {
        reset(httpSecurity);
        doThrow(new IllegalStateException("Configuration error")).when(httpSecurity).build();

        assertThrows(IllegalStateException.class, () -> securityConfig.filterChain(httpSecurity));
    }

    @Test
    void filterChain_Boundary_DoubleInvocation() throws Exception {
        SecurityFilterChain result1 = securityConfig.filterChain(httpSecurity);
        SecurityFilterChain result2 = securityConfig.filterChain(httpSecurity);

        assertNotNull(result1);
        assertNotNull(result2);
        verify(httpSecurity, times(2)).build();
    }
}