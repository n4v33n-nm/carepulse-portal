package com.carepulse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class EmergencyRequestNotFoundException extends RuntimeException {
    public EmergencyRequestNotFoundException(String message) {
        super(message);
    }
}
