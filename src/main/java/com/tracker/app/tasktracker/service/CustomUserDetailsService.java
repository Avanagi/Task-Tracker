package com.tracker.app.tasktracker.service;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

public interface CustomUserDetailsService extends UserDetailsService {

    @Override
    @Transactional(readOnly = true)
    @NonNull
    UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException;

}