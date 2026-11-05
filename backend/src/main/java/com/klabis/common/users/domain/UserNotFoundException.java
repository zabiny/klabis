package com.klabis.common.users.domain;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;

public class UserNotFoundException extends ResourceNotFoundException implements UserFacingError {

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
