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
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.paths.ResolvedPath;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.files.LineEntry;
import com.simpleftp.ui.panels.FilePanel;

import java.io.File;
import java.io.IOException;

/**
 * This class represents a FilePanelContainer storing a FilePanel for remote files
 */
final class LocalFilePanelContainer extends FilePanelContainer {
    /**
     * Constructs a LocalFilePanelContainer with the provided filePanel
     * @param filePanel the panel for this container to hold
     */
    LocalFilePanelContainer(FilePanel filePanel) {
        super(filePanel);
    }

    /**
     * Creates a local directory
     * @param path the specified path
     */
    private void createLocalDirectory(String path) {
        LocalFile file = new LocalFile(path);
        if (file.mkdir()) {
            UI.doInfo("Directory Created", "The directory: " + path + " has been created successfully");
        } else {
            UI.doError("Directory Not Created", "Failed to make directory with path: " + path);
        }
    }

    /**
     * Creates a local file
     * @param path the specified path
     */
    private void createLocalNormalFile(String path) {
        try {
            LocalFile file = new LocalFile(path);
            if (file.createNewFile()) {
                UI.doInfo("File Created", "The file: " + path + " has been created successfully");
            } else {
                UI.doError("File Not Created", "Failed to make file with path: " + path);
            }
        } catch (IOException ex) {
            UI.doException(ex, UI.ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled());
        }
    }

    /**
     * Handler for creating a local directory
     * @param path the path for the directory
     * @param directory true if to create a directory, false if file
     */
    private void createLocalFile(String path, boolean directory) {
        boolean absolute;
        String currentDirectory = filePanel.getCurrentWorkingDirectory();
        try {
            ResolvedPath resolvedPath = UI.resolveLocalPath(path, currentDirectory);
            path = resolvedPath.getResolvedPath();
            absolute = resolvedPath.isPathAlreadyAbsolute();
        } catch (IOException ex) {
            UI.doException(ex, UI.ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled()); //this could keep happening, so show exception dialog
            return;
        }

        String parentPath = UI.getParentPath(path);
        boolean existsAsDir = new File(parentPath).isDirectory();

        if (!existsAsDir) {
            UI.doError("Directory does not exist", "Cannot create directory as path: " + parentPath + " does not exist");
        } else {
            if (directory) {
                createLocalDirectory(path);
            } else {
                createLocalNormalFile(path);
            }

            boolean parentPathMatchesPanelsPath = currentDirectory.equals(parentPath);
            if (!absolute && parentPathMatchesPanelsPath) {
                filePanel.refresh(); // only need to refresh if the path was relative (as the directory would be created in the current folder) or if absolute and the prent path doesnt match current path. The path identified by the absolute will be refreshed when its navigated to
            } else if (parentPathMatchesPanelsPath) {
                filePanel.refresh();
            }
        }
    }

    /**
     * The handler for creating a new directory
     */
    @Override
    void createNewDirectory() {
        String path = UI.doPathDialog(UI.PathAction.CREATE, true);

        if (path != null)
            createLocalFile(path, true);
    }

    /**
     * The handler to create a new empty file
     */
    @Override
    void createNewFile() {
        String path = UI.doPathDialog(UI.PathAction.CREATE, false);

        if (path != null)
            createLocalFile(path, false);
    }

    /**
     * Takes the given path and attempts to go the the location in the local file system identified by it.
     * @param path the path to go to
     */
    private void goToLocalPath(String path) throws FileSystemException {
        try {
            ResolvedPath resolvedPath = UI.resolveLocalPath(path, filePanel.getCurrentWorkingDirectory());
            path = resolvedPath.getResolvedPath();

            LocalFile file = new LocalFile(new File(path).getAbsoluteFile().getPath());

            if (file.exists() && file.isDirectory()) {
                filePanel.setDirectory(file);
                filePanel.refresh();
            } else if (file.exists()) {
                LineEntry lineEntry = LineEntry.newInstance(file, filePanel);
                if (lineEntry != null)
                    filePanel.openLineEntry(lineEntry);
            } else {
                UI.doError("Path does not exist", "The path: " + path + " does not exist");
            }
        } catch (IOException ex) {
            UI.doException(ex, UI.ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled()); // if it fails here, it'll keep failing, show exception
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
                goToLocalPath(path);
            } catch (FileSystemException ex) {
                UI.doException(ex, UI.ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled());
            }
        }
    }
}
