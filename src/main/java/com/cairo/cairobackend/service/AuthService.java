package com.cairo.cairobackend.service;

import com.cairo.cairobackend.dto.response.AuthResponse;
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
    public AuthResponse register(String name,
                                 String email,
                                 String password) {

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "An account with email " + email
                            + " already exists.");
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(User.Role.USER)
                .build();

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved);

        log.info("New user registered: {}", email);

        return AuthResponse.builder()
                .token(token)
                .message("Account created successfully")
                .userId(saved.getId())
                .name(saved.getName())
                .email(saved.getEmail())
                .role(saved.getRole().name())
                .build();
    }

    public AuthResponse login(String email, String password) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email, password)
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        String token = jwtService.generateToken(user);

        log.info("User logged in: {}", email);

        return AuthResponse.builder()
                .token(token)
                .message("Login successful")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}