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
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.background.FileService;
import com.simpleftp.ui.directories.DirectoryPane;
import com.simpleftp.ui.files.LineEntry;
import com.simpleftp.ui.views.PanelView;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is a Panel that contains a DirectoryPane and a toolbar of options for controlling that DirectoryPane.
 * To hook a DirectoryPane and a DirectoryPane together, you need to call DirectoryPane.setFilePanel().
 * While everything here could be implemented in a single DirectoryPane class, this helps provide some abstraction.
 * Keeps the responsibility of DirectoryPane to just that, listing the files, with options to refresh it or go up to the next level of files.
 * For other options there needs to be a class which will contain the panel and provide some options of controlling it
 *
 * A DirectoryPane is abstract as the functionality differs depending on the file type. It can only be constructed with DirectoryPane which returns the appropriate
 * instance for the given file type.
 * It can only be implemented inside the panels package as it does not make sense implementing a DirectoryPane outside of it
 */
@Log4j2
public abstract class FilePanel extends VBox {
    /**
     * The DirectoryPane this FilePanel is connected to
     */
    @Getter
    protected DirectoryPane directoryPane;
    /**
     * The parent panel view
     */
    @Getter
    protected PanelView panelView;
    /**
     * The HBox with combo box and buttons
     */
    protected final FlowPane toolBar;
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
     * The button to go to the root directory
     */
    private Button rootButton;
    /**
     * The button for opening a properties window
     */
    protected Button propertiesButton;
    /**
     * A button to take you to symbolic link destination. Should be the last button on the toolbar
     */
    private Button symLinkDestButton;
    /**
     * The combobox displaying the base names of files displayed in the DirectoryPane
     */
    private ComboBox<String> comboBox;
    /**
     * Mapping of file basenames to their respective line entries
     */
    private final HashMap<String, LineEntry> entryMappings;
    /**
     * A boolean flag to keep track of the directory pane having multiple selections or not
     */
    private boolean multipleSelected;

    /**
     * Constructs a DirectoryPane with the specified directoryPane
     * @param directoryPane the directoryPane this panel holds
     */
    FilePanel(DirectoryPane directoryPane) {
        entryMappings = new HashMap<>();
        setStyle(UI.WHITE_BACKGROUND);
        setPadding(new Insets(UI.UNIVERSAL_PADDING));
        setSpacing(UI.UNIVERSAL_PADDING);

        initComboBox(); // combo box needs to be initialised before file panel is set
        toolBar = new FlowPane();
        initToolbar();
        getChildren().add(toolBar);
        setDirectoryPane(directoryPane);
        initButtons();

        toolBar.getChildren().addAll(new Label("Files: "), comboBox, open, delete, gotoButton, hideHiddenFiles, createButton, maskButton, rootButton, propertiesButton, symLinkDestButton);
        setKeyBindings();
    }

    /**
     * Returns the children of this editor window. Makes this method final so that we are not calling overridable method
     * @return the list of children
     */
    @Override
    public final ObservableList<Node> getChildren() {
        return super.getChildren();
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
        comboBox.setPrefWidth(UI.PANEL_COMBO_WIDTH);
        comboBox.setOnAction(e -> checkComboBoxValue());
        comboBox.setTooltip(new Tooltip("Currently Selected File"));
    }

    /**
     * Initialises the tool bar which contains combobox and buttons
     */
    private void initToolbar() {
        toolBar.setStyle(UI.PANEL_COLOUR);
        toolBar.setHgap(5);
        toolBar.setVgap(5);
        toolBar.setPadding(new Insets(2, 0, 2, 0));
    }

