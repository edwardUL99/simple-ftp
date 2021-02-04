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

import com.simpleftp.filesystem.CopyMoveOperation;
import com.simpleftp.filesystem.LocalFileSystem;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;

import java.util.HashMap;

/**
 * This class represents a FileService for use with local filesystems
 */
class LocalFileService extends FileService {
    /**
     * A HashMap to determine if an active (connected and logged in) connection is required for the operation.
     * A remote file service clearly always needs an active one, but not local
     */
    private static final HashMap<Operation, Boolean> connectionRequired = new HashMap<>();
    /**
     * A HashMap to determine if a connection is required for the COPY/MOVE operation.
     * This map is only checked if the Operation is a COPY or MOVE. It may be "required" for a COPY/MOVE but only used in certain scenarios
     */
    private static final HashMap<CopyMoveOperation, Boolean> connectionUsed = new HashMap<>();

    static {
        connectionRequired.put(Operation.COPY, true);
        connectionRequired.put(Operation.MOVE, true);
        connectionRequired.put(Operation.REMOVE, false);
        connectionUsed.put(CopyMoveOperation.LOCAL_TO_LOCAL, false);
        connectionUsed.put(CopyMoveOperation.REMOTE_TO_LOCAL, true); // we don't need to be concerned about REMOTE_TO_REMOTE or LOCAL_TO_REMOTE on a local FileService
    }

    /**
     * Creates a local file service with the provided parameters
     * @param source the source file this service is to target
     * @param destination the destination directory this service is to copy/move to
     * @param operation the operation this file service is to carry out
     */
    LocalFileService(CommonFile source, CommonFile destination, Operation operation) {
        super(source, destination, operation);
    }

    /**
     * Determines if a connection is required for this LocalFileService
     * @return true if a connection is required, false if not
     */
    private boolean connectionRequired() {
        boolean opConnectionReq = connectionRequired.get(operation);

        if (operation == Operation.COPY || operation == Operation.MOVE) {
            boolean sourceLocal = getSource().isLocal();
            boolean destLocal = getDestination().isLocal();

            CopyMoveOperation copyMoveOperation;

            if (sourceLocal && destLocal) {
                copyMoveOperation = CopyMoveOperation.LOCAL_TO_LOCAL;
            } else if (!sourceLocal && destLocal) {
                copyMoveOperation = CopyMoveOperation.REMOTE_TO_LOCAL;
            } else {
                throw new IllegalArgumentException("Invalid combination of source and destination file localities given to LocalFileService");
            }

            opConnectionReq = opConnectionReq && connectionUsed.get(copyMoveOperation);
        }

        return opConnectionReq;
    }

    /**
     * Retrieves the FileSystem to use for this FileService. Lazily initialises this FileService's FileSystem instance
     *
     * @return the file system to use for this FileService
     * @throws FileSystemException if an exception occurs creating the file system
     */
    @Override
    FileSystem getFileSystem() throws FileSystemException {
        if (fileSystem == null) {
            FTPConnection connection = null;
            try {
                if (connectionRequired()) {
                    connection = FTPConnection.createTemporaryConnection(FTPSystem.getConnection());
                    connection.connect();
                    connection.login();
                }
            } catch (FTPException ex) {
                throw new FileSystemException("Couldn't initialise the FileSystem for this FileService", ex);
            }

            fileSystem = new LocalFileSystem(connection);
        }

        return fileSystem;
    }
}
