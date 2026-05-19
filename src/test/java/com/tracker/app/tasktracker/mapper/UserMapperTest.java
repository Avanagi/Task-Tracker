package com.tracker.app.tasktracker.mapper;

import com.tracker.app.tasktracker.dto.UserResponseDto;
import com.tracker.app.tasktracker.model.entity.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
    }


    @Test
    void toDto_Positive_SuccessMapping() {
        User user = User.builder()
                .id(1L)
                .username("TestUser")
                .email("test@example.com")
                .password("SecretPassword123")
                .build();

        UserResponseDto dto = userMapper.toDto(user);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("TestUser", dto.getUsername());
        assertEquals("test@example.com", dto.getEmail());
    }

    @Test
    void toDto_Negative_CorruptedUserObject() {
        User corruptedUser = new User() {
            @Override
            public String getUsername() {
                throw new RuntimeException("Proxy initialization failed");
            }
        };

        assertThrows(RuntimeException.class, () -> userMapper.toDto(corruptedUser));
    }

    @Test
    void toDto_Boundary_NullUserPassed() {
        UserResponseDto dto = userMapper.toDto(null);

        assertNull(dto, "Маппер должен возвращать null при передаче null, не падая с ошибкой");
    }

    @Test
    void toDto_Boundary_EmptyUserFields() {
        User emptyUser = new User();

        UserResponseDto dto = userMapper.toDto(emptyUser);

        assertNotNull(dto, "DTO должен создаться");
        assertNull(dto.getId());
        assertNull(dto.getUsername());
        assertNull(dto.getEmail());
    }
}