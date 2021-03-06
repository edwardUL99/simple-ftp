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

/**
 * This class represents exceptions that occur after a command fails to be sent or failed to receive a reply from the server
 */
public class FTPCommandFailedException extends FTPException {
    /**
     * Constructs an exception object with the specified message
     * @param message the message for this exception to display
     * @param replyString the reply string from the FTP server
     */
    public FTPCommandFailedException(String message, String replyString) {
        super(message, replyString);
    }

    /**
     * Constructs an exception object with the specified message and causing exception
     * @param message the message for this exception to display
     * @param replyString the reply string from the FTP server
     * @param ex the causing exception
     */
    public FTPCommandFailedException(String message, String replyString, Exception ex) {
        super(message, replyString, ex);
    }
}
