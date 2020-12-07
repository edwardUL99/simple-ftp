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

package com.simpleftp.ui.exceptions;

/**
 * An exception surrounding any exceptions thrown in the UI
 */
public class UIException extends Exception {
    /**
     * Constructs an UIException object with the given message
     * @param message the message for this exception
     */
    public UIException(String message) {
        super(message);
    }

    /**
     * Constructs an exception from the given message and cause
     * @param message the message for the display
     * @param cause the cause of this exception
     */
    public UIException(String message, Exception cause) {
        super(message, cause);
    }
}
