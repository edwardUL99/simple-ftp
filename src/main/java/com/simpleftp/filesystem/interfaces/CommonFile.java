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

package com.simpleftp.filesystem.interfaces;

import com.simpleftp.filesystem.exceptions.FileSystemException;

/**
 * This interface specifies a file that represent a file either on a local or remote file system
 */
public interface CommonFile {
    /**
     * Returns the name of this file without any path
     * @return base name of this file
     */
    String getName();

    /**
     * Returns the path to the file
     * @return file path
     */
    String getFilePath();

    /**
     * Checks if this file exists
     * @return true if it exists, false if not
     * @throws FileSystemException if any error occurs and the file exists could not be determined
     */
    boolean exists() throws FileSystemException;

    /**
     * Checks if this file is a directory
     * @return true if it is a directory, false if not
     * @throws FileSystemException if any error occurs and if isDirectory could not be determined
     */
    boolean isADirectory() throws FileSystemException;

    /**
     * Checks if this file is a normal file, i.e. not a directory
     * @return true if it is not a directory, false if not
     * @throws FileSystemException if any error occurs and it cannot be determined if it is a normal file
     */
    boolean isNormalFile() throws FileSystemException;

    /**
     * Returns the size in bytes of the file
     * @return size in bytes of the file, -1 if could not be determined
     */
    long getSize() throws FileSystemException;

    /**
     * Returns the hashcode for this object
     * @return the hash code
     */
    int hashCode();

    /**
     * Determines whether two CommonFiles are equal, either by hash code or path
     * @param obj the object to compare to
     * @return true if equals, false if not
     */
    boolean equals(Object obj);
}
