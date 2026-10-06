package com.issuetracker.auth;

import com.issuetracker.common.validation.MaxBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Password is capped at 72 UTF-8 bytes because BCrypt ignores anything beyond that.
 */
public record RegisterRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Email @Size(max = 254) String email,
    @NotNull @Size(min = 8, message = "must be at least 8 characters") @MaxBytes(72) String password) {
}
