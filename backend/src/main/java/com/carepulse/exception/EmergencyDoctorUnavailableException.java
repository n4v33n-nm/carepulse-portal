package com.carepulse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class EmergencyDoctorUnavailableException extends RuntimeException {
    public EmergencyDoctorUnavailableException(String message) {
        super(message);
    }
}
