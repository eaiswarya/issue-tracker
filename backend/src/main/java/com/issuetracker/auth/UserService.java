package com.issuetracker.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    /**
     * @param subject the access token's subject, which is the user id
     */
    @Transactional(readOnly = true)
    public User getCurrentUser(String subject) {
        return users.findById(Long.valueOf(subject)).orElseThrow(UnknownUserException::new);
    }
}
