package com.sum.backend.dto;

import com.sum.backend.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserResponse {

    private final Long id;
    private final String username;
    private final String loginId;
    private final String email;
    private final LocalDateTime createdAt;

    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.loginId = user.getLoginId();
        this.email = user.getEmail();
        this.createdAt = user.getCreatedAt();
    }
}
