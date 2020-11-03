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

package com.simpleftp.security.exceptions;

/**
 * This class represents an unchecked exception for errors related to the Password encryption.
 * It is unchecked but still recommended to be caught at start-up to be able to throw an error dialog if it can't be found.
 */
public class PasswordEncryptionException extends RuntimeException {
    /**
     * Constructs an exception with the specified message
     * @param message the message to display
     */
    public PasswordEncryptionException(String message) {
        super(message);
    }

    /**
     * Constructs an exception with the specified message and causing exception
     * @param message the message to display
     * @param cause the exception that caused this
     */
    public PasswordEncryptionException(String message, Exception cause) {
        super(message, cause);
    }
}
