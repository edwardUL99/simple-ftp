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

package com.simpleftp.sessions.exceptions;

/**
 * This exception is used to indicate that the Session saving functionality is not initialised correctly.
 * It's a run-time exception as with proper care (i.e. checking if session is initialised before saving, this shouldn't be thrown.
 * The recommendation is to warn that sessions can't currently be saved in a dialog and give the error message why
 */
public class SessionInitialisationException extends RuntimeException {
    /**
     * Constructs an exception with the specified message
     * @param message the message for this exception
     */
    public SessionInitialisationException(String message) {
        super(message);
    }

    /**
     * Constructs an exception with a message and cause
     * @param message the message for this exception
     * @param ex the exception that caused this exception
     */
    public SessionInitialisationException(String message, Exception ex) {
        super(message, ex);
    }
}
