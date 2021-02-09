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

package com.simpleftp.filesystem.exceptions;

/**
 * This class represents an exception related to the FileSystem
 */
public class FileSystemException extends Exception {
    /**
     * Creates a default exception object
     */
    public FileSystemException() {
        super();
    }

    /**
     * Creates an exception object
     * @param message the message to display
     */
    public FileSystemException(String message) {
        super(message);
    }

    /**
     * Creates an exception object
     * @param message the message to display
     * @param ex the causing exception
     */
    public FileSystemException(String message, Exception ex) {
        super(message, ex);
    }
}
