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

package com.simpleftp.ui.panels;

import com.simpleftp.filesystem.FileUtils;
import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ftp.exceptions.FTPRemotePathNotFoundException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.background.BundledServices;
import com.simpleftp.ui.background.FileService;
import com.simpleftp.ui.directories.DirectoryPane;
import com.simpleftp.ui.directories.RemoteDirectoryPane;
import com.simpleftp.ui.files.LineEntry;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class represents a RemoteFilePanel storing a DirectoryPane for remote files
 */
public final class RemoteFilePanel extends FilePanel {
    /**
     * Constructs a RemoteFilePanel with the provided directoryPane
     * @param directoryPane the directory pane for this Panel to display
     */
    RemoteFilePanel(DirectoryPane directoryPane) {
        super(directoryPane);
    }

    /**
     * Creates a remote directory on the server
     * @param path the path for the directory
     * @param connection the connection to create it with
     * @return true if it succeeds false if not
     * @throws FTPException if an error occurs
     */
    private boolean createRemoteDirectory(String path, FTPConnection connection) throws FTPException {
        if (connection.makeDirectory(path)) {
            UI.doInfo("Directory Created", "The directory: " + path + " has been created successfully");
            return true;
        } else {
            String reply = connection.getReplyString();
            if (reply.trim().startsWith("2")) {
                reply = "Path is either a file or already a directory"; // this was a successful reply, so that call must have been checking remotePathExists in FTPConnection.makeDirectory
            }
            UI.doError("Directory Not Created", "Failed to make directory with path: " + path + " with error: " + reply);

            return false;
        }
    }

    /**
     * Creates a normal file on the remote server
     * @param path the path to create
     * @param connection the connection to create the file with
     * @return true if succeeds, false if not
     * @throws FTPException if a FTP error occurs
     * @throws IOException if a local error occurs
     */
    private boolean createRemoteNormalFile(String path, FTPConnection connection) throws FTPException, IOException {
        String parentPath = FileUtils.getParentPath(path, false);
        // need to make a local file first and then upload
        if (!connection.remotePathExists(path, false)) {
            String fileName = new File(path).getName();
            LocalFile localFile = new LocalFile(FileUtils.TEMP_DIRECTORY + FileUtils.PATH_SEPARATOR + fileName);
            if (localFile.exists())
                localFile.delete(); // it's in a temp directory, so can be deleted

            if (localFile.createNewFile() && connection.uploadFile(localFile, parentPath) != null) {
                UI.doInfo("File Created", "The file: " + path + " has been created successfully");
                localFile.delete();

                return true;
            } else {
                String reply = connection.getReplyString();
                if (reply.trim().startsWith("2")) {
                    reply = "Path is either a directory or already a file"; // this was a successful reply, so that call must have been checking remotePathExists in FTPConnection.makeDirectory
                }
                UI.doError("File Not Created", "Failed to make file with path: " + path + " with reply: " + reply);

                return false;
            }

        } else {
            UI.doError("File Already Exists", "File with path: " + path + " already exists");

            return false;
        }
    }

    /**
     * Handler for creating a remote directory
     * @param resolvedPath the resolved path for the directory
     * @param directory true if directory, false if file
     * @return true if it succeeds, false if not
     */
    private boolean createRemoteFile(String resolvedPath, boolean directory) {
        try {
            CommonFile file = directoryPane.getDirectory();
            String currentPath = file.getFilePath();
            FTPConnection connection = directoryPane.getFileSystem().getFTPConnection();

            String parentPath = FileUtils.getParentPath(resolvedPath, false);
            boolean existsAsDir = connection.remotePathExists(parentPath, true);

            if (!existsAsDir) {
                UI.doError("Directory does not exist", "Cannot create directory as path: " + parentPath + " does not exist");
                return false;
            } else {
                boolean succeeded;
                if (directory) {
                    succeeded = createRemoteDirectory(resolvedPath, connection);
                } else {
                    succeeded = createRemoteNormalFile(resolvedPath, connection);
                }

                if (!succeeded)
                    return false;

                boolean parentPathMatchesPanelsPath = FileUtils.pathEquals(currentPath, parentPath, false);

                if (parentPathMatchesPanelsPath) {
                    directoryPane.refreshCurrentDirectory(); // only need to refresh if the path the file is created in matches the cwd
                } else {
                    ((RemoteDirectoryPane)directoryPane).refreshCache(parentPath); // refresh the cache for the parent path as remote directory pane uses caching
                }

               return true;
            }
        } catch (FTPException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
        } catch (IOException ex) {
            UI.doException(ex, UI.ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled());
        }

        return false;
    }

