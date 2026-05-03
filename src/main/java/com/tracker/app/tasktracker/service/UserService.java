package com.tracker.app.tasktracker.service;

import com.tracker.app.tasktracker.dto.UserCreateDto;
import com.tracker.app.tasktracker.model.entity.users.User;
import com.tracker.app.tasktracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

public interface UserService {

    User createUser(UserCreateDto dto);

    User getUserByUsername(String username);

    List<User> getAllUsers();
}