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

package com.simpleftp.ui.background;

import com.simpleftp.filesystem.RemoteFileSystem;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;

/**
 * This class represents a file service for use with RemoteFileSystems
 */
class RemoteFileService extends FileService {
    /**
     * Constructs a RemoteFileService with the provided parameters
     * @param source the source file this service is copying/moving
     * @param destination the destination file this service is copying/moving to
     * @param operation the operation this file service is to carry out
     */
    RemoteFileService(CommonFile source, CommonFile destination, Operation operation) {
        super(source, destination, operation);
    }

    /**
     * Retrieves the FileSystem to use for this FileService. Lazily initiliases this FileService's FileSystem instance
     *
     * @return the file system to use for this FileService
     * @throws FileSystemException if an exception occurs creating the file system
     */
    @Override
    FileSystem getFileSystem() throws FileSystemException {
        if (fileSystem == null) {
            FTPConnection connection;
            try {
                connection = FTPConnection.createTemporaryConnection(FTPSystem.getConnection());
                connection.connect();
                connection.login();
            } catch (FTPException ex) {
                throw new FileSystemException("Couldn't initialise the FileSystem for this FileService", ex);
            }

            fileSystem = new RemoteFileSystem(connection);
        }

        return fileSystem;
    }
}
