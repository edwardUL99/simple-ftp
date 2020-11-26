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

package com.simpleftp.ui.containers;

import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.filesystem.paths.ResolvedPath;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.files.LineEntry;
import com.simpleftp.ui.panels.FilePanel;

import java.io.File;
import java.io.IOException;

/**
 * This class represents a FilePanelContainer storing a FilePanel for remote files
 */
final class RemoteFilePanelContainer extends FilePanelContainer {
    /**
     * Constructs a RemoteFilePanelContainer with the provided filePanel
     * @param filePanel the panel for this container to hold
     */
    RemoteFilePanelContainer(FilePanel filePanel) {
        super(filePanel);
    }

    /**
     * Creates a remote directory on the server
     * @param path the path for the directory
     * @param connection the connection to create it with
     * @throws FTPException if an error occurss
     */
    private void createRemoteDirectory(String path, FTPConnection connection) throws FTPException {
        if (connection.makeDirectory(path)) {
            UI.doInfo("Directory Created", "The directory: " + path + " has been created successfully");
        } else {
            String reply = connection.getReplyString();
            if (reply.trim().startsWith("2")) {
                reply = "Path is either a file or already a directory"; // this was a successful reply, so that call must have been checking remotePathExists in FTPConnection.makeDirectory
            }
            UI.doError("Directory Not Created", "Failed to make directory with path: " + path + " with error: " + reply);
        }
    }

    /**
     * Creates a normal file on the remote server
     * @param path the path to create
     * @param connection the connection to create the file with
     * @throws FTPException if a FTP error occurs
     * @throws IOException if a local error occurs
     */
    private void createRemoteNormalFile(String path, FTPConnection connection) throws FTPException, IOException {
        String parentPath = UI.getParentPath(path);
        // need to make a local file first and then upload
        if (!connection.remotePathExists(path, false)) {
            String fileName = new File(path).getName();
            LocalFile localFile = new LocalFile(UI.TEMP_DIRECTORY + UI.PATH_SEPARATOR + fileName);
            if (localFile.exists())
                localFile.delete(); // it's in a temp directory, so can be deleted

            if (localFile.createNewFile() && connection.uploadFile(localFile, parentPath) != null) {
                UI.doInfo("File Created", "The file: " + path + " has been created successfully");
            } else {
                String reply = connection.getReplyString();
                if (reply.trim().startsWith("2")) {
                    reply = "Path is either a directory or already a file"; // this was a successful reply, so that call must have been checking remotePathExists in FTPConnection.makeDirectory
                }
                UI.doError("File Not Created", "Failed to make file with path: " + path + " with reply: " + reply);
            }

            localFile.delete();
        } else {
            UI.doError("File Already Exists", "File with path: " + path + " already exists");
        }
    }

    /**
     * Handler for creating a remote directory
     * @param path the path for the directory
     * @param directory true if directory, false if file
     */
    private void createRemoteFile(String path, boolean directory) {
        try {
            CommonFile file = filePanel.getDirectory();
            String currentPath = file.getFilePath();
            FTPConnection connection = filePanel.getFileSystem().getFTPConnection();

            ResolvedPath resolvedPath = UI.resolveRemotePath(path, currentPath, false, connection);
            path = resolvedPath.getResolvedPath();
            boolean absolute = resolvedPath.isPathAlreadyAbsolute();

            String parentPath = UI.getParentPath(path);
            boolean existsAsDir = connection.remotePathExists(parentPath, true);

            if (!existsAsDir) {
                UI.doError("Directory does not exist", "Cannot create directory as path: " + parentPath + " does not exist");
            } else {
                if (directory) {
                    createRemoteDirectory(path, connection);
                } else {
                     createRemoteNormalFile(path, connection);
                }

                boolean parentPathMatchesPanelsPath = currentPath.equals(parentPath);
                if (!absolute && (parentPathMatchesPanelsPath || UI.isFileSymbolicLink(file))) {
                    filePanel.refresh(); // only need to refresh if the path was relative (as the directory would be created in the current folder) or if absolute and the prent path doesnt match current path. The path identified by the absolute will be refreshed when its navigated to
                } else if (parentPathMatchesPanelsPath) {
                    filePanel.refresh();
                }
            }
        } catch (FTPException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
        } catch (IOException ex) {
            UI.doException(ex, UI.ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled());
        }
    }

    /**
     * The handler for creating a new directory
     */
    @Override
    void createNewDirectory() {
        String path = UI.doPathDialog(UI.PathAction.CREATE, true);

        if (path != null)
            createRemoteFile(path, true);
    }

    /**
     * The handler to create a new empty file
     */
    @Override
    void createNewFile() {
        String path = UI.doPathDialog(UI.PathAction.CREATE, false);

        if (path != null)
            createRemoteFile(path, false);
    }

    /**
     * Takes the given path and attempts to go the the location in the remote file system identified by it
     * @param path the path to go to
     */
    private void goToRemotePath(String path) throws FileSystemException {
        FileSystem fileSystem = filePanel.getFileSystem();
        FTPConnection connection = fileSystem.getFTPConnection();

        try {
            ResolvedPath resolvedPath = UI.resolveRemotePath(path, filePanel.getCurrentWorkingDirectory(), true, fileSystem.getFTPConnection());
            path = resolvedPath.getResolvedPath();

            if (connection.remotePathExists(path, true)) {
                CommonFile file = fileSystem.getFile(path);
                filePanel.setDirectory(file);
                filePanel.refresh();
            } else if (connection.remotePathExists(path, false)) {
                LineEntry lineEntry = LineEntry.newInstance(new RemoteFile(path), filePanel);
                if (lineEntry != null)
                    filePanel.openLineEntry(lineEntry);
            } else {
                UI.doError("Path does not exist", "The path: " + path + " does not exist or it is not a directory");
            }
        } catch (FTPException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
        }
    }

    /**
     * The handler for the goto button
     */
    @Override
    void gotoPath() {
        String path = UI.doPathDialog(UI.PathAction.GOTO, true); // directory is irrelevant here for GOTO, but pass to compile

        if (path != null) {
            try {
                goToRemotePath(path);
            } catch (FileSystemException ex) {
                UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
            }
        }
    }
}