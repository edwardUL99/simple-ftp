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

import com.simpleftp.filesystem.FileSystemUtils;
import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.files.LineEntry;
import com.simpleftp.ui.panels.FilePanel;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

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
     * @return true if it succeeds, false if not
     */
    private boolean createLocalDirectory(String path) {
        LocalFile file = new LocalFile(path);
        if (file.mkdir()) {
            UI.doInfo("Directory Created", "The directory: " + path + " has been created successfully");
            return true;
        } else {
            UI.doError("Directory Not Created", "Failed to make directory with path: " + path);
            return false;
        }
    }

    /**
     * Creates a local file
     * @param path the specified path
     * @return true if it succeeds, false if not
     */
    private boolean createLocalNormalFile(String path) {
        try {
            LocalFile file = new LocalFile(path);
            if (file.createNewFile()) {
                UI.doInfo("File Created", "The file: " + path + " has been created successfully");
                return true;
            } else {
                UI.doError("File Not Created", "Failed to make file with path: " + path);
            }
        } catch (IOException ex) {
            UI.doException(ex, UI.ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled());
        }

        return false;
    }

    /**
     * Handler for creating a local directory
     * @param resolvedPath the resolved path for the file
     * @param directory true if to create a directory, false if file
     * @return true if it succeeds, false if not
     */
    private boolean createLocalFile(String resolvedPath, boolean directory) {
        String currentDirectory = filePanel.getCurrentWorkingDirectory();

        String parentPath = UI.getParentPath(resolvedPath);
        boolean existsAsDir = new LocalFile(parentPath).isDirectory();

        if (!existsAsDir) {
            UI.doError("Directory does not exist", "Cannot create directory as path: " + parentPath + " does not exist");
            return false;
        } else {
            boolean succeeds;
            if (directory) {
                succeeds = createLocalDirectory(resolvedPath);
            } else {
                succeeds = createLocalNormalFile(resolvedPath);
            }

            if (!succeeds)
                return false;

            boolean parentPathMatchesPanelsPath = currentDirectory.equals(parentPath);
            if (parentPathMatchesPanelsPath) {
                filePanel.refresh(); // only need to refresh if the path the file is created in matches the cwd
            }

            return true;
        }
    }

    /**
     * The handler for creating a new directory
     */
    @Override
    void createNewDirectory() {
        String path = UI.doCreateDialog(true, null);

        try {
            if (path != null) {
                String resolvedPath = UI.resolveLocalPath(path, filePanel.getCurrentWorkingDirectory());
                createLocalFile(resolvedPath, true);
            }
        } catch (IOException ex) {
            UI.doException(ex, UI.ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled()); //this could keep happening, so show exception dialog
        }
    }

    /**
     * The handler to create a new empty file
     */
    @Override
    void createNewFile() {
        AtomicBoolean openCreatedFile = new AtomicBoolean(false);
        String path = UI.doCreateDialog(true, () -> openCreatedFile.set(true));
        try {
            if (path != null) {
                String resolvedPath = UI.resolveLocalPath(path, filePanel.getCurrentWorkingDirectory());
                if (createLocalFile(resolvedPath, false) && openCreatedFile.get())
                    filePanel.openLineEntry(LineEntry.newInstance(new LocalFile(path), filePanel));
            }
        } catch (IOException ex) {
            UI.doError("Create file error", "Failed to resolve path " + path + " when creating file");
        }
    }

    /**
     * Takes the given path and attempts to go the the location in the local file system identified by it.
     * @param path the path to go to
     */
    private void goToLocalPath(String path) throws FileSystemException {
        try {
            boolean canonicalize = true;
            String currWorkingDir = filePanel.getCurrentWorkingDirectory();
            LocalFile file = new LocalFile(path);
            boolean absolute = file.isAbsolute();
            path = !absolute ? FileSystemUtils.addPwdToPath(currWorkingDir, path, UI.PATH_SEPARATOR):path;
            file = !absolute ? new LocalFile(path):file;

            if (file.isSymbolicLink()) {
                boolean goToPath = UI.doSymbolicPathDialog(path);

                if (goToPath) {
                    canonicalize = false;
                    String windowsRoot;
                    String root = ((windowsRoot = System.getenv("SystemDrive")) != null) ? windowsRoot:null;
                    path = UI.resolveSymbolicPath(path, UI.PATH_SEPARATOR, root);
                    if (path == null)
                        return; // this is a rare case. UI.resolveSymbolicPath would have shown an error dialog
                }
            }

            if (canonicalize)
                path = UI.resolveLocalPath(path, currWorkingDir);
            file = new LocalFile(path);

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
        String path = UI.doPathDialog();

        if (path != null) {
            try {
                goToLocalPath(path);
            } catch (FileSystemException ex) {
                UI.doException(ex, UI.ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled());
            }
        }
    }
}
