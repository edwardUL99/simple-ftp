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
 * This class represents any exception that occurs when loading a Session file
 */
public class SessionLoadException extends Exception {

    /**
     * Constructs a default exception
     */
    public SessionLoadException() {
        super();
    }

    /**
     * Constructs an exception with the specified message
     * @param message the message for this exception to display
     */
    public SessionLoadException(String message) {
        super(message);
    }

    /**
     * Constructs an exception with the specified message and causing exception
     * @param message the message for this exception to display
     * @param throwable the throwable that caused this exception
     */
    public SessionLoadException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
