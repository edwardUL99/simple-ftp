package com.simpleftp.ftp.exceptions;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This abstract class will provide a base class for all FTP exceptions
 */
@Getter
public abstract class FTPException extends Exception {
    /**
     * The time at which this exception occurred;
     */
    private LocalDateTime exceptionTime;

    public FTPException(String message) {
        super(message);
        exceptionTime = LocalDateTime.now();
    }

    public FTPException(String message, Exception e) {
        super(message, e);
        exceptionTime = LocalDateTime.now();
    }

    /**
     * Appends the exception time onto the message for the super exception
     */
    @Override
    public String getMessage() {
        return super.getMessage() + ", Occurred at " + exceptionTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
    }
}
