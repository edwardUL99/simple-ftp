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
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.panels.FileLineEntry;
import com.simpleftp.ui.panels.FilePanel;
import com.simpleftp.ui.panels.LineEntry;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is a "Container" that contains a FilePanel and a toolbar of options for controlling that FilePanel.
 * To hook a FilePanelContainer and a FilePanel together, you need to call FilePanel.setParentContainer().
 * While everything here could be implemented in a single FilePanel class, this helps provide some abstraction.
 * Keeps the responsibility of FilePanel to just that, listing the files, with options to refresh it or go up to the next level of files.
 * For other options there needs to be a class which will contain the panel and provide some options of controlling it
 */
@Log4j2
public class FilePanelContainer extends VBox {
    /**
     * The FilePanel this FilePanelContainer is connected tp
     */
    @Getter
    private FilePanel filePanel;
    /**
     * The HBox with combo box and buttons
     */
    private final FlowPane toolBar;
    /**
     * The button for deleting chosen files
     */
    private final Button delete;
    /**
     * The button for opening chosen files
     */
    private final Button open;
    /**
     * Button for opening the dialog box to change directory
     */
    private final Button gotoButton;
    /**
     * Button for showing/hiding hidden files
     */
    private final Button hideHiddenFiles;
    /**
     * The Button to display options to create different objects
     */
    private final MenuButton createButton;
    /**
     * The button used to bring up the mask button
     */
    private final Button maskButton;
    /**
     * The combobox displaying the base names of files displayed in the FilePanel
     */
    private final ComboBox<String> comboBox;
    /**
     * Mapping of file basenames to their respective line entries
     */
    private final HashMap<String, LineEntry> entryMappings;

