package com.carepulse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidEmergencyRosterException extends RuntimeException {
    public InvalidEmergencyRosterException(String message) {
        super(message);
    }
}
