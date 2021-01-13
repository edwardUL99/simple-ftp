/*
 *  Copyright (C) 2020-2021 Edward Lynch-Milner
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
     * Gets the permissions as a string in the unix form of ls command. For Non-posix systems, this just displays the permissions for the user running the program
     * @return the permissions as a string
     */
    String getPermissions();

    /**
     * Gets the modification time as a formatted String in the form of Month Day Hour:Minute, e.g Jan 01 12:50
     * @return the formatted modification time String
     * @throws FileSystemException if an error occurs
     */
    String getModificationTime() throws FileSystemException;

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

    /**
     * Refreshes the file if the file implementation caches certain info. E.g a remote file may rather than making multiple calls to the server
     */
    void refresh() throws FileSystemException;

    /**
     * Checks if this file is a symbolic link. To determine what type of file the symbolic link points to, call isANormalFile or isADirectory
     * @return true if it is a symbolic link
     */
    boolean isSymbolicLink();

    /**
     * Gets the target of the symbolic link. This target path is canonicalized based on the parent directory of the symbolic link (i.e. parent path of the symbolic link)
     * @return the symbolic link target, null if not symbolic link
     * @throws FileSystemException if an error occurs
     */
    String getSymbolicLinkTarget() throws FileSystemException;

    /**
     * This file may be present locally or remotely. This method determines if it is local or remote
     * @return true if local, false if remote
     */
    boolean isLocal();

    /**
     * This method retrieves the <b>next</b> available parent of this file. What this means is that the parent retrieved
     * may not be the immediate parent of this file, it is just the next parent that exists. It is guaranteed to not be null
     * as the one parent that will always exist is the root file.
     * @return the next available parent of this file
     * @throws FileSystemException if an error occurs retrieving the parent
     */
    CommonFile getExistingParent() throws FileSystemException;
}
