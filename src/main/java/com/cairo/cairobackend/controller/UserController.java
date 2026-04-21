package com.cairo.cairobackend.controller;

import com.cairo.cairobackend.dto.response.UserResponse;
import com.cairo.cairobackend.entity.User;
import com.cairo.cairobackend.exception.BusinessException;
import com.cairo.cairobackend.exception.ResourceNotFoundException;
import com.cairo.cairobackend.exception.UnauthorizedException;
import com.cairo.cairobackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // Any authenticated user can list all users —
    // powers the Add Member and Assignee dropdowns in the frontend.
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    // Returns the currently authenticated user — useful for frontend "who am I" checks.
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(UserResponse.from(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return ResponseEntity.ok(UserResponse.from(user));
    }

    // ADMIN only: promote or demote a user's role.
    // Admins cannot change their own role to prevent accidental lockout.
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new UnauthorizedException(
                    "Only admins can change user roles.");
        }

        if (currentUser.getId().equals(id)) {
            throw new BusinessException(
                    "You cannot change your own role.");
        }

        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        String roleStr = body.get("role");
        try {
            target.setRole(User.Role.valueOf(roleStr));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "Invalid role: " + roleStr + ". Must be USER or ADMIN.");
        }

        userRepository.save(target);
        log.info("User {} role updated to {} by admin {}",
                id, roleStr, currentUser.getEmail());
        return ResponseEntity.ok(UserResponse.from(target));
    }
}