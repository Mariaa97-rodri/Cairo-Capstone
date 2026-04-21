package com.cairo.cairobackend.dto.response;

import com.cairo.cairobackend.entity.User;
import lombok.Builder;
import lombok.Getter;

// Lightweight user summary — used inside other DTOs.
// Never exposes password or created_at.
@Getter
@Builder
public class UserResponse {

    private Long   id;
    private String name;
    private String email;
    private String role;

    public static UserResponse from(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}