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

package com.simpleftp.ui.panels;

import com.simpleftp.filesystem.FileUtils;
import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.local.exceptions.LocalPathNotFoundException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.directories.DirectoryPane;
import com.simpleftp.ui.files.LineEntry;
import javafx.util.Pair;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class represents a DirectoryPane storing a DirectoryPane for remote files
 */
final class LocalFilePanel extends FilePanel {
    /**
     * Constructs a LocalFilePanel with the provided directoryPane
     * @param directoryPane the directory pane for this file panel to hold
     */
    LocalFilePanel(DirectoryPane directoryPane) {
        super(directoryPane);
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
        String currentDirectory = directoryPane.getCurrentWorkingDirectory();

        String parentPath = FileUtils.getParentPath(resolvedPath, true);
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
                directoryPane.refresh(); // only need to refresh if the path the file is created in matches the cwd
            }

            return true;
        }
    }

    /**
     * The handler for creating a new directory
     */
    @Override
    void createNewDirectory() {
        AtomicBoolean openCreatedDirectory = new AtomicBoolean(false);
        String path = UI.doCreateDialog(true, () -> openCreatedDirectory.set(true));

        try {
            if (path != null) {
                String resolvedPath = UI.resolveLocalPath(path, directoryPane.getCurrentWorkingDirectory());
                if (createLocalFile(resolvedPath, true) && openCreatedDirectory.get())
                    directoryPane.openLineEntry(LineEntry.newInstance(new LocalFile(resolvedPath), directoryPane));
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
                String resolvedPath = UI.resolveLocalPath(path, directoryPane.getCurrentWorkingDirectory());
                if (createLocalFile(resolvedPath, false) && openCreatedFile.get())
                    directoryPane.openLineEntry(LineEntry.newInstance(new LocalFile(resolvedPath), directoryPane));
            }
        } catch (IOException ex) {
            UI.doError("Create file error", "Failed to resolve path " + path + " when creating file");
        }
    }

    /**
     * Resolves the target path, if it is a symbolic link, it keeps the symbolic link without going to target
     * @param targetPath the path to resolve
     * @return the resolved path
     */
    private String resolveTargetPath(String targetPath) throws LocalPathNotFoundException, FileSystemException, IOException {
        LocalFile file = new LocalFile(targetPath);
        targetPath = !file.isAbsolute() ? FileUtils.addPwdToPath(directoryPane.getCurrentWorkingDirectory(), targetPath, FileUtils.PATH_SEPARATOR):targetPath;
        String windowsParent;
        String root = ((windowsParent = System.getenv("SystemDrive")) != null) ? windowsParent:null;
        targetPath = UI.resolveSymbolicPath(targetPath, FileUtils.PATH_SEPARATOR, root);
        file = new LocalFile(targetPath);

        if (!file.isSymbolicLink())
            targetPath = UI.resolveLocalPath(targetPath, directoryPane.getCurrentWorkingDirectory());

        FileSystem fileSystem = directoryPane.getFileSystem();

        if (!fileSystem.fileExists(targetPath))
            throw new LocalPathNotFoundException("The target path " + targetPath + " does not exist", targetPath);

        return targetPath;
    }

    /**
     * Checks if the symbolic link by the given name already exists
     * @param symbolicName the name to check
     * @return the resolved string if exists, null if not
     */
    private String resolveSymbolicName(String symbolicName) throws FileSystemException {
        LocalFile file = new LocalFile(symbolicName);
        String path = !file.isAbsolute() ? FileUtils.addPwdToPath(directoryPane.getCurrentWorkingDirectory(), symbolicName, FileUtils.PATH_SEPARATOR):symbolicName;
        String windowsParent;
        String root = ((windowsParent = System.getenv("SystemDrive")) != null) ? windowsParent:null;
        path = UI.resolveSymbolicPath(path, FileUtils.PATH_SEPARATOR, root);

        return !directoryPane.getFileSystem().fileExists(path) ? path:null;
    }

    /**
     * Defines how a symbolic link should be created
     */
    @Override
    void createSymbolicLink() {
        Pair<String, String> paths = UI.doCreateSymLinkDialog();

        if (paths != null) {
            String targetPath = paths.getKey();
            String namePath = paths.getValue();

            if (targetPath.isEmpty()) {
                UI.doError("Empty Target Path", "The target path cannot be empty");
                return;
            }

            if (namePath.isEmpty()) {
                UI.doError("Empty Symbolic Name", "The name of the symbolic link cannot be empty");
                return;
            }

            try {
                targetPath = resolveTargetPath(targetPath);
                String resolvedNamePath = resolveSymbolicName(namePath);

                if (resolvedNamePath != null) {
                    Path link = Paths.get(resolvedNamePath);
                    Path target = Paths.get(targetPath);

                    Path result = Files.createSymbolicLink(link, target);

                    if (Files.exists(result)) {
                        UI.doInfo("Symbolic Link Created", "The Symbolic link " + namePath + " has been successfully created to point to " + targetPath);

                        if (FileUtils.getParentPath(resolvedNamePath, true).equals(directoryPane.getCurrentWorkingDirectory()))
                            directoryPane.refresh();
                    } else {
                        UI.doError("Symbolic Link Not Created", "The Symbolic link was not created successfully");
                    }
                } else {
                    UI.doError("File Exists", "A file with the path " + namePath + " already exists");
                }
            } catch (LocalPathNotFoundException | FileSystemException ex) {
                UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
            } catch (UnsupportedOperationException ex) {
                UI.doError("Symbolic Links Not Supported", "The creation of symbolic links is not supported on this system");
            } catch (AccessDeniedException ex) {
                if (FTPSystem.isDebugEnabled())
                    ex.printStackTrace();
                UI.doError("Access Denied", "You do not have access to create the symbolic link " + namePath);
            } catch (IOException ex) {
                UI.doException(ex, UI.ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled());
            }
        }
    }

    /**
     * Takes the given path and attempts to go the the location in the local file system identified by it.
     * @param path the path to go to
     */
    private void goToLocalPath(String path) throws FileSystemException {
        try {
            String currWorkingDir = directoryPane.getCurrentWorkingDirectory();
            LocalFile file = new LocalFile(path);
            boolean absolute = file.isAbsolute();
            path = !absolute ? FileUtils.addPwdToPath(currWorkingDir, path, FileUtils.PATH_SEPARATOR) : path;

            String windowsRoot;
            String root = ((windowsRoot = System.getenv("SystemDrive")) != null) ? windowsRoot : null;
            String symbolicPath = UI.resolveSymbolicPath(path, FileUtils.PATH_SEPARATOR, root);
            if (symbolicPath == null)
                return; // this is a rare case. UI.resolveSymbolicPath would have shown an error dialog

            file = new LocalFile(symbolicPath);

            if (!file.exists())
                throw new LocalPathNotFoundException("The path " + symbolicPath + " does not exist", symbolicPath);

            boolean canonicalize = file.isADirectory() && (!file.isSymbolicLink() || !UI.doSymbolicPathDialog(symbolicPath));

            if (canonicalize) {
                path = UI.resolveLocalPath(symbolicPath, currWorkingDir);
                file = new LocalFile(path);
            }

            if (file.isDirectory()) {
                directoryPane.setDirectory(file);
                directoryPane.refresh();
            } else if (file.exists()) {
                LineEntry lineEntry = LineEntry.newInstance(file, directoryPane);
                if (lineEntry != null)
                    directoryPane.openLineEntry(lineEntry);
            } else {
                UI.doError("Path does not exist", "The path: " + path + " does not exist");
            }
        } catch (LocalPathNotFoundException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
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
