package com.issuetracker.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "avatar_url", length = 2048)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", nullable = false, length = 32)
    private SystemRole systemRole;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Nullable
    @Column(name = "failed_login_window_start")
    private Instant failedLoginWindowStart;

    @Nullable
    @Column(name = "locked_until")
    private Instant lockedUntil;

    protected User() {
    }

    public User(String name, String email, String passwordHash) {
        this(name, email, passwordHash, SystemRole.USER);
    }

    public User(String name, String email, String passwordHash, SystemRole systemRole) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.systemRole = systemRole;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /**
     * Counts a failure in the current window; a new window starts once the old one has passed. Reaching
     * {@code maxAttempts} locks the account and resets the count, so the user gets fresh attempts after the lock.
     */
    public void recordFailedLogin(Instant now, int maxAttempts, Duration window, Duration lockDuration) {
        if (failedLoginWindowStart == null || !now.isBefore(failedLoginWindowStart.plus(window))) {
            failedLoginWindowStart = now;
            failedLoginCount = 0;
        }
        failedLoginCount++;
        if (failedLoginCount >= maxAttempts) {
            lockedUntil = now.plus(lockDuration);
            failedLoginCount = 0;
            failedLoginWindowStart = null;
        }
    }

    public void recordSuccessfulLogin() {
        failedLoginCount = 0;
        failedLoginWindowStart = null;
        lockedUntil = null;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public SystemRole getSystemRole() {
        return systemRole;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    @Nullable
    public Instant getFailedLoginWindowStart() {
        return failedLoginWindowStart;
    }

    @Nullable
    public Instant getLockedUntil() {
        return lockedUntil;
    }
}
