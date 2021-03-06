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
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.properties.Properties;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.background.BundledServices;
import com.simpleftp.ui.background.FileService;
import com.simpleftp.ui.directories.tasks.FileStringDownloader;
import com.simpleftp.ui.files.FilePropertyWindow;
import com.simpleftp.ui.files.LineEntries;
import com.simpleftp.ui.files.LineEntry;
import com.simpleftp.ui.panels.FilePanel;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represents a panel of Files. These files may be in different locations, hence why the class is abstract.
 * Some methods need to be defined differently depending on the type of file required. i.e. at the moment, LocalFiles have different behaviour than a RemoteFile.
 *
 * This class can only be constructed outside this package by using DirectoryPane.newInstance(CommonFile) which returns the appropriate DirectoryPane implementation for that file type.
 * You can only implement this class inside the panels package as it does not make sense to be implementing FilePanels outside of the panels package
 */
public abstract class DirectoryPane extends VBox {
    /**
     * The status panel which contains all the buttons and current directory label
     */
    private StatusPanel statusPanel;
    /**
     * A tool tip for displaying the current directory when mouse is hovered over currentDirectory in case it is abbreviated
     */
    private Tooltip currentDirectoryTooltip;
    /**
     * Button for refreshing the DirectoryPane and its entries
     */
    private Button refresh;
    /**
     * Button for moving up to the parent directory
     */
    @Getter
    private UpButton upButton;
    /**
     * The list of line entries inside in entries box
     */
    private LineEntries lineEntries;
    /**
     * The bundle for copying and pasting
     */
    protected static BundledServices copyPasteBundle;
    /**
     * The LineEntry (or entries) that has been copied
     */
    protected static final ArrayList<LineEntry> copiedEntries = new ArrayList<>();
    /**
     * The directory that this DirectoryPane is currently listing
     */
    @Getter
    protected CommonFile directory;
    /**
     * The DirectoryPane that is holding this DirectoryPane
     */
    @Getter
    protected FilePanel filePanel;
    /**
     * The ScrollPane that will provide scrolling functionality for the entriesBox
     */
    protected ScrollPane entriesScrollPane;
    /**
     * The entries box which will hold all the LineEntries
     */
    @Getter
    private EntriesBox entriesBox;
    /**
     * The file system this DirectoryPane is connected to. Expected to be initialised by sub-classes
     */
    @Getter
    protected FileSystem fileSystem;
    /**
     * The max length for file path before it is shortened and ... added to the end of it
     */
    private static int MAX_FILE_PATH_LENGTH = 30;
    /**
     * Flag for showing hidden files
     * By default, it is false
     */
    protected boolean showHiddenFiles;
    /**
     * A pane to display an empty folder if there are no files
     */
    private EmptyDirectoryPane emptyFolderPane;
    /**
     * A mask used to match files against. If null, it has no effect
     */
    @Getter
    private String fileMask;
    /**
     * The regex fileMask is converted to
     */
    private String fileMaskRegex;
    /**
     * The directory representing root
     */
    protected CommonFile rootDirectory;
    /**
     * An selection of all the selected line entries on this pane;
     */
    @Getter
    private final LineEntries selectedEntries;
    /**
     * If we are doing multiple operations, we bundle them
     */
    protected BundledServices bundle;
    /**
     * This list keeps track of DirectoryPane instances for use with drag and drop etc
     */
    private static final ArrayList<DirectoryPane> instances = new ArrayList<>();

    /**
     * Constructs a DirectoryPane object.
     * Sub-classes are expected to instantiate the filesystem object as the instance varies depending on if this is local or remote and to call the initDirectory(directory) method.
     */
    DirectoryPane() {
        setStyle(UI.WHITE_BACKGROUND);
        lineEntries = new LineEntries();
        selectedEntries = new LineEntries();

        initButtons();
        initStatusPanel();
        initEntriesBox();
        initEmptyFolderPane();
        initKeyBinding();
        setOnMouseClicked(this::unselectFile);
        instances.add(this);
    }

    /**
     * Get the list of files that will be copied
     * @return list of files to copy
     */
    private List<LineEntry> getCopySelectedFiles() {
        if (selectedEntries.size() > 0) {
            return new ArrayList<>(selectedEntries.getLineEntries());
        } else if (filePanel != null) {
            LineEntry selectedEntry = filePanel.getSelectedEntry();

            if (selectedEntry != null)
                return new ArrayList<>(Collections.singleton(selectedEntry));
        }

        return null;
    }

    /**
     * Handles a copy operation
     */
    private void handleCopy() {
        copiedEntries.clear();
        List<LineEntry> files = getCopySelectedFiles();

        if (files != null) {
            copiedEntries.addAll(files);
        }
    }

    /**
     * Handles the paste operation
     */
    private void handlePaste() {
        LineEntry selected;
        if (filePanel != null && (selected = filePanel.getSelectedEntry()) != null) {
            paste(selected.getFile());
        } else {
            boolean emptyPaneVisible = emptyFolderPane.shown;
            if ((!emptyPaneVisible && entriesBox.canPaste()) || (emptyPaneVisible && emptyFolderPane.canPaste()))
                paste(directory);
        }
    }

    /**
     * Select all line entries
     */
    private void selectAll() {
        lineEntries.getLineEntries().forEach(LineEntry::multiSelect);
    }

