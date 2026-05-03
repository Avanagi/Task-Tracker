package com.tracker.app.tasktracker.controller;

import com.tracker.app.tasktracker.dto.UserCreateDto;
import com.tracker.app.tasktracker.model.entity.users.User;
import com.tracker.app.tasktracker.repository.UserRepository;
import com.tracker.app.tasktracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody UserCreateDto dto) {
        log.info("Received request to register user {}", dto.getUsername());
        return ResponseEntity.ok(userService.createUser(dto));
    }

    @GetMapping("/me")
    public ResponseEntity<User> me(Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        log.info("User with name [{}] found", user.getUsername());
        return ResponseEntity.ok(user);
    }
}