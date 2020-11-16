/*
 *  Copyright (C) 2020  Edward Lynch-Milner
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.simpleftp.ftp.exceptions;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This abstract class will provide a base class for all exceptions related to files within the FTP system/context. This includes local paths as ultimately they may end up on the FTP server
 */
@Getter
public abstract class FTPException extends Exception {
    /**
     * The time at which this exception occurred;
     */
    private final LocalDateTime exceptionTime;
    /**
     * The reply message from the FTP server at the time this exception was thrown
     */
    @Getter
    private String replyString;

    public FTPException(String message, String replyString) {
        super(message);
        this.replyString = replyString;
        exceptionTime = LocalDateTime.now();
    }

    public FTPException(String message, String replyString, Exception e) {
        super(message, e);
        this.replyString = replyString;
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
