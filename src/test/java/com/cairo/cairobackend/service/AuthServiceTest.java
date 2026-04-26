package com.cairo.cairobackend.service;

import com.cairo.cairobackend.dto.response.AuthResponse;
import com.cairo.cairobackend.entity.User;
import com.cairo.cairobackend.exception.DuplicateResourceException;
import com.cairo.cairobackend.repository.UserRepository;
import com.cairo.cairobackend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository        userRepository;
    @Mock PasswordEncoder       passwordEncoder;
    @Mock JwtService            jwtService;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = User.builder()
                .id(1L)
                .name("Maria Aguilar")
                .email("maria@cairo.com")
                .password("hashed")
                .role(User.Role.USER)
                .build();
    }

    // ── register ──────────────────────────────────────────────────

    @Test
    void register_success_returnsTokenAndUserDetails() {
        when(userRepository.existsByEmail("maria@cairo.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("jwt-abc");

        AuthResponse response = authService.register("Maria Aguilar", "maria@cairo.com", "password123");

        assertThat(response.getToken()).isEqualTo("jwt-abc");
        assertThat(response.getEmail()).isEqualTo("maria@cairo.com");
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getMessage()).isEqualTo("Account created successfully");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsDuplicateResourceException() {
        when(userRepository.existsByEmail("maria@cairo.com")).thenReturn(true);

        assertThatThrownBy(() ->
                authService.register("Maria", "maria@cairo.com", "pass"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("maria@cairo.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_passwordIsEncoded_notStoredInPlaintext() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            assertThat(u.getPassword()).isEqualTo("hashed");
            assertThat(u.getPassword()).doesNotContain("password123");
            return savedUser;
        });
        when(jwtService.generateToken(any())).thenReturn("token");

        authService.register("Test", "test@cairo.com", "password123");
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void register_newUser_alwaysGetsUserRole() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            assertThat(u.getRole()).isEqualTo(User.Role.USER);
            return savedUser;
        });
        when(jwtService.generateToken(any())).thenReturn("token");

        authService.register("Test", "test@cairo.com", "pass");
    }

    // ── login ─────────────────────────────────────────────────────

//    @Test
//    void login_validCredentials_returnsToken() {
//        doNothing().when(authenticationManager).authenticate(any());
//        when(userRepository.findByEmail("maria@cairo.com"))
//                .thenReturn(Optional.of(savedUser));
//        when(jwtService.generateToken(savedUser)).thenReturn("login-token");
//
//        AuthResponse response = authService.login("maria@cairo.com", "password123");
//
//        assertThat(response.getToken()).isEqualTo("login-token");
//        assertThat(response.getMessage()).isEqualTo("Login successful");
//        assertThat(response.getRole()).isEqualTo("USER");
//        assertThat(response.getEmail()).isEqualTo("maria@cairo.com");
//    }

    @Test
    void login_wrongPassword_throwsBadCredentialsException() {
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login("maria@cairo.com", "wrong"))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByEmail(anyString());
    }

//    @Test
//    void login_userNotFoundAfterAuth_throwsRuntimeException() {
//        doNothing().when(authenticationManager).authenticate(any());
//        when(userRepository.findByEmail("ghost@cairo.com"))
//                .thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> authService.login("ghost@cairo.com", "pass"))
//                .isInstanceOf(RuntimeException.class)
//                .hasMessageContaining("User not found");
//    }
}