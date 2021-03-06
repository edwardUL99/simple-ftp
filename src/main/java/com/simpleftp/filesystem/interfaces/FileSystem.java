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

import com.simpleftp.filesystem.FileOperationError;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.ftp.connection.FTPConnection;

/**
 * This interface outlines functionality that all file systems can share, e.g. a local and remote file system.
 *
 * A FileSystem provides a subset of operations which operates on files both locally and remote.
 * It can either be permanent or temporary. A permanent filesystem is one that is to be used for a long period of
 * time and usually for the entire application runtime. This means that it performs multiple actions.
 * Conversely, a temporary filesystem is one that is used for a single task and has a short time where it remains in scope. These
 * such filesystems are useful for doing a certain task in a background task.
 * <p>
 * An implementing FileSystem should have the following constructors:
 * <ol>
 *     <li>A no-arg constructor - Uses the system's connection and creates a permanent file system (setting appropriate flags)</li>
 *     <li>A constructor taking an FTPConnection object - Ensures the connection is not the same as the system's connection and throws IllegalArgumentException if it is</li>
 * </ol>
 *
 * Implementing class' constructors, after calling super, can do their own necessary validations then.
 *
 * All implementing FileSystems should extend AbstractFileSystem.
 */
public interface FileSystem {
    /**
     * Add the specified file to the file system. This method can only add a single file to the filesystem.
     * To add multiple files, use copyFiles or moveFiles
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
     * A FTP connection is required for both local and remote file systems as local file system needs to be able to download from the ftp server.
     * If this is a permanent file system, this should return the system connection. If it is temporary, it should return the file system's own connection instance
     * that was passed in to the temporary FileSystem constructor
     * @return the connection being used, may be null
     */
    FTPConnection getFTPConnection();

    /**
     * This is the method which will implement the copy operation. It permits copying between different filesystems and also within the same filesystem.
     * The behaviour of the method depends on the implementation types of the source and destination parameters. If the types are both the same,
     * the copying will take place on the same file system those files are defined for.
     * If the types are different, it is for copying between different file systems, i.e. copying from the file defined for that file system to the file system defined for the
     * destination file.
     * If the types provided are in the wrong order, or both the same types but not the matching type for that file system, an IllegalArgumentException should be thrown.
     * See the documentation of the implementing file systems for the file types and orders expected.
     * It is up to each implementation on the rules for the ordering and types of files allowed/expected when determining the copy behaviour, example on same file system, or between different file systems.
     *
     * An example of this is if source is an instance of RemoteFile and destination is an instance of LocalFile, that could be defined as Remote to local copy (as defined in LocalFileSystem).
     * The implementing filesystem should define the logical copy operations based on the provided files and thus implement the logic for those operations.
     * <p>
     * A copy to another directory on the same remote file system requires a local copy. I.e. it requires a download of the file to a temp folder and then upload it to the destination
     * folder
     *
     * @param source the file representing the file to copy. Can be a directory or a file
     * @param destination the file representing the <b>directory</b> the source will be copied to. If this file is not a directory, an IllegalArgumentException should be thrown
     * @return true if a success, false if not
     */
    boolean copyFiles(CommonFile source, CommonFile destination) throws FileSystemException;

    /**
     * This is the method which will implement the move operation. It permits moving between different filesystems and also within the same filesystem.
     * The behaviour of the method depends on the implementation types of the source and destination parameters. If the types are both the same,
     * the moving will take place on the same file system those files are defined for.
     * If the types are different, it is for moving between different file systems, i.e. moving from the file defined for that file system to the file system defined for the
     * destination file.
     * If the types provided are in the wrong order, or both the same types but not the matching type for that file system, an IllegalArgumentException should be thrown.
     * See the documentation of the implementing file systems for the file types and orders expected.
     * It is up to each implementation on the rules for the ordering and types of files allowed/expected when determining the move behaviour, example on same file system, or between different file systems.
     *
     * An example of this is if source is an instance of RemoteFile and destination is an instance of LocalFile, that could be defined as Remote to local move (as defined in LocalFileSystem)
     * The implementing filesystem should define the logical move operations based on the provided files and thus implement the logic for those operations.
     * <p>
     * A move from local to remote requires a upload and then deletion on the local file system of the source file. Remote to local requires a download and then deletion on
     * the remote file system of the source file.
     *
     * @param source the file representing the file to move. Can be a directory or a file
     * @param destination the file representing the <b>directory</b> the source will be copied to. If this file is not a directory, an IllegalArgumentException should be thrown
     * @return true if a success, false if not
     */
    boolean moveFiles(CommonFile source, CommonFile destination) throws FileSystemException;

    /**
     * Retrieves the next file operation error if there is one available. These errors are non-fatal errors produced by copyFiles or
     * moveFiles/removeFile if source file is a directory.
     * @return the next FileOperationError if available, null if not
     */
    FileOperationError getNextFileOperationError();

    /**
     * Returns true if there is a file operation error available to retrieve
     * @return true if there is a FileOperationError, false if not
     */
    boolean hasNextFileOperationError();
}
