package com.issuetracker.auth;

import com.issuetracker.common.FieldConflictException;

public class EmailAlreadyRegisteredException extends FieldConflictException {

    public EmailAlreadyRegisteredException() {
        super("email", "An account with this email already exists");
    }
}
