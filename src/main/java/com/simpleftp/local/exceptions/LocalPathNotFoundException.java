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

package com.simpleftp.local.exceptions;

import lombok.Getter;

/**
 * This exception is to be thrown when a local path is given but it doesn't exist on the local file system
 */
public class LocalPathNotFoundException extends Exception {
    /**
     * The path that was not found
     */
    @Getter
    private final String localPath;

    /**
     * Constructs an exception object with the given message and path to the local file that does not exist
     * @param message the message for the exception to display
     * @param localPath the local path that does not exist
     */
    public LocalPathNotFoundException(String message, String localPath) {
        super(message);
        this.localPath = localPath;
    }

    /**
     * Same as the first constructor, but allows you to specify a causing exception
     * @param message the message for this exception to display
     * @param ex the exception that caused this one
     * @param localPath the local path that does not exist
     */
    public LocalPathNotFoundException(String message, Exception ex, String localPath) {
        super(message, ex);
        this.localPath = localPath;
    }
}