    /**
     * Initialises any key binding for this directory pane
     */
    private void initKeyBinding() {
        setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            if (e.isControlDown() && !e.isShiftDown()) {
                if (code == KeyCode.C) {
                    handleCopy();
                } else if (code == KeyCode.V) {
                    handlePaste();
                } else if (code == KeyCode.A) {
                    selectAll();
                }
            }
        });
    }

    /**
     * Lazy Initialises the root directory for this directory pane and returns it
     * @return the root directory
     * @throws FileSystemException if an error occurs in creating the file
     */
    abstract CommonFile getRootDirectory() throws FileSystemException;

    /**
     * Instructs this DirectoryPane to go to the root directory
     * @throws FileSystemException if an exception occurs returning to the root
     */
    public void goToRoot() throws FileSystemException {
        if (!isAtRootDirectory()) {
            setDirectory(getRootDirectory());
            refresh(true, Properties.REMOVE_ALL_LISTING_CACHE_REFRESH.getValue());
        }
    }

    /**
     * Unselects the presently selected file if the DirectoryPane is clicked and the target is not a LineEntry
     * @param mouseEvent the mouse event to query
     */
    private void unselectFile(MouseEvent mouseEvent) {
        LineEntry lineEntry = UI.Events.selectLineEntry(mouseEvent); // attempt to "pick" the LineEntry out from the mouse selection. If null, we didn't select a line entry
        if (lineEntry == null) {
            if (selectedEntries.size() > 0)
                clearMultiSelection();
            filePanel.setComboBoxSelection(null);
        }
    }

    /**
     * Initialises the pane used to display an empty folder
     */
    private void initEmptyFolderPane() {
        emptyFolderPane = new EmptyDirectoryPane();
    }

    /**
     * Initialises the buttons and sets their respective actions
     */
    private void initButtons() {
        refresh = new Button("Refresh");
        refresh.setTooltip(new Tooltip("Refresh the files listing on this panel"));
        refresh.setOnAction(e -> refresh());

        upButton = new UpButton();
        upButton.setTooltip(new Tooltip("Move up to the parent directory\nDrop files here to copy/move to parent"));
        upButton.setAction(e -> up());
    }

    /**
     * Initialises the status panel which contains the buttons and current directory
     */
    private void initStatusPanel() {
        statusPanel = new StatusPanel();
        statusPanel.init();
    }

    /**
     * Initialises the VBox containing file entries
     */
    private void initEntriesBox() {
        entriesBox = new EntriesBox();
        entriesBox.initDragAndDrop();
        entriesScrollPane = new ScrollPane();
        entriesScrollPane.setFitToWidth(true);
        entriesScrollPane.setFitToHeight(true);
        entriesScrollPane.setContent(entriesBox);
        entriesScrollPane.setStyle(UI.WHITE_BACKGROUND);
        entriesScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        entriesScrollPane.setPrefHeight(UI.FILE_PANEL_HEIGHT);
    }

    /**
     * Initialises the directory for this PanelView. This should be called by sub-classes to initialise the directory
     * @param directory the directory to set as initial directory
     */
    protected void initDirectory(CommonFile directory) throws FileSystemException {
        setDirectory(directory);
        refresh();

        statusPanel.setCurrDirText(directory.getFilePath());
    }

    /**
     * Sets the DirectoryPane that is the parent, i.e. contains this DirectoryPane.
     * This action hooks the two together.
     * @param filePanel the panel to add this DirectoryPane to
     */
    public void setFilePanel(FilePanel filePanel) {
        this.filePanel = filePanel;
        if (this.filePanel.getDirectoryPane() != this) // prevent a cycle of infinite recursion
            this.filePanel.setDirectoryPane(this);
    }

    /**
     * Checks if you are at the root
     * @return true if at the root
     */
    public boolean isAtRootDirectory() {
        return directory.getFilePath().equals(FileUtils.getRootPath(isLocal()));
    }

    /**
     * Controls going up to parent directory
     */
    public void up() {
        if (!isAtRootDirectory()) {
            try {
                setDirectory(directory.getExistingParent());
                refresh(true, Properties.REMOVE_ALL_LISTING_CACHE_REFRESH.getValue());
            } catch (FileSystemException ex) {
                UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
            }
        }
    }

    /**
     * Checks the directory passed in to see if the type matches the file type this DirectoryPane is for.
     * setDirectory calls this
     * @param directory the directory to check
     * @throws IllegalArgumentException if the type of directory is different to the type of the current one
     */
    abstract void checkFileType(CommonFile directory) throws IllegalArgumentException;

    /**
     * Returns the path of the current working directory
     * @return the current working directory path
     */
    public String getCurrentWorkingDirectory() {
        return directory.getFilePath();
    }

    /**
     * Checks if the file represents a symbolic link and throws IllegalArgumentException if not
     * @param symbolicLink the file to check for being a link
     * @throws IllegalArgumentException if it is not a symbolic link
     */
    private void checkSymbolicLink(CommonFile symbolicLink) throws IllegalArgumentException {
        if (!symbolicLink.isSymbolicLink())
            throw new IllegalArgumentException("The file provided is not a symbolic link");
    }

    /**
     * This method determines if a DirectoryPane is a local one. All RemotePanes, regardless of the underlying connection is still remote, so they should return false
     * @return true if local, false if remote
     */
    public abstract boolean isLocal();

    /**
     * Changes directory but doesn't check the type of the directory or if it is a directory.
     * This is used internally as it is called by doubleClickDirectoryEntry. Since, you knew it was a directory to create a directory entry, you don't need to check it again.
     * The types will also always stay the same. The public method setDirectory is a wrapper for this, doing validation checks and then calling this.
     * While, internally these checks should be enforced, it can't be guaranteed an external class calling it would have stuck to them.
     * @param directory the directory to set
     */
     void setDirectoryUnchecked(CommonFile directory) {
        boolean local = isLocal();

        if (this.directory != null)
            UI.closeFile(getCurrentWorkingDirectory(), local); // after successfully leaving this directory to the new one, close it
        this.directory = directory;
        String path = directory.getFilePath();
        UI.openFile(path, local); // open the new directory

        statusPanel.setCurrDirText(path);
     }

    /**
     * Changes directory. Refresh should be called after this action
     * @param directory the directory to change to, local, or remote
     * @throws FileSystemException if the directory is not a directory
     * @throws IllegalArgumentException if type of the directory is different to the type that was initially passed in.
     *              You're not allowed pass in RemoteFile to constructor and then suddenly set directory to a LocalFile or it's not a directory
     */
    public void setDirectory(CommonFile directory) throws FileSystemException, IllegalArgumentException {
        checkFileType(directory);

        if (directory.isADirectory()) {
            setDirectoryUnchecked(directory);
        } else {
            throw new IllegalArgumentException("The directory for a DirectoryPane must be in fact a directory, not a file");
        }
    }

    /**
     * This method is for changing to a symbolicLink that is a symbolic link and indicates to follow it to the destination.
     * setDirectory called on symbolic link follows it symbolically, represents it as a folder of the parent
     * @param symbolicLink the symbolic link to change to
     * @throws FileSystemException if an error occurs
     * @throws IllegalArgumentException if the directory is not in fact a directory and is not a symbolic link
     */
    private void setSymbolicLinkTargetDir(CommonFile symbolicLink) throws FileSystemException, IllegalArgumentException {
        checkFileType(symbolicLink);

        String path = symbolicLink.getSymbolicLinkTarget();
        if (symbolicLink.isNormalFile())
        {
            // go to the parent folder of the file
            path = FileUtils.getParentPath(path, isLocal());
        }

        CommonFile targetFile = fileSystem.getFile(path);
        setDirectory(targetFile); // need to use the checked setDirectory as this is a public method
        refresh(true, Properties.REMOVE_ALL_LISTING_CACHE_REFRESH.getValue());
    }

    /**
     * Opens the given symbolic link. If it is a directory, it goes to the target directory. If it is a file, it goes to the parent of the target file
     * @param symbolicLink the symbolic link to open
     * @throws FileSystemException if an error occurs
     * @throws IllegalArgumentException if the file is not a symbolic link
     */
    public void openSymbolicLink(CommonFile symbolicLink) throws FileSystemException, IllegalArgumentException {
        checkSymbolicLink(symbolicLink);
        setSymbolicLinkTargetDir(symbolicLink);
    }

    /**
     * Checks the given filename against the file mask. It is assumed the mask is not null when calling this method
     * @param fileName the name to check
     * @return if the filename matches the mask
     */
    private boolean checkNameAgainstMask(String fileName) {
        return fileName.matches(fileMaskRegex);
    }

    /**
     * Converts any wildcards in the provided glob like linux matching i.e. file*
     * @param glob the glob to convert
     * @return the matching regex
     */
    private String createRegexFromGlob(String glob) {
        StringBuilder out = new StringBuilder();
        for(int i = 0; i < glob.length(); ++i) {
            final char c = glob.charAt(i);
            switch(c) {
                case '*': out.append(".*"); break; // zero or more characters of any value
                case '?': out.append('.'); break; // one and only one character. Can be any character
                case '.': out.append("\\."); break; // if it is a dot, escape the dot as it is a literal
                case '\\': out.append("\\\\"); break; // escape a slash
                default: out.append(c); // just a normal character, add it to the regex
            }
        }

        return out.toString();
    }

    /**
     * Returns whether the local file should be shown
     * @param file the file being queried
     * @param hiddenChecker the predicate to determine if the file is hidden or not. This is required because at the moment there is no platform independent way of determining if it is hidden
     * @return true if the file is to be shown
     */
    protected boolean showFile(CommonFile file, Predicate<CommonFile> hiddenChecker) {
        boolean showFile = hiddenChecker.test(file);

        if (!showFile) // if it is hidden, it will never be shown regardless of matching mask or not
            return false;

        if (fileMask != null) {
            showFile = checkNameAgainstMask(file.getName());
        }

        return showFile;
    }


    /**
     * Checks whether the file exists already, if it is a directory, it throws an error and returns false as you can't rename a file to a directory
     * If it is a file, it shows a confirmation dialog
     * @param file the file to check
     * @return true if the rename should continue, false if not
     */
    boolean overwriteExistingFile(CommonFile file) throws FileSystemException {
        String filePath = file.getFilePath();

        if (file.exists()) {
            if (file.isADirectory()) {
                UI.doError("File is a Directory", "You cannot rename file as " + filePath + " is a directory");
                return false;
            } else {
                if (!file.isSymbolicLink()) {
                    return UI.doConfirmation("Overwrite Existing File", "Renaming the file to " + filePath + " will overwrite an existing file. Confirm this operation", true);
                } else {
                    return UI.doConfirmation("Overwrite Symbolic Link", "Renaming the file to " + filePath + " will overwrite the existing link and may break the symbolic link. Confirm this operation", true);
                }
            }
        }

        return true;
    }

    /**
     * Handles when rename is called on the context menu with the specified line entry
     * @param lineEntry the line entry to rename the file of
     */
    abstract void renameLineEntry(final LineEntry lineEntry);

    /**
     * Creates a line entry, adding a context menu to it and returns it
     * @param file the file the create the line entry from
     * @return the line entry created
     */
    protected LineEntry createLineEntry(CommonFile file) {
        LineEntry lineEntry = LineEntry.newInstance(file, this);

        if (lineEntry == null)
            return null;

        lineEntry.setOnContextMenuRequested(e -> {
            ContextMenu contextMenu = createContextMenu(lineEntry);
            contextMenu.show(lineEntry, e.getScreenX(), e.getScreenY());
        });

        return lineEntry;
    }

    /**
     * Gets the context menu for when multiple files are selected
     * @return the context menu for multiple selected files
     */
    private ContextMenu createMultiContextMenu() {
        if (copiedEntries.size() == 0) {
            List<LineEntry> lineEntries = getCopySelectedFiles();

            boolean copyDisabled = lineEntries == null || lineEntries.size() == 0;

            MenuItem copy = new MenuItem("Copy");
            copy.setOnAction(e -> handleCopy());
            copy.setDisable(copyDisabled);

            ContextMenu contextMenu = new ContextMenu();
            contextMenu.getItems().add(copy);

            return contextMenu;
        }

        return null;
    }

    /**
     * Creates a context menu for the provided LineEntry
     * @param lineEntry the line entry to create a context menu for
     * @return the constructed context menu
     */
    private ContextMenu createContextMenu(final LineEntry lineEntry) {
        List<LineEntry> selected = getCopySelectedFiles();

        if (selected != null) {
            if (selected.size() > 1)
                return createMultiContextMenu();
        }

        CommonFile file = lineEntry.getFile();

        MenuItem open = new MenuItem("Open");
        open.setOnAction(e -> openLineEntry(lineEntry));

        MenuItem rename = new MenuItem("Rename");
        rename.setOnAction(e -> renameLineEntry(lineEntry));

        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(e -> handleCopy());

        int copiedEntriesSize = copiedEntries.size();
        MenuItem paste;
        if (copiedEntriesSize == 1) {
            List<LineEntry> entries = retrieveCopiedEntriesPasteable(lineEntry);

            Label pasteLabel = new Label("Paste");
            paste = new CustomMenuItem(pasteLabel); // custom menu item so we can use tooltips
            boolean disabled = entries.size() == 0;
            paste.setDisable(disabled);
            paste.setOnAction(e -> paste(file));
            if (!disabled) {
                LineEntry copiedEntry = entries.get(0);
                String copiedEntryFilePath = copiedEntry.getFilePath();
                pasteLabel.setTooltip(new Tooltip("Copied File: " + copiedEntryFilePath + " (" + (copiedEntry.isLocal() ? "Local" : "Remote") + ")"));
            }
        } else {
            paste = new MenuItem("Paste");

            if (copiedEntriesSize == 0) {
                paste.setDisable(true);
            } else {
                List<LineEntry> entries = retrieveCopiedEntriesPasteable(lineEntry);
                paste.setDisable(entries.size() == 0);
            }

            paste.setOnAction(e -> paste(file));
        }

        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(e -> filePanel.delete()); // right clicking this would have selected it in the container's combo box. So use containers delete method to display confirmation dialog
        // as a consequence, this method also checks if the file is open or not. If this call is to be changed to this.delete(), add the isOpen check and confirmation dialog to that method
        // this.delete doesn't remove the file from the combo box anyway (although refresh would get that)

        MenuItem properties = new MenuItem("Properties");
        properties.setOnAction(e -> openPropertiesWindow(lineEntry));

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(open, rename, copy, paste, delete, properties);

        if (file.isSymbolicLink()) {
            MenuItem target = new MenuItem("Go to Target");
            target.setOnAction(e -> filePanel.goToSymLinkTarget());
            contextMenu.getItems().add(target);
        }

        return contextMenu;
    }

    /**
     * Return list of selected entries to copy that can be pasted into the provided line entry
     * @param lineEntry the line entry to paste into
     * @return list of entries that can be pasted
     */
    private List<LineEntry> retrieveCopiedEntriesPasteable(final LineEntry lineEntry) {
        return copiedEntries.stream()
                .filter(lineEntry1 -> isFilePasteable(lineEntry1, lineEntry))
                .collect(Collectors.toList());
    }

    /**
     * Returns true if the copiedEntry can be pasted into the targetEntry
     * @param copiedEntry the entry to copy
     * @param lineEntry the entry to paste into
     * @return true if it can be copied, false if not
     */
    private boolean isFilePasteable(final LineEntry copiedEntry, final LineEntry lineEntry) {
        String copiedEntryFilePath = copiedEntry == null ? null:copiedEntry.getFilePath();
        String lineEntryPath = lineEntry.getFilePath();
        boolean copiedLocal;

        return !(copiedEntryFilePath == null || lineEntry.isFile() || copiedEntryFilePath.equals(lineEntryPath)
                || (FileUtils.getParentPath(copiedEntryFilePath, (copiedLocal = copiedEntry.isLocal())).equals(lineEntryPath) && copiedLocal == lineEntry.isLocal()));
    }

    /**
     * Pastes the copied LineEntry into target
     * @param target the target file to paste (copy) into
     */
    private void paste(final CommonFile target) {
        if (copiedEntries.size() > 1)
            copyPasteBundle = new BundledServices(FileService.Operation.COPY);

        for (LineEntry copiedEntry : copiedEntries) {
            if (copiedEntry != null) {
                CommonFile source = copiedEntry.getFile();

                DirectoryPane sourcePane = copiedEntry.getOwningPane();
                if (sourcePane == this) {
                    scheduleCopyMoveService(source, target, true, this);
                } else {
                    sourcePane.scheduleCopyMoveService(source, target, true, this);
                }

                copiedEntry.multiSelect();
            }
        }

        if (copyPasteBundle != null) {
            copyPasteBundle.activate();
            copyPasteBundle = null;
        }

        copiedEntries.clear();
    }

    /**
     * Opens a property window for the specified line entry
     * @param lineEntry the line entry to open the properties window for
     */
    public void openPropertiesWindow(LineEntry lineEntry) {
        try {
            new FilePropertyWindow(lineEntry).show();
        } catch (FileSystemException ex) {
            if (FTPSystem.isDebugEnabled())
                ex.printStackTrace();
            UI.doError("Properties Window Failure", "Failed to open the properties window for file " + lineEntry.getFilePath() + ".");
        }
    }

    /**
     * Constructs the list of line entries to display
     * @param useCache true if to use a cached list if supported/available. Some panes may take any value and not have any affect.
     *                 Not all panes are required to cache line entries for each directory visited. However, remotely it is done if enabled
     * @param removeAllCache if useCache is false, then removeAllCache means that all cached line entries should be removed, if false, just current directory
     * @return the list of constructed line entries
     */
    abstract LineEntries constructListOfFiles(boolean useCache, boolean removeAllCache);

    /**
     * Adds the list of line entries to the entriesBox
     * @param lineEntries the line entries to add
     */
    private void addLineEntriesFromList(LineEntries lineEntries) {
        entriesBox.getChildren().addAll(lineEntries.getLineEntries());
        entriesBox.addBottomSpacing();
    }

    /**
     * Displays the entries box is entries is true or empty folder pane if false as the directory listing
     * @param entries true to display entriesBox, false to display emptyFolderPane
     */
    private void displayDirectoryListing(boolean entries) {
        getChildren().clear();
        getChildren().addAll(statusPanel, entries ? entriesScrollPane:emptyFolderPane);
        emptyFolderPane.show(!entries);
    }

    /**
     * Checks if the current directory still exists and if it doesn't it sets the directory to the next available parent
     */
    private void checkCurrentDirExistence() throws FileSystemException {
        if (!directory.exists()) {
            UI.doError("Directory Not Found", "The current directory no longer exists on the file system, moving to the next parent directory that exists");
            setDirectory(directory.getExistingParent());
        }
    }

    /**
     * Refreshes this directory listing.
     * This method is <b>cache destructive</b>. If configured, calling this method will clear all cached line entries if
     * the directory pane implements caching. If you don't want to clear cache, call refreshCurrentDirectory() or cacheRefresh() instead. That
     * method is most useful after completing some task and you only need it to appear in the current working directory
     */
    public void refresh() {
        try {
            checkCurrentDirExistence();
            refresh(false, Properties.REMOVE_ALL_LISTING_CACHE_REFRESH.getValue());
        } catch (FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
        }
    }

    /**
     * Refreshes using cached list of line entries if supported/available
     * @param useCache true to use cache, false if not
     * @param removeAllCache if useCache is false, then removeAllCache means that all cached line entries should be removed, if false, just current directory
     */
    protected void refresh(boolean useCache, boolean removeAllCache) {
        if (!Properties.CACHE_REMOTE_DIRECTORY_LISTING.getValue())
            useCache = false; // always use false if properties have set cache to false

        LineEntries lineEntries = constructListOfFiles(useCache, removeAllCache);
        lineEntries.sort();

        boolean displayEntries = lineEntries.size() > 0;
        entriesBox.getChildren().clear();
        if (displayEntries)
            addLineEntriesFromList(lineEntries);
        displayDirectoryListing(displayEntries);

        this.lineEntries = lineEntries;

        clearMultiSelection();
        if (filePanel != null) {
            filePanel.refresh();
            filePanel.onMultipleSelected(false);
        }

        entriesScrollPane.setHvalue(0); // resetConnection scroll position
        entriesScrollPane.setVvalue(0);
    }

    /**
     * If cache is implemented by the implementing DirectoryPane, calling this method will refresh the current directory only
     * without clearing any other cached directory listings.
     * Calling just refresh is used to clear all cached entries if configured, so you'll most likely want to use this method
     * unless you are using the refresh button ot F5
     */
    public void refreshCurrentDirectory() {
        refresh(false, false);
    }

    /**
     * Calls refresh but using cached information
     */
    public void cacheRefresh() {
        refresh(true, Properties.REMOVE_ALL_LISTING_CACHE_REFRESH.getValue());
    }

    /**
     * Handles double click of the specified directory entry
     * @param lineEntry the directory entry to double click
     */
    private void doubleClickDirectoryEntry(final LineEntry lineEntry) {
        CommonFile file = lineEntry.getFile();
        setDirectoryUnchecked(file);
        refresh(true, Properties.REMOVE_ALL_LISTING_CACHE_REFRESH.getValue());
    }

    /**
     * Checks the size of the file and if under 100MB returns true, if over, gets confirmation if file size is ok and returns true
     * @param file the file to check size of
     * @return true if under 100MB or if over, the user confirmed it is ok to open this file
     * @throws FileSystemException if an error occurs checking file size if the file is a remote file
     */
    private boolean checkFileSize(CommonFile file) throws FileSystemException {
        long size = file.getSize();

        if (size >= Properties.FILE_EDITOR_SIZE_WARN_LIMIT.getValue())  {
            return UI.doFileSizeWarningDialog(file.getFilePath());
        }

        return true;
    }

    /**
     * Handles double clicks of the specified file entry
     * @param lineEntry the file entry to double click
     */
    private void doubleClickFileEntry(final LineEntry lineEntry) throws FTPException, FileSystemException {
        CommonFile file = lineEntry.getFile();
        if (checkFileSize(file)) {
            FileStringDownloader fileStringDownloader = new FileStringDownloader(lineEntry, fileSystem, this);
            fileStringDownloader.start();
            UI.openFile(lineEntry.getFilePath(), isLocal()); // only open it if an error doesn't occur
        }
    }

    /**
     * Handles the double click of the specified line entry
     * @param lineEntry the line entry to double click
     */
    private void doubleClick(final LineEntry lineEntry) {
        try {
            boolean local = isLocal();

            String filePath = lineEntry.getFilePath();
            if (!UI.isFileOpened(filePath, local)) {
                CommonFile file = lineEntry.getFile();
                file.refresh(); // update the existence information if this is a RemoteFile as the status may have changed after this file was loaded
                            // RemoteFile sorts of "caches" the file retrieved from the remote server to update performance rather than retrieving the info from the server every time. But this info may not be up to date
                           // Calling refresh updates the cache of the file if RemoteFile
                if (file.isNormalFile()) {
                    try {
                        doubleClickFileEntry(lineEntry);
                    } catch (FTPException ex) {
                        UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
                    }
                } else if (file.isADirectory()) {
                    doubleClickDirectoryEntry(lineEntry); // this calls setDirectory which opens the new path
                } else {
                    UI.doError("File not found", "The file " + filePath + " no longer exists", true);
                    removeEntryFromPanel(lineEntry);
                }
            } else {
                UI.doInfo("File Open", "The file " + filePath + " is already opened");
            }
        } catch (FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
        }
    }

    /**
     * "Opens" the specified lineEntry. This is the equivalent to double clicking it
     * If it is a directory, it will change the directory this file panel is in
     * @param lineEntry the line entry to open
     */
    public void openLineEntry(final LineEntry lineEntry) {
        doubleClick(lineEntry);
    }

    /**
     * Removes the line entry from view on the panel and it's parent container. Doesn't physically delete the file
     * @param lineEntry the line entry to remove
     */
    private void removeEntryFromPanel(final LineEntry lineEntry) {
        if (filePanel != null) {
            filePanel.removeLineEntry(lineEntry);
        }
        deleteEntry(lineEntry, false);
    }

    /**
     * Does the remove action for the file. Removal of a file may differ between local and remote file panels, hence why abstract
     * @param commonFile the file to remove
     * @return the result
     */
    abstract boolean doRemove(CommonFile commonFile) throws Exception;

    /**
     * Removes the line entry from the directory's line entries list, and if the size is 0, displays the empty directory pane
     * @param lineEntry the line entry to remove
     */
    private void removeLineEntry(LineEntry lineEntry) {
        entriesBox.getChildren().remove(lineEntry);
        lineEntries.remove(lineEntry);

        if (lineEntries.size() == 0) {
            // display the empty folder pane
            displayDirectoryListing(false);
        }
    }

    /**
     * Attempts to delete the specified line entry and the file associated with it
     * @param lineEntry the line entry to remove
     * @param removeFile remove the file from the filesystem also, leave false if you just want to delete the entry from the directory pane
     * @return true if successful, false if not
     */
    public boolean deleteEntry(final LineEntry lineEntry, boolean removeFile) {
        CommonFile file = lineEntry.getFile();

        try {
            if (!removeFile || doRemove(file)) {
                removeLineEntry(lineEntry);
                copiedEntries.remove(lineEntry);

                return true;
            }
        } catch (Exception ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
        }

        return false;
    }

    /**
     * Attempts to delete the specified line entry and the file associated with it.
     * This removes the associated file by calling deleteEntry(lineEntry, true)
     * @param lineEntry the line entry to remove
     * @return true if successful, false if not
     */
    public boolean deleteEntry(final LineEntry lineEntry) {
        return deleteEntry(lineEntry, true);
    }

    /**
     * Returns the files that this DirectoryPane is displaying
     * @return list of displayed files
     */
    public LineEntries filesDisplayed() {
        return lineEntries;
    }

    /**
     * Gets the value for showing hidden files
     * @return true if hidden files are being shown
     */
    public boolean hiddenFilesShown() {
        return showHiddenFiles;
    }

    /**
     * Enables the file panel to show hidden files on the NEXT refresh.
     * This just sets the flag. Refresh needs to be called to show the effects
     */
    public void showHiddenFiles() {
        showHiddenFiles = true;
    }

    /**
     * Disables the file panel to show hidden files on the NEXT refresh.
     * This just sets the flag. Refresh needs to be called to show the effects
     */
    public void hideHiddenFiles() {
        showHiddenFiles = false;
    }

    /**
     * Sets the mask used for displaying files. refresh should be called after for the mask to be visibly applied
     * @param fileMask the file mask to show
     */
    public void setFileMask(String fileMask) {
        this.fileMask = fileMask;

        if (fileMask == null) {
            fileMaskRegex = null;
            statusPanel.currentDirectoryLabel.setText("Current Directory:");
            MAX_FILE_PATH_LENGTH = 30;
        } else {
            fileMaskRegex = createRegexFromGlob(fileMask);
            statusPanel.currentDirectoryLabel.setText("Current Directory (masked): ");
            MAX_FILE_PATH_LENGTH = 20;
        }

        statusPanel.setCurrDirText(getCurrentWorkingDirectory());
    }

    /**
     * Displays the message that the operation completed successfully
     * @param copy true to copy, false for move
     * @param source the source file
     * @param destination the destination file
     */
    void operationCompletedMessage(boolean copy, CommonFile source, CommonFile destination) {
        String operationHeader = copy ? "Copy":"Move", operationMessage = copy ? "copy":"move";
        String destinationPath = destination.getFilePath();
        UI.doInfo(operationHeader + " Completed", "The " + operationMessage + " of " + source.getFilePath()
                + " to " + destinationPath + " has completed");
    }

    /**
     * Displays the message that the operation failed
     * @param copy true to copy, false for move
     * @param source the source file
     * @param destination the destination file
     */
    void operationFailedMessage(boolean copy, CommonFile source, CommonFile destination) {
        String operationHeader = copy ? "Copy":"Move", operationMessage = copy ? "copy":"move";
        UI.doError(operationHeader + " Failed", "The " + operationMessage + " of " + source.getFilePath()
                + " to " + destination.getFilePath() + " has failed");
    }

    /**
     * This method creates and schedules the file service used for copying/moving the source to destination
     * @param source the source file to be copied/moved
     * @param destination the destination directory to be copied/moved to
     * @param copy true to copy, false to move
     * @param targetPane  the target pane if a different pane. Leave null if not different
     */
    abstract void scheduleCopyMoveService(CommonFile source, CommonFile destination, boolean copy, DirectoryPane targetPane);

    /**
     * Clears all currently selected files if a multiple file selection has been made
     */
    public void clearMultiSelection() {
        selectedEntries.getLineEntries()
                .stream()
                .filter(Objects::nonNull)
                .forEach(lineEntry -> lineEntry.setSelected(false));
        selectedEntries.clear();

        if (filePanel != null)
            filePanel.onMultipleSelected(false);
    }


    /**
     * Validates that the target entry is a directory if not null and that the target isLocal matches this isLocal.
     * If not, throws IllegalArgumentException
     * @param target the target line entry to check. If null, this method is a no-op
     */
    private void validateTarget(LineEntry target) {
        if (target != null) {
            if (!target.isDirectory())
                throw new IllegalArgumentException("The target LineEntry must be a directory");

            boolean targetLocal = target.isLocal();
            boolean isLocal = isLocal();

            if (targetLocal != isLocal) {
                if (isLocal)
                    throw new IllegalArgumentException("The target LineEntry is remote but the DirectoryPane is local, they must both be local");
                else
                    throw new IllegalArgumentException("The target LineEntry is local but the DirectoryPane is remote, they must both be remote");
            }
        }
    }

    /**
     * This method handles a DragEvent that source and target is on the same DirectoryPane.
     * This check is expected to be already done. This method consumes the events and completes the drop
     * @param dragEvent the drag event
     * @param target the target line entry. Should be a directory. If it's null the current directory is taken as the directory
     */
    public void handleDragAndDropOnSamePane(MouseDragEvent dragEvent, LineEntry target) {
        validateTarget(target);
        handleDragAndDrop(dragEvent, this, target, !Properties.DRAG_DROP_SAME_PANEL_OPERATION.getValue().equals("MOVE"));
        /* only 2 possible values for this property, so just check value of one,
            if getValue() returns false it means copy was chosen, so negate it
             to make copy have the value of true to copy.
             If getValue() returns true, the property is MOVE but we need to make it false so we schedule a move
         */
    }

    /**
     * This method handles a DragEvent where the source comes from a different pane than this one.
     * This check is expected to be already done. This method consumes the events and completes the drop
     * @param dragEvent the drag event
     * @param sourcePane the directory pane that's the source of the drag and drop
     * @param target the target line entry. Should be a directory. If it's null the current directory is taken as the directory
     */
    public void handleDragAndDropFromDifferentPane(MouseDragEvent dragEvent, DirectoryPane sourcePane, LineEntry target) {
        validateTarget(target);
        handleDragAndDrop(dragEvent, sourcePane, target, Properties.DRAG_DROP_DIFFERENT_PANEL_OPERATION.getValue().equals("COPY"));
        // only 2 possible values for this property, so just check value of one, if false it means move was chosen
    }

    /**
     * Carry out the drag and drop event after necessary validation and setup is completed
     * @param dragEvent the drag event that started it
     * @param sourcePane the source pane of the drag event
     * @param sourceEntry the source line entry
     * @param destination the destination line entry
     * @param copy true to copy, false to move
     */
    private void doDragAndDrop(MouseDragEvent dragEvent, DirectoryPane sourcePane, LineEntry sourceEntry, CommonFile destination, boolean copy) {
        CommonFile source = sourceEntry.getFile();
        if (dragEvent.isControlDown()) {
            copy = !copy;
        }

        if (!copy && UI.isFileLockedByFileService(source))
            UI.doError("File Locked", "File " + source.getName() + " is currently locked by a background task, file can't be moved");
        else
            sourcePane.scheduleCopyMoveService(source, destination, copy, this); // same panel is a copy operation
    }

    /**
     * If multiple line entries are selected, this method handles the drag and drop of all the selected files
     * @param dragEvent the event that started the drag and drop action
     * @param sourcePane the pane that is the source of the drag and drop event
     * @param destination the destination file
     * @param copy true to copy, false to move
     */
    private void handleMultipleDragAndDrop(MouseDragEvent dragEvent, DirectoryPane sourcePane, CommonFile destination, boolean copy) {
        if (sourcePane.selectedEntries.size() > 1)
            sourcePane.bundle = new BundledServices(copy ? FileService.Operation.COPY: FileService.Operation.MOVE);

        for (LineEntry selected : sourcePane.selectedEntries.getLineEntries()) {
            if (selected != null) {
                doDragAndDrop(dragEvent, sourcePane, selected, destination, copy);
            }
        }

        if (sourcePane.bundle != null) {
            sourcePane.bundle.activate();
            sourcePane.bundle = null;
        }

        sourcePane.clearMultiSelection();
    }

    /**
     * The internal method to handle drag and drop.
     * @param dragEvent the drag event representing the drag and drop
     * @param sourcePane the pane that is the source of the event. Pass in 'this' if this pane is the source
     * @param target the target LineEntry. Assumed to already represents a directory
     * @param copy true to copy or false to move. If Ctrl is pressed this value will be negated
     */
    private void handleDragAndDrop(MouseDragEvent dragEvent, DirectoryPane sourcePane, LineEntry target, boolean copy) {
        CommonFile destination = target != null ? target.getFile():directory;
        LineEntry sourceEntry = UI.Events.selectLineEntry(dragEvent.getGestureSource());

        if (sourceEntry != null) {
            if (sourcePane.selectedEntries.size() > 0 && sourcePane.selectedEntries.contains(sourceEntry)) {
                handleMultipleDragAndDrop(dragEvent, sourcePane, destination, copy);
            } else {
                doDragAndDrop(dragEvent, sourcePane, sourceEntry, destination, copy);
            }
        }

        dragEvent.consume();
    }

    /**
     * If you are not using this DirectoryPane anymore, you should call this method so that Drag and Drop etc. is not
     * propagated to it anymore
     */
    public void removeInstance() {
        instances.remove(this);
    }

    /**
     * Apply the given consumer for each instance of directory pane registered.
     * This method is useful for applying mouse events to each pane etc.
     * @param consumer the consumer to apply to each instance.
     */
    public static void forEachInstance(Consumer<DirectoryPane> consumer) {
        instances.forEach(consumer);
    }

    /**
     * Creates an instance of DirectoryPane based on the given directory
     * @param directory the directory to initialise with
     * @return the constructed file panel
     * @throws NullPointerException if directory is null
     */
    public static DirectoryPane newInstance(CommonFile directory) throws FileSystemException {
        if (directory == null)
            throw new NullPointerException("The CommonFile object passed in is null");

        if (directory.isLocal())
            return new LocalDirectoryPane((LocalFile)directory);
        else
            return new RemoteDirectoryPane((RemoteFile)directory);
    }

    /**
     * This class holds buttons related to current directory and a label with the directory
     */
    public class StatusPanel extends HBox {
        /**
         * The label outlining current directory
         */
        private Label currentDirectoryLabel;
        /**
         * The label showing the path to the current directory
         */
        private Label currentDirectory;

        /**
         * Constructs a status panel
         */
        private StatusPanel() {
            setPadding(new Insets(UI.UNIVERSAL_PADDING));
            setSpacing(10);
            setBorder(new Border(new BorderStroke(Paint.valueOf("BLACK"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
            setStyle(UI.GREY_BACKGROUND);
        }

        /**
         * This method initialises the children of the StatusPanel.
         */
        private void init() {
            getChildren().addAll(refresh, upButton, initCurrentDirectoryLabel(), currentDirectory);
        }

        /**
         * Initialises the CurrentDirectory labels of the DirectoryPane and returns the header label
         * @return header label with value "Current Directory:"
         */
        private Label initCurrentDirectoryLabel() {
            currentDirectoryLabel = new Label("Current Directory:");
            currentDirectoryLabel.setPadding(new Insets(5, 0, 0, 0));
            currentDirectoryLabel.setAlignment(Pos.CENTER_LEFT);
            currentDirectoryLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, currentDirectoryLabel.getFont().getSize()));

            currentDirectory = new Label();
            currentDirectory.setPadding(new Insets(5, 0, 0, 0));
            currentDirectory.setAlignment(Pos.CENTER_LEFT);
            currentDirectory.setFont(Font.font("Monospaced"));

            return currentDirectoryLabel;
        }

        /**
         * Sets the text of the current directory text in the status panel and abbreviates it if it is too long
         * @param currentDirectory the directory to change the text to
         */
        private void setCurrDirText(String currentDirectory) {
            if (currentDirectory.length() >= MAX_FILE_PATH_LENGTH) {
                String currentDirectoryShortened = currentDirectory.substring(0, MAX_FILE_PATH_LENGTH - 3) + "...";
                if (currentDirectoryTooltip == null) {
                    currentDirectoryTooltip = new Tooltip(currentDirectory);
                    Tooltip.install(this.currentDirectory, currentDirectoryTooltip);
                } else {
                    currentDirectoryTooltip.setText(currentDirectory);
                }

                this.currentDirectory.setText(currentDirectoryShortened);
            } else {
                if (currentDirectoryTooltip != null) {
                    Tooltip.uninstall(this.currentDirectory, currentDirectoryTooltip);
                    currentDirectoryTooltip = null;
                }

                this.currentDirectory.setText(currentDirectory);
            }
        }
    }

    /**
     * This class represents a box for the line entries
     */
    public class EntriesBox extends VBox {
        /**
         * Constructs an entries box object
         */
        private EntriesBox() {
            setStyle(UI.WHITE_BACKGROUND);
            setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
            VBox.setVgrow(this, Priority.ALWAYS);
            setMaxHeight(Double.MAX_VALUE);
            setOnContextMenuRequested(e -> {
                LineEntry target = UI.Events.selectLineEntry(e.getTarget());
                if (target == null) { // we didn't request a line entry's context menu, so display the EntriesBox's context menu
                    ContextMenu contextMenu = createContextMenu();
                    contextMenu.show(this, e.getScreenX(), e.getScreenY());
                }
            });
        }

        /**
         * Creates and returns the context menu for this EntriesBox
         */
        private ContextMenu createContextMenu() {
            MenuItem paste = new MenuItem("Paste");
            boolean disabled = !canPaste();
            paste.setDisable(disabled);
            paste.setOnAction(e -> paste(DirectoryPane.this.directory));

            ContextMenu contextMenu = new ContextMenu();
            contextMenu.getItems().add(paste);

            return contextMenu;
        }

        /**
         * Returns true if the copiedEntry can be pasted here
         * @return true if can paste, false if not
         */
        private boolean canPaste() {
            boolean pasteableEntries = false;
            LineEntry target = LineEntry.newInstance(DirectoryPane.this.directory, DirectoryPane.this);
            if (target != null) {
                for (LineEntry copiedEntry : copiedEntries) {
                    pasteableEntries = pasteableEntries || isFilePasteable(copiedEntry, target);
                }

                return pasteableEntries;
            }

            return false;
        }

        /**
         * Adds the bottom spacing to the end of the entries box to allow drag drop to occur if the entries box is full
         */
        public void addBottomSpacing() {
            /*
               There are children.size() (y) line entries of x height.
               We want to set the minimum height of our HBox to (x * y) height with a little offset to
                allow space to drag and drop into the current directory if it is full.
              */
            ObservableList<Node> children = getChildren();
            double entryHeight = ((LineEntry)children.get(0)).getHeight() * children.size();
            setHeight(entryHeight);
            setMinHeight(entryHeight + 50.00);
        }

        /**
         * This method handles the logic for handling a drag event that has been entered on a target on this DirectoryPane
         * @param dragEvent the drag event representing the entry
         */
        private void dragEntered(MouseDragEvent dragEvent) {
            LineEntry sourceEntry = UI.Events.selectLineEntry(dragEvent.getGestureSource());
            LineEntry targetEntry = UI.Events.selectLineEntry(dragEvent.getTarget());

            if (sourceEntry != null && sourceEntry.getOwningPane() != DirectoryPane.this
                    && (targetEntry == null || targetEntry.isFile())) {
                setStyle(UI.Events.DRAG_ENTERED_BACKGROUND);
                UI.Events.setDragCursorEnteredImage();
            }
        }

        /**
         * This method handles the logic for handling a drag event that has exited a target on this DirectoryPane
         * @param dragEvent the drag event representing the exit
         */
        private void dragExited(MouseDragEvent dragEvent) {
            setStyle(UI.WHITE_BACKGROUND);
            if (!Properties.DRAG_DROP_CURSOR_FILE_ICON.getValue())
                UI.Events.setDragCursorImage(null); // leave null as we don't need it her
        }

        /**
         * This method handles the logic for handling a drag event that has been dropped on this DirectoryPane
         * @param dragEvent the drag event representing the drop
         */
        private void dragDropped(MouseDragEvent dragEvent) {
            LineEntry sourceEntry = UI.Events.selectLineEntry(dragEvent.getGestureSource());
            DirectoryPane sourcePane = sourceEntry != null ? sourceEntry.getOwningPane():null;
            if (sourcePane != null && sourcePane != DirectoryPane.this) {
                LineEntry targetEntry = UI.Events.selectLineEntry(dragEvent.getTarget());

                if (targetEntry == null || (targetEntry.isFile() && targetEntry.getOwningPane() == DirectoryPane.this))
                    DirectoryPane.this.handleDragAndDropFromDifferentPane(dragEvent, sourcePane,
                            LineEntry.newInstance(directory, DirectoryPane.this));
            }

            dragEvent.consume();
        }

        /**
         * Initialises the logic for drag and drop
         */
        private void initDragAndDrop() {
            setOnMouseDragEntered(this::dragEntered);
            setOnMouseDragExited(this::dragExited);
            setOnMouseDragReleased(this::dragDropped);
        }

        /**
         * Retrieves the directory pane that contains this EntriesBox
         * @return directory pane containing this EntriesBox
         */
        public DirectoryPane getDirectoryPane() {
            return DirectoryPane.this;
        }
    }

    /**
     * This class represents the Up button so that mouse events can be registered on it
     */
    public class UpButton extends Button {
        /**
         * The original style before any edits
         */
        private final String originalStyle = getStyle();
        /**
         * The original event handler before the button was disabled
         */
        private EventHandler<ActionEvent> originalHandler;

        /**
         * Constructs an Up Button
         */
        private UpButton() {
            setText("Up");
            initDragAndDrop();
        }

        /**
         * If true, displays this up button as a drag drop target.
         * While the button is a target it becomes unusable so when drag is detected this should be called with true, but when the drag
         * is finished, call this with false to make it usable again
         * @param display true to display as target, false to not
         */
        public final void displayDragDropTarget(boolean display) {
            if (display) {
                setStyle(UI.Events.DRAG_ENTERED_BACKGROUND);
                setOnAction(null);
            } else {
                setStyle(originalStyle);
                setOnAction(originalHandler);
            }
        }

        /**
         * Retrieves the DirectoryPane this up button is a member of
         * @return directory pane containing this up button
         */
        public DirectoryPane getDirectoryPane() {
            return DirectoryPane.this;
        }

        /**
         * This method should be called instead of setOnAction or else a dragAndDrop will effect the action of the button
         * @param actionHandler the action handler to set
         */
        public void setAction(EventHandler<ActionEvent> actionHandler) {
            originalHandler = actionHandler;
            setOnAction(actionHandler);
        }

        /**
         * Handles mouse entry event to the up button
         * @param mouseDragEvent the mouse event representing the entry
         */
        private void dragEntered(MouseDragEvent mouseDragEvent) {
            LineEntry sourceEntry = UI.Events.selectLineEntry(mouseDragEvent.getGestureSource());

            if (mouseDragEvent.getGestureSource() != this && sourceEntry != null) {
                UI.Events.setDragCursorEnteredImage();
            }
        }

        /**
         * Handles mouse exit event from the up button
         * @param mouseDragEvent the mouseevent representing the exit
         */
        private void dragExited(MouseDragEvent mouseDragEvent) {
            if (!Properties.DRAG_DROP_CURSOR_FILE_ICON.getValue())
                UI.Events.setDragCursorImage(null);
        }

        /**
         * This method handles a drag being dropped onto the up button
         * @param mouseDragEvent the mouse drag event representing the drag being dropped
         */
        private void dragDropped(MouseDragEvent mouseDragEvent) {
            mouseDragEvent.consume();
            if (!isAtRootDirectory()) {
                Object source = mouseDragEvent.getGestureSource();
                if (source != this) {
                    LineEntry sourceEntry = UI.Events.selectLineEntry(mouseDragEvent.getGestureSource());

                    if (sourceEntry != null) {
                        DirectoryPane sourcePane = sourceEntry.getOwningPane();

                        try {
                            CommonFile destination = directory.getExistingParent();
                            if (sourcePane == DirectoryPane.this)
                                handleDragAndDropOnSamePane(mouseDragEvent, LineEntry.newInstance(destination, DirectoryPane.this));
                            else
                                handleDragAndDropFromDifferentPane(mouseDragEvent, sourcePane, LineEntry.newInstance(destination, sourcePane));
                        } catch (FileSystemException ex) {
                            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
                        }
                    }
                }
            }

            displayDragDropTarget(false);
        }

        /**
         * This method initialises the drag and drop onto the Up button
         */
        private void initDragAndDrop() {
            setOnMouseDragEntered(this::dragEntered);
            setOnMouseDragExited(this::dragExited);
            setOnMouseDragReleased(this::dragDropped);
        }
    }

    /**
     * This class provides an abstraction that displays an empty Directory to be displayed
     * but also allows drop events to be handled on it
     */
    public class EmptyDirectoryPane extends BorderPane {
        /**
         * This constant outlines the offset between the bottom of the empty directory pane and the bottom toolbar in the MainView class.
         * It is intended to be set as the minimum height for this pane
         */
        public static final int FILE_PANEL_BOTTOM_TOOLBAR_OFFSET = UI.FILE_PANEL_HEIGHT - 100;
        /**
         * A variable to keep track of if this is shown or not
         */
        private boolean shown;

        /**
         * Constructs an instance of the empty directory pane
         */
        public EmptyDirectoryPane() {
            init();
        }

        /**
         * Initialises the empty directory pane
         */
        private void init() {
            ImageView openDirImage = new ImageView(new Image("opened_folder.png"));
            Label emptyFolder = new Label("Directory is empty");
            VBox dirBox = new VBox();
            dirBox.setSpacing(5);
            dirBox.getChildren().addAll(openDirImage, emptyFolder);
            dirBox.setAlignment(Pos.CENTER);
            setCenter(dirBox);
            setPadding(new Insets(UI.EMPTY_FOLDER_PANEL_PADDING));
            setMinHeight(FILE_PANEL_BOTTOM_TOOLBAR_OFFSET);

            initDragAndDrop();
            setOnContextMenuRequested(e -> {
                LineEntry target = UI.Events.selectLineEntry(e.getTarget());
                if (target == null) { // we didn't request a line entry's context menu, so display the EntriesBox's context menu
                    ContextMenu contextMenu = createContextMenu();
                    contextMenu.show(this, e.getScreenX(), e.getScreenY());
                }
            });
        }

        /**
         * This method initialises the drag and drop onto the Up button
         */
        private void initDragAndDrop() {
            setOnMouseDragEntered(this::dragEntered);
            setOnMouseDragExited(this::dragExited);
            setOnMouseDragReleased(this::dragDropped);
        }

        /**
         * This method handles the logic for handling a drag event that has been entered on a target on this DirectoryPane
         * @param dragEvent the drag event representing the entry
         */
        private void dragEntered(MouseDragEvent dragEvent) {
            LineEntry sourceEntry = UI.Events.selectLineEntry(dragEvent.getGestureSource());
            LineEntry targetEntry = UI.Events.selectLineEntry(dragEvent.getTarget());

            if (sourceEntry != null && sourceEntry.getOwningPane() != DirectoryPane.this
                    && (targetEntry == null || targetEntry.isFile())) {
                setStyle(UI.Events.DRAG_ENTERED_BACKGROUND);
                UI.Events.setDragCursorEnteredImage();
            }
        }

        /**
         * This method handles the logic for handling a drag event that has exited a target on this DirectoryPane
         * @param dragEvent the drag event representing the exit
         */
        private void dragExited(MouseDragEvent dragEvent) {
            setStyle(UI.WHITE_BACKGROUND);
            if (!Properties.DRAG_DROP_CURSOR_FILE_ICON.getValue())
                UI.Events.setDragCursorImage(null); // leave null as we don't need it her
        }

        /**
         * This method handles the logic for handling a drag event that has been dropped on this DirectoryPane
         * @param dragEvent the drag event representing the drop
         */
        private void dragDropped(MouseDragEvent dragEvent) {
            LineEntry sourceEntry = UI.Events.selectLineEntry(dragEvent.getGestureSource());
            DirectoryPane sourcePane = sourceEntry != null ? sourceEntry.getOwningPane():null;
            if (sourcePane != null && sourcePane != DirectoryPane.this) {
                LineEntry targetEntry = UI.Events.selectLineEntry(dragEvent.getTarget());

                if (targetEntry == null || (targetEntry.isFile() && targetEntry.getOwningPane() == DirectoryPane.this))
                    DirectoryPane.this.handleDragAndDropFromDifferentPane(dragEvent, sourcePane,
                            LineEntry.newInstance(directory, DirectoryPane.this));
            }

            dragEvent.consume();
        }

        /**
         * Creates and returns the context menu for this EntriesBox
         */
        private ContextMenu createContextMenu() {
            MenuItem paste = new MenuItem("Paste");
            boolean disabled = !canPaste();
            paste.setDisable(disabled);
            paste.setOnAction(e -> paste(DirectoryPane.this.directory));

            ContextMenu contextMenu = new ContextMenu();
            contextMenu.getItems().add(paste);

            return contextMenu;
        }

        /**
         * Returns true if the copiedEntry can be pasted here
         * @return true if can paste, false if not
         */
        private boolean canPaste() {
            boolean pasteableEntries = false;
            LineEntry target = LineEntry.newInstance(DirectoryPane.this.directory, DirectoryPane.this);
            if (target != null) {
                for (LineEntry copiedEntry : copiedEntries) {
                    pasteableEntries = pasteableEntries || isFilePasteable(copiedEntry, target);
                }

                return pasteableEntries;
            }

            return false;
        }

        /**
         * Show this empty folder pane or not
         * @param show true to show, false if now
         */
        private void show(boolean show) {
            shown = show;
        }
    }
}
