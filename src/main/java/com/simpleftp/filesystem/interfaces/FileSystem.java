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
import com.simpleftp.ftp.connection.FTPConnection;

/**
 * This interface outlines functionality that all file systems can share, e.g. a local and remote file system
 */
public interface FileSystem {
    /**
     * Add the specified file to the file system
     * @param file the representation of the file to add
     * @param path the path to the dir to add the file to on the system
     * @return true if it was a success, false if not
     * @throws FileSystemException if an error occurs
     */
    boolean addFile(CommonFile file, String path) throws FileSystemException;

    /**
     * Removes the specified file from the file system
     * @param file the representation of the file to remove
     * @return true if it was a success, false if not
     * @throws FileSystemException if an error occurs
     */
    boolean removeFile(CommonFile file) throws FileSystemException;

    /**
     * Removes the file specified by the path name
     * @param fileName the name of the file (can be a path)
     * @return true if it was a success, false if not
     * @throws FileSystemException if an error occurs
     */
    boolean removeFile(String fileName) throws FileSystemException;

    /**
     * Attempts to find the specified file and returns it
     * @param fileName the name/path to the file
     * @return the file if found, null if not
     */
    CommonFile getFile(String fileName) throws FileSystemException;

    /**
     * Checks if the specified file name exists
     * @param fileName the name/path to the file
     * @return true if the file exists, false if not
     */
    boolean fileExists(String fileName) throws FileSystemException;

    /**
     * List the files in the specified dir in the file system
     * @param dir the directory path
     * @return an array of files if found, null if not
     */
    CommonFile[] listFiles(String dir) throws FileSystemException;

    /**
     * Returns the FTPConnection the file system is linked to.
     * A FTP connection is required for both local and remote file systems as local file system needs to be able to download from the ftp server
     * @return the connection being used
     */
    FTPConnection getFTPConnection();
}
