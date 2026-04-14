package com.cairo.cairobackend.service;

import com.cairo.cairobackend.entity.User;
import com.cairo.cairobackend.exception.DuplicateResourceException;
import com.cairo.cairobackend.repository.UserRepository;
import com.cairo.cairobackend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public String register(String name, String email, String password) {

        // Check for duplicate email before trying to save
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "An account with email " + email + " already exists.");
        }

        User user = User.builder()
                .name(name)
                .email(email)
                // Hash the password with BCrypt before saving —
                // never store plain text passwords
                .password(passwordEncoder.encode(password))
                .role(User.Role.USER)
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: {}", email);

        // Return a JWT immediately so the user is
        // logged in right after registering
        return jwtService.generateToken(saved);
    }

    public String login(String email, String password) {

        // AuthenticationManager validates the credentials
        // and throws an exception if they're wrong —
        // we don't need to check manually
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("User logged in: {}", email);
        return jwtService.generateToken(user);
    }
}