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

import com.simpleftp.ftp.exceptions.FTPException;
import lombok.Getter;

import java.io.IOException;

/**
 * An exception indicating that resolving a path failed.
 * Can only wrap an IOException or FTPException, or else an IllegalArgumentException is thrown.
 * It uses the message from the wrapped exception.
 * Used by paths.interfaces.PathResolver
 */
public class PathResolverException extends Exception {
    @Getter
    private final Exception wrappedException;

    /**
     * Constructs a PathResolverException with the specified exception
     * @param wrappedException the IOException or FTPException this is wrapping
     * @throws IllegalArgumentException if the wrappedException is not a FTPException or IOException
     */
    public PathResolverException(Exception wrappedException) throws IllegalArgumentException {
        if (!(wrappedException instanceof IOException) && !(wrappedException instanceof FTPException)) {
            throw new IllegalArgumentException("The provided exception must be an instance of IOException or FTPException");
        }
        this.wrappedException = wrappedException;
    }

    /**
     * Gets the message of the wrapped exception as super(wrappedException.getMessage) was not called as the type of wrapped exception needed to be checked before super
     * @return the message of the wrapped exception
     */
    @Override
    public String getMessage() {
        return wrappedException.getMessage();
    }

    /**
     * Prints the stack trace with stderr
     */
    @Override
    public void printStackTrace() {
        System.err.println("PathResolverException wrapping:");
        super.printStackTrace();
    }
}
