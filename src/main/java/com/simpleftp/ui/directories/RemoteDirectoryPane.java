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

package com.simpleftp.ui.directories;

import com.simpleftp.filesystem.FileUtils;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.RemoteFileSystem;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.files.LineEntry;
import org.apache.commons.net.ftp.FTPFile;

import java.util.ArrayList;
import java.util.Collections;

/**
 * This DirectoryPane displays files on the remote file server
 */
final class RemoteDirectoryPane extends DirectoryPane {
    /**
     * Constructs a RemoteDirectoryPane with the given directory to initialise this panel with
     * @param directory the initial directory to display
     */
    RemoteDirectoryPane(RemoteFile directory) throws FileSystemException {
        super(directory);
    }

    /**
     * Initialises the file system for use with this DirectoryPane
     *
     * @throws FileSystemException if an error occurs initialising it
     */
    @Override
    void initFileSystem() throws FileSystemException {
        fileSystem = new RemoteFileSystem();
    }

    /**
     * An overridden version of the superclass' method which does a little bit of extra work changing the server's working directory
     * @param directory the directory to set
     */
    @Override
    void setDirectoryUnchecked(CommonFile directory) {
        super.setDirectoryUnchecked(directory);
        String path = directory.getFilePath();
        FTPConnection connection = fileSystem.getFTPConnection();
        if (connection != null) {
            try {
                boolean changed = connection.changeWorkingDirectory(path);

                if (!changed) {
                    UI.doError("Error changing directory", "Current directory may not have been changed successfully. FTP Reply: " + connection.getReplyString());
                }
            } catch (FTPException ex) {
                checkFTPConnectionException(ex);
                UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
            }
        }
    }