    /**
     * The handler for creating a new directory
     */
    @Override
    public void createNewDirectory() {
        AtomicBoolean openCreatedDirectory = new AtomicBoolean(false);
        String path = UI.doCreateDialog(true, () -> openCreatedDirectory.set(true));
        try {
            if (path != null) {
                String resolvedPath = UI.resolveRemotePath(path, directoryPane.getCurrentWorkingDirectory(), false, directoryPane.getFileSystem().getFTPConnection());
                if (createRemoteFile(resolvedPath, true) && openCreatedDirectory.get())
                    directoryPane.openLineEntry(LineEntry.newInstance(new RemoteFile(resolvedPath), directoryPane));
            }
        } catch (FTPException | FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
        }
    }

    /**
     * The handler to create a new empty file
     */
    @Override
    public void createNewFile() {
        AtomicBoolean openCreatedFile = new AtomicBoolean(false);
        String path = UI.doCreateDialog(false, () -> openCreatedFile.set(true));
        try {
            if (path != null) {
                String resolvedPath = UI.resolveRemotePath(path, directoryPane.getCurrentWorkingDirectory(), false, directoryPane.getFileSystem().getFTPConnection());
                if (createRemoteFile(resolvedPath, false) && openCreatedFile.get()) {
                    try {
                        directoryPane.openLineEntry(LineEntry.newInstance(new RemoteFile(resolvedPath), directoryPane));
                    } catch (FileSystemException ex) {
                        UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
                    }
                }
            }
        } catch (FTPException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
        }
    }

    /**
     * Defines how a symbolic link should be created
     */
    @Override
    public void createSymbolicLink() {
        UI.doInfo("Symbolic Link Creation", "To create a symbolic link, contact the Server Administrator to create the link in the FTP location on the server");
    }

    /**
     * Takes the given path and attempts to go the the location in the remote file system identified by it
     * @param path the path to go to
     */
    private void goToRemotePath(String path) {
        FileSystem fileSystem = directoryPane.getFileSystem();

        try {
            String currWorkingDir = directoryPane.getCurrentWorkingDirectory();
            path = !path.startsWith("/") ? FileUtils.addPwdToPath(currWorkingDir, path, "/"):path;
            String symbolicPath = UI.resolveSymbolicPath(path, "/", "/"); // do symbolic path, first, in case it doesn't exist. Local file resolves .. for the not found dialog, this would do the same here
            if (symbolicPath == null)
                return; // this is a rare case. UI.resolveSymbolicPath would have shown an error dialog

            RemoteFile remoteFile = new RemoteFile(symbolicPath);

            if (!remoteFile.exists())
                throw new FTPRemotePathNotFoundException("The path " + symbolicPath + " does not exist", symbolicPath);

            boolean canonicalize = remoteFile.isADirectory() && (!remoteFile.isSymbolicLink() || !UI.doSymbolicPathDialog(symbolicPath)); // only open dialog if it a directory, opening a file doesn't matter
            if (canonicalize) {
                path = UI.resolveRemotePath(symbolicPath, currWorkingDir, true, fileSystem.getFTPConnection());
                remoteFile = new RemoteFile(path);
            }

            if (remoteFile.isADirectory()) {
                directoryPane.setDirectory(remoteFile);
                directoryPane.cacheRefresh(); // go to path refreshing using cache if the entered directory is cached
            } else if (remoteFile.isNormalFile()) {
                LineEntry lineEntry = LineEntry.newInstance(remoteFile, directoryPane); // we are not sure the path is a sym link, so use this work around in case. See the JavaDoc for that method to see why it is a workaround
                if (lineEntry != null)
                    directoryPane.openLineEntry(lineEntry);
            } else {
                // this should be caught above, but it is here defensively
                UI.doError("Path does not exist", "The path: " + remoteFile.getFilePath() + " does not exist or it is not a directory");
            }
        } catch (Exception ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
        }
    }

    /**
     * The handler for the goto button
     */
    @Override
    public void gotoPath() {
        String path = UI.doPathDialog();

        if (path != null)
            goToRemotePath(path);
    }

    /**
     * Deletes the list of line entries
     *
     * @param entries the entries to delete
     */
    @Override
    void doMultipleDelete(List<LineEntry> entries) {
        BundledServices bundle = new BundledServices(FileService.Operation.REMOVE);

        for (LineEntry entry : entries) {
            CommonFile source = entry.getFile();
            bundle.bundle(FileService.newInstance(source, null, FileService.Operation.REMOVE, false)
                            .setOnOperationSucceeded(() -> fileDeleteSucceeded(entry, false))
                            .setOnOperationFailed(() -> fileDeleteFailed(source)));
        }

        bundle.activate();
    }
}