    /**
     * Initialises all the buttons for the toolbar
     */
    private void initButtons() {
        open = new Button("Open");
        open.setOnAction(e -> open());
        open.setTooltip(new Tooltip("Opens the selected file (changes directory or opens text file)"));

        delete = new Button("Delete");
        delete.setOnAction(e -> delete());
        delete.setTooltip(new Tooltip("Deletes the selected file"));

        gotoButton = new Button("Go To");
        gotoButton.setOnAction(e -> gotoPath());
        gotoButton.setTooltip(new Tooltip("Specify a path to open"));

        hideHiddenFiles = new Button();
        initHideButton();
        hideHiddenFiles.setTooltip(new Tooltip("Toggle the flag to display/hide hidden files"));

        createButton = new MenuButton("New");
        initCreateButton();
        createButton.setTooltip(new Tooltip("Choose a file type to create"));

        maskButton = new Button("File Mask");
        initMaskButton();
        maskButton.setTooltip(new Tooltip("Specify a mask to filter the files displayed"));

        rootButton = new Button(FileUtils.getRootPath(directoryPane.isLocal()));
        rootButton.setTooltip(new Tooltip("Go to the root directory"));
        rootButton.setOnAction(e -> goToRootDirectory());

        propertiesButton = new Button("Properties");
        propertiesButton.setOnAction(e -> openPropertiesWindow());
        propertiesButton.managedProperty().bind(propertiesButton.visibleProperty()); // if hidden, re-arrange toolbar. But for best effects, keep the button as the second last button so ToolBar doesn't keep chopping and changing
        propertiesButton.setVisible(false);
        propertiesButton.setTooltip(new Tooltip("Display the properties window for the selected file"));

        initSymLinkButton();
        symLinkDestButton.setTooltip(new Tooltip("Open symbolic link target (if a file, opens the parent directory of the target file)"));
    }

    /**
     * Initialises the symbolic link button
     */
    private void initSymLinkButton() {
        symLinkDestButton = new Button("Go to Target");
        symLinkDestButton.setTooltip(new Tooltip("The selected file is a symbolic link. Click to go directly to the target directory"));
        symLinkDestButton.setOnAction(e -> goToSymLinkTarget());
        symLinkDestButton.managedProperty().bind(symLinkDestButton.visibleProperty()); // if hidden, re-arrange toolbar. But for best effects, keep the button as the last button so ToolBar doesn't keep chopping and changing
        symLinkDestButton.setVisible(false);
    }

    /**
     * Initialises the button to show/hide hidden files
     */
    private void initHideButton() {
        boolean showHiddenFiles = directoryPane.hiddenFilesShown();

        if (showHiddenFiles) {
            hideHiddenFiles.setText(UI.HIDE_FILES);
        } else {
            hideHiddenFiles.setText(UI.SHOW_FILES);
        }

        hideHiddenFiles.setOnAction(e -> {
            boolean hiddenFilesShown = directoryPane.hiddenFilesShown();
            if (hiddenFilesShown) {
                directoryPane.hideHiddenFiles();
                hideHiddenFiles.setText(UI.SHOW_FILES);
            } else {
                directoryPane.showHiddenFiles();
                hideHiddenFiles.setText(UI.HIDE_FILES);
            }

            directoryPane.refreshCurrentDirectory();
        });
    }

