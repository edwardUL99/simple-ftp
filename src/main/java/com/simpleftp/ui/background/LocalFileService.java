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

package com.simpleftp.ui.background;

import com.simpleftp.filesystem.LocalFileSystem;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;

/**
 * This class represents a FileService for use with local filesystems
 */
class LocalFileService extends FileService {
    /**
     * Creates a local file service with the provided parameters
     * @param source the source file this service is to target
     * @param destination the destination directory this service is to copy/move to
     * @param copy true to copy, false to move
     */
    LocalFileService(CommonFile source, CommonFile destination, boolean copy) {
        super(source, destination, copy);
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
                throw new FileSystemException("Couldn't intialise the FileSystem for this FileService", ex);
            }

            fileSystem = new LocalFileSystem(connection);
        }

        return fileSystem;
    }
}