    /**
     * Constructs a FilePanelContainer with the specified filePanel
     * @param filePanel the filePanel this container holds
     */
    public FilePanelContainer(FilePanel filePanel) {
        setStyle(UI.WHITE_BACKGROUND);
        toolBar = new FlowPane();
        toolBar.setHgap(5);
        toolBar.setVgap(5);
        toolBar.setPadding(new Insets(2, 0, 2, 0));
        delete = new Button();
        delete.setMnemonicParsing(true);
        delete.setText("_Delete");
        delete.setOnAction(e -> delete());
        open = new Button();
        open.setMnemonicParsing(true);
        open.setText("_Open");
        open.setOnAction(e -> open());
        comboBox = new ComboBox<>();
        comboBox.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                open();
            }
        });
        comboBox.setPrefWidth(UI.PANEL_CONTAINER_COMBO_WIDTH);
        gotoButton = new Button();
        gotoButton.setMnemonicParsing(true);
        gotoButton.setText("_Go To");
        gotoButton.setOnAction(e -> gotoPath());
        hideHiddenFiles = new Button();
        hideHiddenFiles.setMnemonicParsing(true);
        entryMappings = new HashMap<>();
        createButton = new MenuButton();
        createButton.setMnemonicParsing(true);
        maskButton = new Button();
        maskButton.setMnemonicParsing(true);
        maskButton.setText("File _Mask");
        toolBar.getChildren().addAll(new Label("Files: "), comboBox, delete, open, gotoButton, hideHiddenFiles, createButton, maskButton);
        getChildren().add(toolBar);
        setFilePanel(filePanel);
        initHideButton();
        initCreateButton();
        setKeyBindings();
        initMaskButton();
    }

    /**
     * Initialises the button to show/hide hidden files
     */
    private void initHideButton() {
        boolean showHiddenFiles = filePanel.hiddenFilesShown();

        if (showHiddenFiles) {
            hideHiddenFiles.setText(UI.HIDE_FILES);
        } else {
            hideHiddenFiles.setText(UI.SHOW_FILES);
        }

        hideHiddenFiles.setOnAction(e -> {
            boolean hiddenFilesShown = filePanel.hiddenFilesShown();
            if (hiddenFilesShown) {
                filePanel.hideHiddenFiles();
                hideHiddenFiles.setText(UI.SHOW_FILES);
            } else {
                filePanel.showHiddenFiles();
                hideHiddenFiles.setText(UI.HIDE_FILES);
            }

            filePanel.refresh();
        });
    }

    /**
     * Initialises the button to create new objects
     */
    private void initCreateButton() {
        createButton.setText("_New");
        MenuItem menuItem = new MenuItem();
        menuItem.setMnemonicParsing(true);
        menuItem.setText("Di_rectory");
        ImageView imageView = new ImageView(new Image("dir_icon.png"));
        imageView.setFitHeight(20);
        imageView.setFitWidth(20);
        menuItem.setGraphic(imageView);
        menuItem.setOnAction(e -> createNewDirectory());
        createButton.getItems().add(menuItem);
    }

    /**
     * Initialises the file mask button
     */
    private void initMaskButton() {
        maskButton.setOnAction(e -> {
            String currentMask = filePanel.getFileMask();
            String fileMask = UI.doInputDialog("File Mask", "Enter a mask to filter files: ", currentMask, "Enter wildcards (*) to check if filename contains, i.e *ile*.txt or no wildcards for exact match");
            filePanel.setFileMask(fileMask);
            filePanel.refresh(); // refresh to put the mask in effect
        });
    }

    /**
     * Sets key shortcut bindings which works on the selection in the combo box
     * D - Delete
     * R - Refresh
     * U - Up
     * G - Go To
     * H - Hide hidden files
     * S - Show hidden files
     * Q - Quit
     */
    private void setKeyBindings() {
        setOnKeyPressed(e -> {
            if (!e.isAltDown()) {
                // if alt is down, a mnenomic is needed
                KeyCode keyCode = e.getCode();

                if (keyCode == KeyCode.D) {
                    delete();
                } else if (keyCode == KeyCode.O) {
                    open();
                } else if (keyCode == KeyCode.R) {
                    filePanel.refresh();
                } else if (keyCode == KeyCode.U) {
                    filePanel.up();
                } else if (keyCode == KeyCode.G) {
                    gotoPath();
                } else if (keyCode == KeyCode.H && filePanel.hiddenFilesShown()) {
                    hideHiddenFiles.fire();
                } else if (keyCode == KeyCode.S && !filePanel.hiddenFilesShown()) {
                    hideHiddenFiles.fire();
                } else if (keyCode == KeyCode.Q) {
                    UI.doQuit();
                }
            }
        });
    }

    /**
     * Handler for creating a local directory
     * @param path the path for the directory
     */
    private void createLocalDirectory(String path) {
        boolean absolute = false;
        try {
            Object[] resolvedPath = pathToAbsolute(path, true);
            path = (String)resolvedPath[0];
            absolute = (boolean)resolvedPath[1];
        } catch (FTPException ex) {
            // shouldn't happen but log in case
            log.warn("Unexpected exception occurred", ex);
            absolute = new File(path).isAbsolute();
        }

        String parentPath = new File(path).getParent();

        boolean existsAsDir = true;
        if (parentPath != null) {
            existsAsDir = new File(parentPath).isDirectory();
        }

        if (!existsAsDir) {
            UI.doError("Directory does not exist", "Cannot create directory as path: " + parentPath + " does not exist");
        } else {
            LocalFile file = new LocalFile(path);
            if (file.mkdir()) {
                UI.doInfo("Directory Created", "The directory: " + path + " has been created successfully");

                boolean parentPathMatchesPanelsPath = filePanel.getDirectory().getFilePath().equals(parentPath);
                if (!absolute && parentPathMatchesPanelsPath) {
                    filePanel.refresh(); // only need to refresh if the path was relative (as the directory would be created in the current folder) or if absolute and the prent path doesnt match current path. The path identified by the absolute will be refreshed when its navigated to
                } else if (parentPathMatchesPanelsPath) {
                    filePanel.refresh();
                }
            } else {
                UI.doError("Directory Not Created", "Failed to make directory with path: " + path);
            }
        }
    }

    /**
     * Handler for creating a remote directory
     * @param path the path for the directory
     */
    private void createRemoteDirectory(String path) {
        try {
            Object[] resolvedPath = pathToAbsolute(path, true);
            path = (String)resolvedPath[0];
            boolean absolute = (boolean)resolvedPath[1];

            FTPConnection connection = filePanel.getFileSystem().getFTPConnection();
            String parentPath = new File(path).getParent(); // even though this isn't a local file we can still use getParent to get the parent i.e. the path without the last /....
            boolean existsAsDir = true;
            if (parentPath != null) {
                existsAsDir = connection.remotePathExists(parentPath, true);
            }

            if (!existsAsDir) {
                UI.doError("Directory does not exist", "Cannot create directory as path: " + parentPath + " does not exist");
            } else {
                if (connection.makeDirectory(path)) {
                    UI.doInfo("Directory Created", "The directory: " + path + " has been created successfully");

                    boolean parentPathMatchesPanelsPath = filePanel.getDirectory().getFilePath().equals(parentPath);
                    if (!absolute && parentPathMatchesPanelsPath) {
                        filePanel.refresh(); // only need to refresh if the path was relative (as the directory would be created in the current folder) or if absolute and the prent path doesnt match current path. The path identified by the absolute will be refreshed when its navigated to
                    } else if (parentPathMatchesPanelsPath) {
                        filePanel.refresh();
                    }
                } else {
                    String reply = connection.getReplyString();
                    if (reply.trim().startsWith("2")) {
                        reply = "Path is either a file or already a directory"; // this was a successful reply, so that call must have been checking remotePathExists in FTPConnection.makeDirectory
                    }
                    UI.doError("Directory Not Created", "Failed to make directory with path: " + path + " with error: " + reply);
                }
            }

        } catch (FTPException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, true);
        }
    }

    /**
     * The handler for creating a new directory
     */
    private void createNewDirectory() {
        String path = UI.doPathDialog(UI.PathAction.CREATE);

        if (path != null) {
            if (filePanel.getDirectory() instanceof LocalFile) {
                createLocalDirectory(path);
            } else {
                createRemoteDirectory(path);
            }
        }
    }

    /**
     * Converts a path to an absolute one. If already absolute, it is returned as is
     * @param path the path to make absolute
     * @param local true if a local path, false if remote
     * @return an array where position 0 = path, position 1 = boolean (true if absolute, false if relative)
     * @throws FTPException if local is false and a FTP exception occurs
     */
    private Object[] pathToAbsolute(String path, boolean local) throws FTPException {
        Object[] retArr = new Object[2];
        if (local) {
            LocalFile file = new LocalFile(path);
            if (!file.isAbsolute()) {
                String pwd = filePanel.getDirectory().getFilePath();
                String fileSeparator = System.getProperty("file.separator");
                if (!pwd.equals(fileSeparator)) {
                    path = pwd + fileSeparator + path;
                } else {
                    path = pwd + path;
                }
                retArr[1] = false;
            } else {
                retArr[1] = true;
            }
        } else {
            if (!path.startsWith("/")) {
                FTPConnection connection = filePanel.getFileSystem().getFTPConnection();
                // a relative path
                String pwd = connection.getWorkingDirectory();
                if (!pwd.equals("/")) {
                    path = pwd + "/" + path;
                } else {
                    path = pwd + path;
                }

                retArr[1] = false;
            } else {
                retArr[1] = true;
            }
        }

        retArr[0] = path;

        return retArr;
    }

    /**
     * Takes the given path and attempts to go the the location in the local file system identified by it.
     * @param path the path to go to
     */
    private void goToLocalPath(String path) throws FileSystemException, FTPException {
        path = (String)pathToAbsolute(path, true)[0];

        LocalFile file = new LocalFile(new File(path).getAbsoluteFile().getPath());

        if (file.exists() && file.isDirectory()) {
            filePanel.setDirectory(file);
            filePanel.refresh();
        } else if (file.exists()) {
            filePanel.openLineEntry(new FileLineEntry(file, filePanel));
        } else {
            UI.doError("Path does not exist", "The path: " + path + " does not exist");
        }
    }

    /**
     * Takes the given path and attempts to go the the location in the remote file system identified by it
     * @param path the path to go to
     */
    private void goToRemotePath(String path) throws FileSystemException {
        FileSystem fileSystem = filePanel.getFileSystem();
        FTPConnection connection = fileSystem.getFTPConnection();

        try {
            path = (String)pathToAbsolute(path, false)[0];

            if (connection.remotePathExists(path, true)) {
                CommonFile file = fileSystem.getFile(path);
                filePanel.setDirectory(file);
                filePanel.refresh();
            } else if (connection.remotePathExists(path, false)) {
                filePanel.openLineEntry(new FileLineEntry(new RemoteFile(path), filePanel));
            } else {
                UI.doError("Path does not exist", "The path: " + path + " does not exist or it is not a directory");
            }
        } catch (FTPException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, true);
        }
    }

    /**
     * The handler for the goto button
     */
    private void gotoPath() {
        String path = UI.doPathDialog(UI.PathAction.GOTO);

        if (path != null) {
            try {
                if (filePanel.getDirectory() instanceof LocalFile) {
                    goToLocalPath(path);
                } else {
                    goToRemotePath(path);
                }

            } catch (FileSystemException | FTPException ex) {
                UI.doException(ex, UI.ExceptionType.EXCEPTION, true);
            }
        }
    }

    /**
     * Refreshes the file names displayed in the ComboBox
     */
    private void refreshComboBox() {
        entryMappings.clear();
        comboBox.getItems().clear();
        ArrayList<LineEntry> filesDisplayed = filePanel.filesDisplayed();
        filesDisplayed.forEach(e -> {
            String fileName = e.getFile().getName();
            comboBox.getItems().add(fileName);
            entryMappings.put(fileName, e);
        });

        comboBox.getItems().add(0, "");
    }

    /**
     * Gets the specified LineEntry matching the value chosen in the combo box
     * @return LineEntry matching file name chosen, can be null
     */
    private LineEntry getLineEntryFromComboBox() {
        String file = comboBox.getValue();
        if (file != null && !file.equals("")) {
            return entryMappings.get(file);
        } else {
            return null;
        }
    }

    /**
     * Deletes the chosen line entry from the combobox
     */
    public void delete() {
        LineEntry lineEntry = getLineEntryFromComboBox();

        if (lineEntry != null) {
            if (UI.doConfirmation("Confirm file deletion", "Confirm deletion of " + lineEntry.getFile().getName())) {
                if (filePanel.deleteEntry(lineEntry)) {
                    UI.doInfo("File deleted successfully", "File " + lineEntry.getFile().getName() + " deleted");
                    comboBox.getItems().remove(lineEntry.getFile().getName());
                } else {
                    UI.doError("File not deleted", "File " + lineEntry.getFile().getName() + " wasn't deleted. FTP Reply: " + filePanel.getFileSystem().getFTPConnection().getReplyString());
                }
            }
        }
    }

    /**
     * Opens the chosen file from the combo box
     */
    private void open() {
        LineEntry lineEntry = getLineEntryFromComboBox();

        if (lineEntry != null) {
            filePanel.openLineEntry(lineEntry);
        }
    }

    /**
     * Sets the file panel for this container.
     * Automatically links the file panel by calling FilePanel.setParentContainer
     * @param filePanel the file panel to set
     */
    public void setFilePanel(FilePanel filePanel) {
        if (this.filePanel != null)
            getChildren().remove(this.filePanel);

        this.filePanel = filePanel;
        if (this.filePanel.getParentContainer() != this) // prevent infinite recursion
            this.filePanel.setParentContainer(this);

        getChildren().add(filePanel);
        refresh();
    }

    /**
     * Refresh this FilePanelContainer
     */
    public void refresh() {
        refreshComboBox();
    }

    /**
     * Sets the combobox to represent this line entry if the name of it is inside the combo box in the first place
     * @param lineEntry the line entry to display
     */
    public void setComboBoxSelection(final LineEntry lineEntry) {
        String name = lineEntry.getFile().getName();

        if (comboBox.getItems().contains(name)) {
            comboBox.setValue(name);
        }
    }
}
