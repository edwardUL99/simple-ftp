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

import com.simpleftp.FTPSystem;
import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.paths.ResolvedPath;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.files.FileLineEntry;
import com.simpleftp.ui.panels.FilePanel;
import com.simpleftp.ui.files.LineEntry;
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
import java.io.IOException;
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
    private FlowPane toolBar;
    /**
     * The button for deleting chosen files
     */
    private Button delete;
    /**
     * The button for opening chosen files
     */
    private Button open;
    /**
     * Button for opening the dialog box to change directory
     */
    private Button gotoButton;
    /**
     * Button for showing/hiding hidden files
     */
    private Button hideHiddenFiles;
    /**
     * The Button to display options to create different objects
     */
    private MenuButton createButton;
    /**
     * The button used to bring up the mask button
     */
    private Button maskButton;
    /**
     * A button to take you to symbolic link destination. Should be the last button on the toolbar
     */
    private Button symLinkDestButton;
    /**
     * The combobox displaying the base names of files displayed in the FilePanel
     */
    private ComboBox<String> comboBox;
    /**
     * Mapping of file basenames to their respective line entries
     */
    private final HashMap<String, LineEntry> entryMappings;

    /**
     * Constructs a FilePanelContainer with the specified filePanel
     * @param filePanel the filePanel this container holds
     */
    public FilePanelContainer(FilePanel filePanel) {
        entryMappings = new HashMap<>();
        setStyle(UI.WHITE_BACKGROUND);
        setPadding(new Insets(UI.UNIVERSAL_PADDING));
        setSpacing(UI.UNIVERSAL_PADDING);

        initComboBox(); // combo box needs to be initalised before file panel is set
        toolBar = new FlowPane();
        initToolbar();
        getChildren().add(toolBar);
        setFilePanel(filePanel);
        initButtons();

        toolBar.getChildren().addAll(new Label("Files: "), comboBox, delete, open, gotoButton, hideHiddenFiles, createButton, maskButton, symLinkDestButton);
        setKeyBindings();
    }

    /**
     * Initialises the combo box in the toolbar
     */
    private void initComboBox() {
        comboBox = new ComboBox<>();
        comboBox.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                open();
            }
        });
        comboBox.setPrefWidth(UI.PANEL_CONTAINER_COMBO_WIDTH);
        comboBox.setOnAction(e -> checkComboBoxValue());
        comboBox.setTooltip(new Tooltip("Currently Selected File"));
    }

    /**
     * Initialises the tool bar which contains combobox and buttons
     */
    private void initToolbar() {
        toolBar.setStyle(UI.PANEL_CONTAINER_COLOUR);
        toolBar.setHgap(5);
        toolBar.setVgap(5);
        toolBar.setPadding(new Insets(2, 0, 2, 0));
    }

    /**
     * Initialises all the buttons for the toolbar
     */
    private void initButtons() {
        delete = new Button();
        delete.setMnemonicParsing(true);
        delete.setText("_Delete");
        delete.setOnAction(e -> delete());

        open = new Button();
        open.setMnemonicParsing(true);
        open.setText("_Open");
        open.setOnAction(e -> open());

        gotoButton = new Button();
        gotoButton.setMnemonicParsing(true);
        gotoButton.setText("_Go To");
        gotoButton.setOnAction(e -> gotoPath());

        hideHiddenFiles = new Button();
        hideHiddenFiles.setMnemonicParsing(true);
        initHideButton();

        createButton = new MenuButton();
        createButton.setMnemonicParsing(true);
        initCreateButton();

        maskButton = new Button();
        maskButton.setMnemonicParsing(true);
        maskButton.setText("File _Mask");
        initMaskButton();

        initSymLinkButton();
    }

    /**
     * Initialises the symbolic link button
     */
    private void initSymLinkButton() {
        symLinkDestButton = new Button("Go to Target");
        symLinkDestButton.setTooltip(new Tooltip("The selected directory is a symbolic link. Click to go directly to the directory it points to"));
        symLinkDestButton.setOnAction(e -> goToSymLinkTarget());
        symLinkDestButton.managedProperty().bind(symLinkDestButton.visibleProperty()); // if hidden, re-arrange toolbar. But for best effects, keep the button as the last button so ToolBar doesn't keep chopping and changing
        symLinkDestButton.setVisible(false);
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

        MenuItem menuItem1 = new MenuItem();
        menuItem.setMnemonicParsing(true);
        menuItem1.setText("_File");
        ImageView imageView1 = new ImageView(new Image("file_icon.png"));
        imageView1.setFitWidth(20);
        imageView1.setFitHeight(20);
        menuItem1.setGraphic(imageView1);
        menuItem1.setOnAction(e -> createNewFile());
        createButton.getItems().addAll(menuItem, menuItem1);
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
     * Sets key shortcut bindings
     * Q - Quit
     */
    private void setKeyBindings() {
        setOnKeyPressed(e -> {
            if (!e.isAltDown()) {
                // if alt is down, a mnenomic is needed
                KeyCode keyCode = e.getCode();

               if (keyCode == KeyCode.Q) {
                    UI.doQuit();
               } else if (keyCode == KeyCode.UP || keyCode == KeyCode.DOWN) {
                   comboBox.requestFocus();
               } else if (keyCode == KeyCode.DELETE) {
                   delete();
               }
            }
        });
    }

    /**
     * Goes to the target of a symbolic link
     */
    private void goToSymLinkTarget() {
        LineEntry lineEntry = getLineEntryFromComboBox(); // the line entry in question
        if (lineEntry != null) {
            try {
                filePanel.setDirectorySymbolicLink(lineEntry.getFile());
            } catch (Exception ex) {
                UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
            }
        }
    }

    /**
     * This method checks the current value of the combo box for a certain property. At the moment, it just checks for symbolic link directory
     */
    private void checkComboBoxValue() {
        LineEntry lineEntry = getLineEntryFromComboBox(); // the line entry in question
        if (lineEntry != null) {
            CommonFile file = lineEntry.getFile();

            try {
                if (UI.isFileSymbolicLink(file) && file.isADirectory()) {
                    symLinkDestButton.setVisible(true);
                } else {
                    symLinkDestButton.setVisible(false);
                }
            } catch (FileSystemException ex) {
                UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
            }
        } else {
            symLinkDestButton.setVisible(false);
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
            LocalFile file = new LocalFile(path);
            if (directory) {
                if (file.mkdir()) {
                    UI.doInfo("Directory Created", "The directory: " + path + " has been created successfully");
                } else {
                    UI.doError("Directory Not Created", "Failed to make directory with path: " + path);
                }
            } else {
                try {
                    if (file.createNewFile()) {
                        UI.doInfo("File Created", "The file: " + path + " has been created successfully");
                    } else {
                        UI.doError("File Not Created", "Failed to make file with path: " + path);
                    }
                } catch (IOException ex) {
                    UI.doException(ex, UI.ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled());
                }
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
                    if (connection.makeDirectory(path)) {
                        UI.doInfo("Directory Created", "The directory: " + path + " has been created successfully");
                    } else {
                        String reply = connection.getReplyString();
                        if (reply.trim().startsWith("2")) {
                            reply = "Path is either a file or already a directory"; // this was a successful reply, so that call must have been checking remotePathExists in FTPConnection.makeDirectory
                        }
                        UI.doError("Directory Not Created", "Failed to make directory with path: " + path + " with error: " + reply);
                    }
                } else {
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
    private void createNewDirectory() {
        String path = UI.doPathDialog(UI.PathAction.CREATE, true);

        if (path != null) {
            if (filePanel.getDirectory() instanceof LocalFile) {
                createLocalFile(path, true);
            } else {
                createRemoteFile(path, true);
            }
        }
    }

    /**
     * The handler to create a new empty file
     */
    private void createNewFile() {
        String path = UI.doPathDialog(UI.PathAction.CREATE, false);

        if (path != null) {
            if (filePanel.getDirectory() instanceof LocalFile) {
                createLocalFile(path, false);
            } else {
                createRemoteFile(path, false);
            }
        }
    }

    /**
     * Takes the given path and attempts to go the the location in the local file system identified by it.
     * @param path the path to go to
     */
    private void goToLocalPath(String path) throws FileSystemException, FTPException {
        try {
            ResolvedPath resolvedPath = UI.resolveLocalPath(path, filePanel.getCurrentWorkingDirectory());
            path = resolvedPath.getResolvedPath();

            LocalFile file = new LocalFile(new File(path).getAbsoluteFile().getPath());

            if (file.exists() && file.isDirectory()) {
                filePanel.setDirectory(file);
                filePanel.refresh();
            } else if (file.exists()) {
                filePanel.openLineEntry(new FileLineEntry(file, filePanel));
            } else {
                UI.doError("Path does not exist", "The path: " + path + " does not exist");
            }
        } catch (IOException ex) {
            UI.doException(ex, UI.ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled()); // if it fails here, it'll keep failing, show exception
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
            ResolvedPath resolvedPath = UI.resolveRemotePath(path, filePanel.getCurrentWorkingDirectory(), true, fileSystem.getFTPConnection());
            path = resolvedPath.getResolvedPath();

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
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
        }
    }

    /**
     * The handler for the goto button
     */
    private void gotoPath() {
        String path = UI.doPathDialog(UI.PathAction.GOTO, true); // directory is irrelevant here for GOTO, but pass to compile

        if (path != null) {
            try {
                if (filePanel.getDirectory() instanceof LocalFile) {
                    goToLocalPath(path);
                } else {
                    goToRemotePath(path);
                }

            } catch (FileSystemException | FTPException ex) {
                UI.doException(ex, UI.ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled());
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
            checkComboBoxValue();
        }
    }
}
