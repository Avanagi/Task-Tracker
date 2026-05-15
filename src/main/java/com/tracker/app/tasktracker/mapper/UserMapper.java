package com.tracker.app.tasktracker.mapper;

import com.tracker.app.tasktracker.dto.UserResponseDto;
import com.tracker.app.tasktracker.model.entity.users.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponseDto toDto(User user) {
        if (user == null) return null;

        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}