package com.marketplace.service;

import com.marketplace.dto.RegisterRequest;
import com.marketplace.dto.UserResponse;
import com.marketplace.exception.InvalidRegistrationException;
import com.marketplace.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AccountService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidRegistrationException("Password must contain at most 72 UTF-8 bytes");
        }
        var user = repository.create(request.email().toLowerCase(Locale.ROOT), request.displayName().strip(),
                passwordEncoder.encode(request.password()));
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt());
    }
}
