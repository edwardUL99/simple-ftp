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
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ui.UI;
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

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is a "Container" that contains a FilePanel and a toolbar of options for controlling that FilePanel.
 * To hook a FilePanelContainer and a FilePanel together, you need to call FilePanel.setParentContainer().
 * While everything here could be implemented in a single FilePanel class, this helps provide some abstraction.
 * Keeps the responsibility of FilePanel to just that, listing the files, with options to refresh it or go up to the next level of files.
 * For other options there needs to be a class which will contain the panel and provide some options of controlling it
 *
 * A FilePanelContainer is abstract as the functionality differs depending on the file type. It can only be constructed with FilePanelContainer which returns the appropriate
 * instance for the given file type.
 * It can only be implemented inside the containers package as it does not make sense implementing a FilePanelContainer outside of it
 */
@Log4j2
public abstract class FilePanelContainer extends VBox {
    /**
     * The FilePanel this FilePanelContainer is connected tp
     */
    @Getter
    protected FilePanel filePanel;
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
    FilePanelContainer(FilePanel filePanel) {
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
        symLinkDestButton = new Button();
        symLinkDestButton.setMnemonicParsing(true);
        symLinkDestButton.setText("Go to _Target");
        symLinkDestButton.setTooltip(new Tooltip("The selected file is a symbolic link. Click to go directly to the target directory"));
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
        menuItem1.setMnemonicParsing(true);
        menuItem1.setText("_File");
        ImageView imageView1 = new ImageView(new Image("file_icon.png"));
        imageView1.setFitWidth(20);
        imageView1.setFitHeight(20);
        menuItem1.setGraphic(imageView1);
        menuItem1.setOnAction(e -> createNewFile());

        MenuItem menuItem2 = new MenuItem();
        menuItem2.setMnemonicParsing(true);
        menuItem2.setText("_Symbolic Link");
        ImageView imageView2 = new ImageView(new Image("sym_link.png"));
        imageView2.setFitWidth(20);
        imageView2.setFitHeight(20);
        menuItem2.setGraphic(imageView2);
        menuItem2.setOnAction(e -> createSymbolicLink());

        createButton.getItems().addAll(menuItem, menuItem1, menuItem2);
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
               } else if (keyCode == KeyCode.F5) {
                   filePanel.refresh();
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
                filePanel.openSymbolicLink(lineEntry.getFile());
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
            symLinkDestButton.setVisible(file.isSymbolicLink());
        } else {
            symLinkDestButton.setVisible(false);
        }
    }

    /**
     * The handler for creating a new directory
     */
    abstract void createNewDirectory();

    /**
     * The handler to create a new empty file
     */
    abstract void createNewFile();

    /**
     * The handler for the goto button
     */
    abstract void gotoPath();

    /**
     * Defines how a symbolic link should be created
     */
    abstract void createSymbolicLink();

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
            CommonFile file = lineEntry.getFile();
            String fileName = file.getName();

            if (!UI.isFileOpened(file.getFilePath())) {
                if (UI.doConfirmation("Confirm file deletion", "Confirm deletion of " + fileName)) {
                    if (filePanel.deleteEntry(lineEntry)) {
                        UI.doInfo("File deleted successfully", "File " + fileName + " deleted");
                        comboBox.getItems().remove(fileName);
                        comboBox.setValue("");
                        entryMappings.remove(fileName);
                    } else {
                        UI.doError("File not deleted", "File " + fileName + " wasn't deleted. FTP Reply: " + filePanel.getFileSystem().getFTPConnection().getReplyString());
                    }
                }
            } else {
                UI.doError("File Open", "File " + fileName + " is currently opened, file can't be deleted");
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

    /**
     * Removes the line entry from the combo-box (doesn't delete the actual file)
     * @param lineEntry the line entry to remove
     */
    public void removeLineEntry(final LineEntry lineEntry) {
        String name = lineEntry.getFile().getName();

        if (entryMappings.containsKey(name)) {
            entryMappings.remove(name);
            comboBox.getItems().remove(name);
            comboBox.setValue("");
        }
    }

    /**
     * Creates a new FilePanelContainer instance based on the file panel provided
     * @param filePanel the file panel to be contained by this
     * @return the appropriate FilePanelContainerInstance
     * @throws NullPointerException if panel is null
     */
    public static FilePanelContainer newInstance(FilePanel filePanel) {
        if (filePanel == null)
            throw new NullPointerException("The provided FilePanel is null");

        boolean local = filePanel.getDirectory() instanceof LocalFile; // Because of the FilePanel.checkFileType method, it guarantees that the type will always match the type of panel

        if (local)
            return new LocalFilePanelContainer(filePanel);
        else
            return new RemoteFilePanelContainer(filePanel);
    }
}
