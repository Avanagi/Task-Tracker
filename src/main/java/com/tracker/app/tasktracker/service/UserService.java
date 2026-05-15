package com.tracker.app.tasktracker.service;

import com.tracker.app.tasktracker.dto.UserCreateDto;
import com.tracker.app.tasktracker.dto.UserResponseDto;

import java.util.List;

public interface UserService {

    UserResponseDto createUser(UserCreateDto dto);

    UserResponseDto getUserByUsername(String username);

    List<UserResponseDto> getAllUsers();
}