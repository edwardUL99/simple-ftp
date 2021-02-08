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

package com.simpleftp.filesystem;

import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;

import java.util.LinkedList;
import java.util.Queue;

/**
 * This is an abstract file system class to keep track of the temporaryFileSystem variable and also ensuring the rule that if true,
 * the connection should be different to the system connection
 *
 * All FileSystems should extend this class
 */
public abstract class AbstractFileSystem implements FileSystem {
    /**
     * The connection backing this file system.
     */
    protected FTPConnection ftpConnection;
    /**
     * The list of CopyMoveErrors produced by the FileSystem
     */
    protected final Queue<FileOperationError> fileOperationErrors;
    /**
     * A boolean flag to specify that the filesystem was created using the constructor taking a connection
     */
    private final boolean temporaryFileSystem;

    /**
     * Constructs an AbstractFileSystem with temporaryFileSystem set to false.
     */
    protected AbstractFileSystem() {
        fileOperationErrors = new LinkedList<>(); // a LinkedList implements Deque which extends Queue
        temporaryFileSystem = false;
    }

    /**
     * Constructs an AbstractFileSystem with temporaryFileSystem set to true
     * @param ftpConnection the connection to use. If reference equal to the system connection, an IllegalArgumentException is thrown
     */
    protected AbstractFileSystem(FTPConnection ftpConnection) {
        FTPConnection systemConnection = FTPSystem.getConnection();
        if (systemConnection != null && ftpConnection == systemConnection)
            throw new IllegalArgumentException("The connection provided to this constructor cannot be the same as the FTPSystem connection, use FTPConnection.createTemporaryConnection(FTPSystem.getConnection())");
        fileOperationErrors = new LinkedList<>(); // a LinkedList implements Deque which extends Queue
        temporaryFileSystem = true;
        this.ftpConnection = ftpConnection;
    }

    /**
     * This method returns true if this file system is a temporary one. i.e. it is not (and should not) using the system connection.
     * It is to be only used for single tasks like background tasks. Implementing classes should have a default constructor which sets a temporaryFileSystem
     * instance variable to false and the constructor taking a specified connection should set it to true. That constructor should ensure the given connecction is not reference equal
     * to the system's connection
     *
     * @return true if a temporary file system, false if not
     */
    public boolean isTemporaryFileSystem() {
        return temporaryFileSystem;
    }

    /**
     * Returns the FTPConnection the file system is linked to.
     * A FTP connection is required for both local and remote file systems as local file system needs to be able to download from the ftp server
     *
     * @return the connection being used, may be null
     */
    @Override
    public FTPConnection getFTPConnection() {
        return temporaryFileSystem ? ftpConnection:FTPSystem.getConnection();
    }

    /**
     * Retrieves the next copy move error if there is one available. These errors are non-fatal errors produced by copyFiles or
     * moveFiles if source file is a directory.
     *
     * @return the next FileOperationError if available, null if not
     */
    @Override
    public FileOperationError getNextFileOperationError() {
        return fileOperationErrors.poll();
    }

    /**
     * Returns true if there is a copy move error available to retrieve
     *
     * @return true if there is a FileOperationError, false if not
     */
    @Override
    public boolean hasNextFileOperationError() {
        return !fileOperationErrors.isEmpty();
    }
}
