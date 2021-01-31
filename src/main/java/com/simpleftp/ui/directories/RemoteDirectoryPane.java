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

package com.simpleftp.ui.directories;

import com.simpleftp.filesystem.FileUtils;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.RemoteFileSystem;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.properties.Properties;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.background.FileService;
import com.simpleftp.ui.files.LineEntries;
import com.simpleftp.ui.files.LineEntry;

import java.util.HashMap;

/**
 * This DirectoryPane displays files on the remote file server
 */
public final class RemoteDirectoryPane extends DirectoryPane {
    /**
     * A cached list of entries to reduce time spent navigating without an update in already visited directories on a remote pane
     */
    private final HashMap<String, LineEntries> cachedEntries;

    /**
     * Constructs a RemoteDirectoryPane with the given directory to initialise this panel with
     * @param directory the initial directory to display
     */
    RemoteDirectoryPane(RemoteFile directory) throws FileSystemException {
        super();
        fileSystem = new RemoteFileSystem();
        cachedEntries = Properties.CACHE_REMOTE_DIRECTORY_LISTING.getValue() ? new HashMap<>():null;
        initDirectory(directory);
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
                UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
            }
        }
    }

    /**
     * Refreshes the directory pane after a rename
     * @param newPath the new path the file has to be renamed to
     */
    private void refreshAfterRename(String newPath) {
        double vPosition = entriesScrollPane.getVvalue();
        double hPosition = entriesScrollPane.getHvalue();
        String parent = FileUtils.getParentPath(newPath, false);
        refreshCurrentDirectory();
        if (!FileUtils.pathEquals(parent, getCurrentWorkingDirectory(), false))
            refreshCache(parent); // refresh the cache of the directory the file was renamed to if it wasn't renamed to the same directory
        entriesScrollPane.setVvalue(vPosition);
        entriesScrollPane.setHvalue(hPosition); // resets the position of the scrollbars to where they were before the refresh
    }

    /**
     * Renames the specified remote file
     * @param lineEntry the line entry to rename
     */
    private void renameRemoteFile(final LineEntry lineEntry) {
        RemoteFile remoteFile = (RemoteFile)lineEntry.getFile();
        String filePath = remoteFile.getFilePath();

        String fileName = remoteFile.getName();
        String newPath = UI.doRenameDialog(fileName);

        if (newPath != null) {
            try {
                String pwd = getCurrentWorkingDirectory();
                newPath = FileUtils.addPwdToPath(pwd, newPath, "/");
                newPath = UI.resolveSymbolicPath(newPath, "/", "/"); // we will use symbolic path resolving as we may want to rename a file to a symbolic path
                RemoteFile newFile = new RemoteFile(newPath);
                if (overwriteExistingFile(newFile)) {
                    FTPConnection connection = fileSystem.getFTPConnection();
                    if (connection.renameFile(filePath, newPath)) {
                        UI.doInfo("File Renamed", "File has been renamed successfully");
                        refreshAfterRename(newPath);
                    } else {
                        String replyString = connection.getReplyString();
                        UI.doError("Rename Failed", "Failed to rename file with error code: " + replyString);
                    }
                }
            } catch (FTPException | FileSystemException ex) {
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
        String filePath = lineEntry.getFilePath();

        if (!UI.isFileOpened(filePath, false)) {
            renameRemoteFile(lineEntry);
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
     * @param path the path to list
     */
    private void constructListOfRemoteFiles(LineEntries lineEntries, String path) {
        try {
            RemoteFile[] files = (RemoteFile[])fileSystem.listFiles(path);
            if (files == null) {
                UI.doError("Path does not exist", "The path " + path + " does not exist");
            } else if (files.length == 0) {
                lineEntries.clear();
            } else {
                for (RemoteFile f : files) {
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
            }
        } catch (FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
            lineEntries.clear();
        }
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
        return fileSystem.removeFile(remoteFile);
    }

    /**
     * Lazy Initialises the root directory for this directory pane and returns it
     *
     * @return the root directory
     * @throws FileSystemException if an error occurs in creating the file
     */
    @Override
    CommonFile getRootDirectory() throws FileSystemException {
        if (rootDirectory == null) {
            rootDirectory = new RemoteFile(FileUtils.getRootPath(false));
        }

        return rootDirectory;
    }

    /**
     * Constructs the list of line entries to display
     * @param useCache true to use cached line entries if any, or false to create a new list
     * @param removeAllCache if useCache is false, then removeAllCache means that all cached line entries should be removed, if false, just current directory
     * @return the list of constructed line entries
     */
    @Override
    LineEntries constructListOfFiles(boolean useCache, boolean removeAllCache) {
        String currentDirectory = getCurrentWorkingDirectory();
        LineEntries lineEntries;

        if (Properties.CACHE_REMOTE_DIRECTORY_LISTING.getValue()) {
            lineEntries = cachedEntries.get(currentDirectory);
            if (lineEntries != null && lineEntries.size() > 0 && useCache) {
                lineEntries.setSort(false);
                return lineEntries;
            } else {
                lineEntries = lineEntries == null ? new LineEntries() : lineEntries;
                if (!useCache) {
                    if (removeAllCache) {
                        cachedEntries.clear();
                    }

                    lineEntries.clear();
                }

                cachedEntries.put(currentDirectory, lineEntries);
            }
        } else {
            lineEntries = new LineEntries();
        }

        constructListOfRemoteFiles(lineEntries, directory.getFilePath());
        lineEntries.setSort(true);

        return lineEntries;
    }

    /**
     * Refreshes the cached line entries for the provided file path if found. This should be used if the destination of an operation is not the current working directory
     * @param filePath the file path to refresh
     */
    public void refreshCache(String filePath) {
        if (Properties.CACHE_REMOTE_DIRECTORY_LISTING.getValue())
            cachedEntries.remove(filePath); // removing the cache for this file path will force a refresh on the next visit to this directory
    }

    /**
     * This method creates and schedules the file service used for copying/moving the source to destination
     *
     * @param source      the source file to be copied/moved
     * @param destination the destination directory to be copied/moved to
     * @param copy        true to copy, false to move
     * @param targetPane  the target pane if a different pane. Leave null if not different
     */
    @Override
    void scheduleCopyMoveService(CommonFile source, CommonFile destination, boolean copy, DirectoryPane targetPane) {
        String operationHeader = copy ? "Copy":"Move", operationMessage = copy? "copy":"move";
        FileService.newInstance(source, destination, copy ? FileService.Operation.COPY:FileService.Operation.MOVE, destination.isLocal())
                .setOnOperationSucceeded(() -> {
                    String destinationPath = destination.getFilePath();
                    String currentPath = directory.getFilePath();
                    UI.doInfo(operationHeader + " Completed", "The " + operationMessage + " of " + source.getFilePath()
                    + " to " + destinationPath + " has completed");

                    if (!copy) {
                        String parentPath = FileUtils.getParentPath(source.getFilePath(), source.isLocal());
                        if (FileUtils.pathEquals(parentPath, currentPath, false))
                            refreshCurrentDirectory();
                        else
                            refreshCache(parentPath); // if it was a move, we refresh so that the file no longer exists

                        if (copiedEntry != null && copiedEntry.getFile().equals(source))
                            copiedEntry = null; // After a move the copied entry will no longer be in the location it was
                    }

                    if (!FileUtils.pathEquals(destinationPath, currentPath, false))
                        refreshCache(destinationPath);

                    if (targetPane != null && targetPane.isLocal()) {
                        targetPane.refresh();
                    }
                })
                .setOnOperationFailed(() -> UI.doError(operationHeader + " Failed", "The " + operationMessage + " of " + source.getFilePath()
                + " to " + destination.getFilePath() + " has failed"))
                .schedule();
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
}