    /**
     * Initialises the button to create new objects
     */
    private void initCreateButton() {
        MenuItem menuItem = new MenuItem("Directory");
        ImageView imageView = new ImageView(new Image("dir_icon.png"));
        imageView.setFitHeight(20);
        imageView.setFitWidth(20);
        menuItem.setGraphic(imageView);
        menuItem.setOnAction(e -> createNewDirectory());

        MenuItem menuItem1 = new MenuItem("File");
        ImageView imageView1 = new ImageView(new Image("file_icon.png"));
        imageView1.setFitWidth(20);
        imageView1.setFitHeight(20);
        menuItem1.setGraphic(imageView1);
        menuItem1.setOnAction(e -> createNewFile());

        MenuItem menuItem2 = new MenuItem("Symbolic Link");
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
            String currentMask = directoryPane.getFileMask();
            String fileMask = UI.doInputDialog("File Mask", "Enter a mask to filter files: ", currentMask, "Enter wildcards (*, ?) to check if filename contains, i.e *ile*.txt or no wildcards for exact match");
            directoryPane.setFileMask(fileMask);
            directoryPane.refreshCurrentDirectory(); // refresh to put the mask in effect
        });
    }

    /**
     * Sets key shortcut bindings
     * Q - Quit
     */
    private void setKeyBindings() {
        setOnKeyPressed(e -> {
            if (!e.isAltDown()) {
                // if alt is down, a mnemonic is needed
                KeyCode keyCode = e.getCode();

                if (keyCode == KeyCode.UP || keyCode == KeyCode.DOWN) {
                    comboBox.requestFocus();
                } else if (keyCode == KeyCode.DELETE) {
                    delete();
                } else if (keyCode == KeyCode.F5) {
                    directoryPane.refresh();
                }
            }
        });
    }

    /**
     * Goes to the root directory
     */
    public void goToRootDirectory() {
        try {
            directoryPane.goToRoot();
        } catch (FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
        }
    }

    /**
     * Opens the property window for the selected file in the ComboBox
     */
    private void openPropertiesWindow() {
        LineEntry lineEntry = getLineEntryFromComboBox();

        if (lineEntry != null) {
            directoryPane.openPropertiesWindow(lineEntry);
        }
    }

    /**
     * Goes to the target of a symbolic link
     */
    public void goToSymLinkTarget() {
        LineEntry lineEntry = getLineEntryFromComboBox(); // the line entry in question
        if (lineEntry != null) {
            try {
                directoryPane.openSymbolicLink(lineEntry.getFile());
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
            if (!multipleSelected) {
                if (!lineEntry.isSelected())
                    lineEntry.setSelected(true);
                CommonFile file = lineEntry.getFile();
                symLinkDestButton.setVisible(file.isSymbolicLink());
                propertiesButton.setVisible(true);
            }
        } else {
            symLinkDestButton.setVisible(false);
            propertiesButton.setVisible(false);
            if (!multipleSelected)
                LineEntry.unselectLastEntry(directoryPane);
        }
    }

    /**
     * Displays a dialog for the creation of a new directory
     */
    public abstract void createNewDirectory();

    /**
     * Displays a dialog for the creation of an empty file
     */
    public abstract void createNewFile();

    /**
     * This method displays a dialog to specify a path the user can go to for a directory
     * or open for a file
     */
    public abstract void gotoPath();

    /**
     * Defines how a symbolic link should be created displaying the appropriate dialog
     */
    public abstract void createSymbolicLink();

    /**
     * Refreshes the file names displayed in the ComboBox
     */
    private void refreshComboBox() {
        entryMappings.clear();
        comboBox.getItems().clear();
        ArrayList<LineEntry> filesDisplayed = directoryPane.filesDisplayed().getLineEntries();
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
     * Gets the selected entry from the combobox
     * @return selected LineEntry, may be null
     */
    public LineEntry getSelectedEntry() {
        return getLineEntryFromComboBox();
    }

    /**
     * Cleans this file name from the panel, i.e. from combo box and mappings etc
     * @param fileName the file name to remove
     */
    private void removeLineEntryFromPanel(String fileName) {
        comboBox.getItems().remove(fileName);
        comboBox.setValue("");
        entryMappings.remove(fileName);
    }

    /**
     * Handles the deletion of multiple files
     */
    private void doMultipleDelete() {
        List<LineEntry> selectedEntries = directoryPane.getSelectedEntries().getLineEntries();
        Stream<LineEntry> stream = selectedEntries
                .stream()
                .filter(Objects::nonNull);
        long count = stream.count();
        if (UI.doConfirmation("Confirm files deletion", "Confirm deletion of " + count + " files")) {
            doMultipleDelete(selectedEntries.stream()
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()));
            selectedEntries.clear();
            directoryPane.clearMultiSelection();
        }
    }

    /**
     * Deletes the chosen line entry from the combobox
     */
    public void delete() {
        if (multipleSelected) {
            doMultipleDelete();
        } else {
            LineEntry lineEntry = getLineEntryFromComboBox();

            if (lineEntry != null) {
                doDelete(lineEntry);
            }
        }
    }

    /**
     * Deletes the provided line entry
     * @param lineEntry the line entry to delete
     */
    private void doDelete(final LineEntry lineEntry) {
        CommonFile file = lineEntry.getFile();
        String fileName = file.getName();

        boolean opened = UI.isFileOpened(file.getFilePath(), directoryPane.isLocal());
        boolean locked = UI.isFileLockedByFileService(file);
        if (!opened && !locked) {
            if (UI.doConfirmation("Confirm file deletion", "Confirm deletion of " + fileName)) {
                try {
                    if (file.isNormalFile() || file.isSymbolicLink()) {
                        if (directoryPane.deleteEntry(lineEntry)) {
                            UI.doInfo("File deleted successfully", "File " + fileName + " deleted");
                            removeLineEntryFromPanel(fileName);
                        } else {
                            UI.doError("File not deleted", "File " + fileName + " wasn't deleted successfully");
                        }
                    } else {
                        doRecursiveDeletion(lineEntry); // recursive may take a while so we want a FileService to do it in the background
                    }
                } catch (FileSystemException ex) {
                    UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
                }
            }
        } else if (opened) {
            UI.doError("File Open", "File " + fileName + " is currently opened, file can't be deleted");
        } else {
            UI.doError("File Locked", "File " + fileName + " is currently locked by a background task, file can't be deleted");
        }
    }

    /**
     * Deletes the list of line entries
     * @param entries the entries to delete
     */
    abstract void doMultipleDelete(List<LineEntry> entries);

    /**
     * Handles when a file is deleted successfully
     * @param lineEntry the line entry that was deleted
     * @param displaySuccess true to display a success message, false if not
     */
    protected void fileDeleteSucceeded(LineEntry lineEntry, boolean displaySuccess) {
        CommonFile file = lineEntry.getFile();
        String fileName = file.getName();
        if (displaySuccess)
            UI.doInfo("File Deleted", "File " + fileName + " has been deleted");
        directoryPane.deleteEntry(lineEntry, false);
        removeLineEntryFromPanel(fileName);
    }

    /**
     * Handles when a file fails to be deleted
     * @param file the file that failed to be deleted
     */
    protected void fileDeleteFailed(CommonFile file) {
        UI.doError("File not deleted", "File " + file.getName() + " wasn't deleted successfully");
    }

    /**
     * This method carries out recursive file deletion
     * @param lineEntry the line entry to delete recursively
     */
    private void doRecursiveDeletion(final LineEntry lineEntry) {
        CommonFile file = lineEntry.getFile();
        if (directoryPane.isLocal()) {
            boolean error = false;
            try {
                if (directoryPane.getFileSystem().removeFile(file)) {
                    fileDeleteSucceeded(lineEntry, true);
                } else {
                    error = true;
                }
            } catch (FileSystemException ex) {
                if (FTPSystem.isDebugEnabled())
                    ex.printStackTrace();

                error = true;
            }

            if (error)
                fileDeleteFailed(file);
        } else {
            FileService.newInstance(file, null, FileService.Operation.REMOVE, false)
                    .setOnOperationSucceeded(() -> fileDeleteSucceeded(lineEntry, true))
                    .setOnOperationFailed(() -> fileDeleteFailed(file))
                    .schedule(); // schedule it to run
        }
    }

    /**
     * Opens the chosen file from the combo box
     */
    private void open() {
        LineEntry lineEntry = getLineEntryFromComboBox();

        if (lineEntry != null) {
            directoryPane.openLineEntry(lineEntry);
        }
    }

    /**
     * Sets the directory pane for this panel.
     * Automatically links the file panel by calling DirectoryPane.setFilePanel
     * @param directoryPane the file panel to set
     */
    public final void setDirectoryPane(DirectoryPane directoryPane) {
        if (this.directoryPane != null)
            getChildren().remove(this.directoryPane);

        this.directoryPane = directoryPane;
        if (this.directoryPane.getFilePanel() != this) // prevent infinite recursion
            this.directoryPane.setFilePanel(this);

        getChildren().add(directoryPane);
        refresh();
    }

    /**
     * Refresh this FilePanel only, not the underlying DirectoryPane. To refresh this and the directory pane at the same time,
     * call getDirectoryPane().refresh()
     */
    public void refresh() {
        refreshComboBox();
    }

    /**
     * Sets the combobox to represent this line entry if the name of it is inside the combo box in the first place
     * @param lineEntry the line entry to display, if null, it basically unsets any currently set file
     */
    public void setComboBoxSelection(final LineEntry lineEntry) {
        String name = lineEntry != null ? lineEntry.getFile().getName():"";

        if (comboBox.getItems().contains(name)) {
            if (!multipleSelected)
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
     * Sets the panel view parent for this FilePanel.
     * This is necessary if you want to propagate exceptions to the PanelView (e.g. a connection failed exception, you may want to do an action in the PanelView
     * @param panelView the panel view to set
     */
    public void setPanelView(PanelView panelView) {
        this.panelView = panelView;
    }

    /**
     * Handles the case of multiple files being selected in the DirectoryPane (or not)
     * @param multipleSelected true if multiple selected or not
     */
    public void onMultipleSelected(boolean multipleSelected) {
        this.multipleSelected = multipleSelected;
        open.setDisable(multipleSelected);

        comboBox.setValue("");
    }


    /**
     * Creates a new FilePanel instance based on the file panel provided
     * @param directoryPane the file panel to be contained by this
     * @return the appropriate FilePanel Instance
     * @throws NullPointerException if panel is null
     */
    public static FilePanel newInstance(DirectoryPane directoryPane) {
        if (directoryPane == null)
            throw new NullPointerException("The provided DirectoryPane is null");

        boolean local = directoryPane.isLocal();

        if (local)
            return new LocalFilePanel(directoryPane);
        else
            return new RemoteFilePanel(directoryPane);
    }
}