    /**
     * Renames the specified remote file
     * @param remoteFile the file to rename
     */
    private void renameRemoteFile(final RemoteFile remoteFile) {
        String filePath = remoteFile.getFilePath();

        String fileName = remoteFile.getName();
        String newPath = UI.doRenameDialog(fileName);

        if (newPath != null) {
            try {
                newPath = FileUtils.addPwdToPath(getCurrentWorkingDirectory(), newPath, "/");
                newPath = UI.resolveSymbolicPath(newPath, "/", null); // we will use symbolic path resolving as we may want to rename a file to a symbolic path
                if (overwriteExistingFile(new RemoteFile(newPath))) {
                    FTPConnection connection = fileSystem.getFTPConnection();
                    if (connection.renameFile(filePath, newPath)) {
                        UI.doInfo("File Renamed", "File has been renamed successfully");
                        double vPosition = entriesScrollPane.getVvalue();
                        double hPosition = entriesScrollPane.getHvalue();
                        refresh();
                        entriesScrollPane.setVvalue(vPosition);
                        entriesScrollPane.setHvalue(hPosition); // resets the position of the scrollbars to where they were before the refresh
                    } else {
                        String replyString = connection.getReplyString();
                        UI.doError("Rename Failed", "Failed to rename file with error code: " + replyString);
                    }
                }
            } catch (FTPException | FileSystemException ex) {
                if (ex instanceof FileSystemException) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof FTPException)
                        checkFTPConnectionException((FTPException)cause);
                } else {
                    checkFTPConnectionException((FTPException)ex);
                }
                UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
            }
        }
    }

    /**
     * Handles when rename is called on the context menu with the specified line entry
     *
     * @param lineEntry the line entry to rename the file of
     */
    @Override
    void renameLineEntry(LineEntry lineEntry) {
        CommonFile file = lineEntry.getFile();
        String filePath = file.getFilePath();

        if (!UI.isFileOpened(filePath, false)) {
            renameRemoteFile((RemoteFile) file);
        } else {
            UI.doError("File Open", "The file " + file.getName() + " is open, it cannot be renamed");
        }
    }

    /**
     * Checks the directory passed in to see if the type matches the dfile type this DirectoryPane is for.
     * setDirectory calls this
     *
     * @param directory the directory to check
     * @throws IllegalArgumentException if the type of directory is different to the type of the current one
     */
    @Override
    void checkFileType(CommonFile directory) throws IllegalArgumentException {
        if (!(directory instanceof RemoteFile))
            throw new IllegalArgumentException("The file type passed into this DirectoryPane must be an instance of RemoteFile");
    }

    /**
     * Constructs the list of line entries from the files listed by the remote file
     * @param lineEntries the list of line entries to populate
     * @param remoteFile the file to list
     */
    private void constructListOfRemoteFiles(ArrayList<LineEntry> lineEntries, RemoteFile remoteFile) {
        try {
            String path = remoteFile.getFilePath();
            for (CommonFile f : fileSystem.listFiles(path)) {
                boolean showFile = showFile(f, e -> {
                    String fileName = f.getName();
                    return (showHiddenFiles && !fileName.equals(".") && !fileName.startsWith("..")) || !fileName.startsWith(".");
                });

                if (showFile) {
                    LineEntry constructed = createLineEntry(f);

                    if (constructed != null)
                        lineEntries.add(constructed);
                }
            }
        } catch (FileSystemException ex) {
            Throwable cause = ex.getCause();

            if (cause instanceof FTPException)
                checkFTPConnectionException((FTPException)cause);
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
            lineEntries.clear();
        }
    }

    /**
     * This method manually deletes the FileSystem remove method as there is a limitation when it comes to symbolic files, that the file system remove emthod fails
     * @param file the file to manually remove
     * @return the result
     * @throws FTPException if any FTPException is thrown
     */
    private boolean manuallyRemoveFile(RemoteFile file) throws FTPException {
        FTPConnection connection = fileSystem.getFTPConnection();
        return connection.removeFile(file.getFilePath());
    }

    /**
     * Does the remove action for the file. Removal of a file may differ between local and remote file panels, hence why abstract
     *
     * @param commonFile the file to remove
     * @return the result
     */
    @Override
    boolean doRemove(CommonFile commonFile) throws Exception {
        RemoteFile remoteFile = (RemoteFile)commonFile;

        if (remoteFile.isSymbolicLink()) {
            String filePath = remoteFile.getFilePath();
            remoteFile = RemoteFile.getSymbolicFile(fileSystem.getFTPConnection(), filePath);
            if (remoteFile == null)
                return false;

            return manuallyRemoveFile(remoteFile);
        } else {
            return fileSystem.removeFile(remoteFile);
        }
    }

    /**
     * Constructs the list of line entries to display
     *
     * @return the list of constructed line entries
     */
    @Override
    ArrayList<LineEntry> constructListOfFiles() {
        ArrayList<LineEntry> lineEntries = new ArrayList<>();
        constructListOfRemoteFiles(lineEntries, (RemoteFile)directory);

        if (lineEntries.size() > 0) {
            Collections.sort(lineEntries);
        }

        return lineEntries;
    }

    /**
     * This method determines if a DirectoryPane is a local one. All Remote panes, regardless of the underlying connection is still remote, so they should return false
     *
     * @return false
     */
    @Override
    public boolean isLocal() {
        return false;
    }

    /**
     * Attempts to change to the remote parent directory. If not symbolic link this is connection.changeToParentDirectory, if symbolic, it is the parent folder containing symbolic link
     * @throws FileSystemException if an exception occurs
     */
    private void changeToRemoteParent() throws FileSystemException {
        String parentPath = FileUtils.getParentPath(directory.getFilePath(), false); // the directory's path should be the current one
        RemoteFile parentFile = new RemoteFile(parentPath);
        setDirectory(parentFile);
        refresh();
    }

    /**
     * Controls going up to parent directory
     */
    @Override
    public void up() {
        if (!isAtRootDirectory()) {
            try {
                changeToRemoteParent();
            } catch (FileSystemException ex) {
                Throwable cause = ex.getCause();

                if (cause instanceof FTPException)
                    checkFTPConnectionException((FTPException)cause);

                UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
            }
        }
    }
}
