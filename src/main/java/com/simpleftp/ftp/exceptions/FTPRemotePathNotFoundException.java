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

/**
 * This exception is to be thrown when a remote path cannot be found on the FTP server
 */
public class FTPRemotePathNotFoundException extends FTPException {
    /**
     * The remote path that could not be found
     */
    @Getter
    private final String remotePath;

    /**
     * Constructs an exception object with the given message and path to the remote path that does not exist
     * @param message the message for the exception to display
     * @param remotePath the remote path that does not exist
     */
    public FTPRemotePathNotFoundException(String message, String remotePath) {
        super(message, "");
        this.remotePath = remotePath;
    }

    /**
     * Same as the first constructor, but allows you to specify a causing exception
     * @param message the message for this exception to display
     * @param ex the exception that caused this one
     * @param remotePath the remote path that does not exist
     */
    public FTPRemotePathNotFoundException(String message, Exception ex, String remotePath) {
        super(message, "", ex);
        this.remotePath = remotePath;
    }
}
