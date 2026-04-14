package com.cairo.cairobackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String message;
    private Long userId;
    private String name;
    private String email;
    private String role;
}