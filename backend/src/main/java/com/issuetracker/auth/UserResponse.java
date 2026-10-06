package com.issuetracker.auth;

import java.time.Instant;

public record UserResponse(
    Long id, String name, String email, String avatarUrl, SystemRole systemRole, boolean active, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getAvatarUrl(),
            user.getSystemRole(),
            user.isActive(),
            user.getCreatedAt());
    }
}
