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
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.local.exceptions.LocalPathNotFoundException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.directories.DirectoryPane;
import com.simpleftp.ui.files.LineEntry;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.util.Pair;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class represents a DirectoryPane storing a DirectoryPane for local files
 */
public final class LocalFilePanel extends FilePanel {
    /**
     * This property determines if home is defined for this file panel
     */
    private final BooleanProperty homeDefinedProperty;
    /**
     * The file representing home
     */
    private LocalFile homeFile;

    /**
     * Constructs a LocalFilePanel with the provided directoryPane
     * @param directoryPane the directory pane for this file panel to hold
     */
    LocalFilePanel(DirectoryPane directoryPane) {
        super(directoryPane);
        homeDefinedProperty = new SimpleBooleanProperty(false);
        initButtons();
    }

    /**
     * Initialises any buttons on the LocalFilePanel
     */
    private void initButtons() {
        String homePath = System.getProperty("user.home");
        String errorHeader = "Home button initialisation error";
        if (homePath != null && (homeFile = new LocalFile(homePath)).isADirectory()) {
            // only add the home button if there is a user.home property defined and it exists as a directory

            Button homeButton = new Button("Home");
            homeButton.setTooltip(new Tooltip("Go to user's home directory"));
            final LocalFile finalFile = homeFile;
            homeButton.setOnAction(e -> home(finalFile));
            ObservableList<Node> children = toolBar.getChildren();
            children.add(children.indexOf(propertiesButton), homeButton);

            homeDefinedProperty.setValue(true);
        } else if (homePath == null) { // Platform runLater as this code may be executed before a stage is shown. A dialog can't be displayed without primary scene/stage being set up, so run later for when a scene is prepared
            Platform.runLater(() -> UI.doError(errorHeader, "The system does not have a Java user.home property defined outlining the location of the user's home directory. Not enabling Home button"));
        } else if (homeFile != null && !homeFile.isADirectory()) {
            Platform.runLater(() -> UI.doError(errorHeader, "The system Java property defined user home " + homePath + " is either not a directory or a file. Not enabling Home button"));
        }
    }

    /**
     * Goes to the user.home directory. Handler for the homeButton
     */
    private void home(LocalFile homeFile) {
        try {
            if (!homeFile.equals(directoryPane.getDirectory())) {
                directoryPane.setDirectory(homeFile);
                directoryPane.refresh();
            }
        } catch (FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
        }
    }

    /**
     * Goes to the user's home directory if it is defined
     */
    public void home() {
        if (homeDefinedProperty.get())
            home(homeFile);
    }

    /**
     * This property determines if the home functionality of the local panel has been initialised correctly. If this has a value
     * of false, home() will not work
     * @return the property for if home is enabled
     */
    public BooleanProperty getHomeDefinedProperty() {
        return homeDefinedProperty;
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

            boolean parentPathMatchesPanelsPath = FileUtils.pathEquals(currentDirectory, parentPath, true);
            if (parentPathMatchesPanelsPath) {
                directoryPane.refreshCurrentDirectory(); // only need to refresh if the path the file is created in matches the cwd
            }

            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createNewDirectory() {
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
     * {@inheritDoc}
     */
    @Override
    public void createNewFile() {
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
        String root = FileUtils.getRootPath(true);
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
        String root = FileUtils.getRootPath(true);
        path = UI.resolveSymbolicPath(path, FileUtils.PATH_SEPARATOR, root);

        return !directoryPane.getFileSystem().fileExists(path) ? path:null;
    }

    /**
     * Defines how a symbolic link should be created
     */
    @Override
    public void createSymbolicLink() {
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

                        if (FileUtils.pathEquals(FileUtils.getParentPath(resolvedNamePath, true), directoryPane.getCurrentWorkingDirectory(), true))
                            directoryPane.refreshCurrentDirectory();
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

            String root = FileUtils.getRootPath(true);
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
                directoryPane.refresh(); // a local directory pane doesn't cache so just use normal refresh
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
     * {@inheritDoc}
     */
    @Override
    public void gotoPath() {
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
