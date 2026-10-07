package com.issuetracker.auth;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Locks the row so concurrent failed logins are all counted towards the lockout.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findForLoginByEmail(@Param("email") String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsBySystemRole(SystemRole systemRole);
}
