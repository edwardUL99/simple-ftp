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
 * This exception is used to represent a generic FTP error, not one related to connection
 * IOExceptions thrown in the context of the ftp calls are considered connection failures so, do not use this exception for that
 */
public class FTPError extends FTPException {
    /**
     * Constructs a FTPError object with the specified message
     * @param message the message for this exception to display
     */
    public FTPError(String message) {
        super(message);
    }

    /**
     * Constructs a FTPError object with the specified message and causing exception
     * @param message the message for this exception to display
     * @param ex the causing exception
     */
    public FTPError(String message, Exception ex) {
        super(message, ex);
    }
}
