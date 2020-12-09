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

package com.simpleftp.ui.directories;

import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.exceptions.FTPConnectionFailedException;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.directories.tasks.FileStringDownloader;
import com.simpleftp.ui.files.FilePropertyWindow;
import com.simpleftp.ui.files.LineEntry;
import com.simpleftp.ui.panels.FilePanel;
import com.simpleftp.ui.views.PanelView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import lombok.Getter;

import java.util.ArrayList;
import java.util.function.Predicate;

/*
The conditions for determining if an exception or error dialog show up may have to be reconsidered
 */

/**
 * Represents a panel of Files. These files may be in different locations, hence why the class is abstract.
 * Some methods need to be defined differently depending on the type of file required. i.e. at the moment, LocalFiles have different behaviour than a RemoteFile.
 *
 * This class can only be constructed outside this package by using DirectoryPane.newInstance(CommonFile) which returns the appropriate DirectoryPane implementation for that file type.
 * You can only implement this class inside the panels package as it does not make sense to be implementing FilePanels outside of the panels package
 */
public abstract class DirectoryPane extends VBox {
    /**
     * The status panel which contains all the buttons and current symbolicLink label
     */
    private HBox statusPanel;
    /**
     * The label outlining current symbolicLink
     */
    private Label currentDirectoryLabel;
    /**
     * The label showing the path to the current symbolicLink
     */
    private Label currentDirectory;
    /**
     * A tool tip for displaying the current symbolicLink when mouse is hovered over currentDirectory in case it is abbreviated
     */
    private Tooltip currentDirectoryTooltip;
    /**
     * Button for refreshing the DirectoryPane and its entries
     */
    private Button refresh;
    /**
     * Button for moving up to the parent symbolicLink
     */
    private Button up;
    /**
     * The list of line entries inside in entries box
     */
    private ArrayList<LineEntry> lineEntries;
    /**
     * The symbolicLink that this DirectoryPane is currently listing
     */
    @Getter
    protected CommonFile directory;
    /**
     * The DirectoryPane that is holding this DirectoryPane
     */
    @Getter
    private FilePanel filePanel;
    /**
     * The ScrollPane that will provide scrolling functionality for the entriesBox
     */
    protected ScrollPane entriesScrollPane;
    /**
     * The VBox which will hold all the LineEntries
     */
    private VBox entriesBox;
    /**
     * The file system this DirectoryPane is connected to
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
    private BorderPane emptyFolderPane;
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
     * Constructs a DirectoryPane object with the specified symbolicLink
     * @param directory the symbolicLink object. Can be Local or Remote file
     * @throws FileSystemException if the symbolicLink is not in fact a symbolicLink
     */
    DirectoryPane(CommonFile directory) throws FileSystemException {
        setStyle(UI.WHITE_BACKGROUND);
        lineEntries = new ArrayList<>();

        initButtons();
        initStatusPanel();
        initEntriesBox();
        initFileSystem();
        initDirectory(directory);
        initEmptyFolderPane();
    }

    /**
     * Initialises the pane used to display an empty folder
     */
    private void initEmptyFolderPane() {
        emptyFolderPane = new BorderPane();
        ImageView openDirImage = new ImageView(new Image("opened_folder.png"));
        Label emptyFolder = new Label("Directory is empty");
        VBox dirBox = new VBox();
        dirBox.setSpacing(5);
        dirBox.getChildren().addAll(openDirImage, emptyFolder);
        dirBox.setAlignment(Pos.CENTER);
        emptyFolderPane.setCenter(dirBox);
        emptyFolderPane.setPadding(new Insets(UI.EMPTY_FOLDER_PANEL_PADDING));
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
     * Initialises the buttons and sets their respective actions
     */
    private void initButtons() {
        refresh = new Button();
        refresh.setMnemonicParsing(true);
        refresh.setText("_Refresh");
        up = new Button();
        up.setMnemonicParsing(true);
        up.setText("_Up");
        refresh.setOnAction(e -> refresh());

        up.setOnAction(e -> up());

        up.setPickOnBounds(true);
        refresh.setPickOnBounds(true);
    }

    /**
     * Intialises the status panel which contains the buttons and current symbolicLink
     */
    private void initStatusPanel() {
        statusPanel = new HBox();
        statusPanel.setPadding(new Insets(UI.UNIVERSAL_PADDING));
        statusPanel.setSpacing(10);
        statusPanel.getChildren().addAll(refresh, up, initCurrentDirectoryLabel(), currentDirectory);
        statusPanel.setBorder(new Border(new BorderStroke(Paint.valueOf("BLACK"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        statusPanel.setStyle(UI.GREY_BACKGROUND);
    }

    /**
     * Initialises the VBox containing file entries
     */
    private void initEntriesBox() {
        entriesBox = new VBox();
        entriesScrollPane = new ScrollPane();
        entriesScrollPane.setFitToWidth(true);
        entriesScrollPane.setFitToHeight(true);
        entriesScrollPane.setContent(entriesBox);
        entriesScrollPane.setStyle(UI.WHITE_BACKGROUND);
        entriesScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        entriesBox.setStyle(UI.WHITE_BACKGROUND);
        entriesScrollPane.setPrefHeight(UI.FILE_PANEL_HEIGHT);
    }

    /**
     * Initialises the file system for use with this DirectoryPane
     * @throws FileSystemException if an error occurs initialising it
     */
    abstract void initFileSystem() throws FileSystemException;

    /**
     * Initialises the symbolicLink that this DirectoryPane is set up to view
     * @param directory the symbolicLink to set as initial symbolicLink
     */
    private void initDirectory(CommonFile directory) throws FileSystemException {
        setDirectory(directory);
        refresh();

        setCurrDirText(directory.getFilePath());
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
     * Sets the text of the current symbolicLink text in the status panel and abbreviates it if it is too long
     * @param currentDirectory the symbolicLink to change the text to
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

    /**
     * Checks if you are at the root symbolicLink
     * @return true if at the root symbolicLink
     */
    public boolean isAtRootDirectory() {
        return new LocalFile(directory.getFilePath()).getParent() == null;
    }

    /**
     * Controls going up to parent symbolicLink
     */
    public abstract void up();

    /**
     * Checks the symbolicLink passed in to see if the type matches the dfile type this DirectoryPane is for.
     * setDirectory calls this
     * @param directory the symbolicLink to check
     * @throws IllegalArgumentException if the type of symbolicLink is different to the type of the current one
     */
    abstract void checkFileType(CommonFile directory) throws IllegalArgumentException;

    /**
     * Returns the path of the current working symbolicLink
     * @return the current working symbolicLink path
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
     * Changes symbolicLink but doesn't check the type of the symbolicLink or if it is a symbolicLink.
     * This is used internally as it is called by doubleClickDirectoryEntry. Since, you knew it was a symbolicLink to create a symbolicLink entry, you don't need to check it again.
     * The types will also always stay the same. The public method setDirectory is a wrapper for this, doing validation checks and then calling this.
     * While, internally these checks should be enforced, it can't be guaranteed an external class calling it would have stuck to them.
     * @param directory the symbolicLink to set
     */
     void setDirectoryUnchecked(CommonFile directory) {
        if (this.directory != null)
            UI.closeFile(getCurrentWorkingDirectory()); // after successfully leaving this symbolicLink to the new one, close it
        this.directory = directory;
        String path = directory.getFilePath();
        UI.openFile(path); // open the new symbolicLink

        setCurrDirText(path);
    }

    /**
     * Changes symbolicLink. Refresh should be called after this action
     * @param directory the symbolicLink to change to, local, or remote
     * @throws FileSystemException if the symbolicLink is not a symbolicLink
     * @throws IllegalArgumentException if type of the symbolicLink is different to the type that was initially passed in.
     *              You're not allowed pass in RemoteFile to constructor and then suddenly set symbolicLink to a LocalFile or it's not a symbolicLink
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
     * @throws IllegalArgumentException if the symbolicLink is not in fact a symbolicLink and is not a symbolic link
     */
    private void setSymbolicLinkTargetDir(CommonFile symbolicLink) throws FileSystemException, IllegalArgumentException {
        checkFileType(symbolicLink);

        String path = symbolicLink.getSymbolicLinkTarget();
        if (symbolicLink.isNormalFile())
        {
            // go to the parent folder of the file
            path = UI.getParentPath(path);
        }

        CommonFile targetFile = fileSystem.getFile(path);
        setDirectory(targetFile); // need to use the checked setDirectory as this is a public method
        refresh();
    }

    /**
     * Opens the given symbolic link. If it is a symbolicLink, it goes to the target symbolicLink. If it is a file, it goes to the parent of the target file
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

        ContextMenu contextMenu = new ContextMenu();
        MenuItem menuItem1 = new MenuItem("Open");
        menuItem1.setOnAction(e -> openLineEntry(lineEntry));
        MenuItem menuItem2 = new MenuItem("Rename");
        menuItem2.setOnAction(e -> renameLineEntry(lineEntry));
        MenuItem menuItem3 = new MenuItem("Delete");
        menuItem3.setOnAction(e -> filePanel.delete()); // right clicking this would have selected it in the container's combo box. So use containers delete method to display confirmation dialog
                                                             // as a consequence, this method also checks if the file is open or not. If this call is to be changed to this.delete(), add the isOpen check and confirmation dialog to that method
                                                            // this.delete doesn't remove the file from the combo box anyway (although refresh would get that)
        MenuItem menuItem4 = new MenuItem("Properties");
        menuItem4.setOnAction(e -> {
            try {
                new FilePropertyWindow(lineEntry).show();
            } catch (FileSystemException ex) {
                if (FTPSystem.isDebugEnabled())
                    ex.printStackTrace();
                UI.doError("Properties Window Failure", "Failed to open the properties window for file " + lineEntry.getFilePath() + ".");
            }
        });
        contextMenu.getItems().addAll(menuItem1, menuItem2, menuItem3, menuItem4);

        lineEntry.setOnContextMenuRequested(e -> contextMenu.show(lineEntry, e.getScreenX(), e.getScreenY()));

        return lineEntry;
    }

    /**
     * Constructs the list of line entries to display
     * @return the list of constructed line entries
     */
    abstract ArrayList<LineEntry> constructListOfFiles();

    /**
     * Adds the list of line entries to the entriesBox
     * @param lineEntries the line entries to add
     */
    private void addLineEntriesFromList(ArrayList<LineEntry> lineEntries) {
        lineEntries.forEach(entriesBox.getChildren()::add);
    }

    /**
     * Refreshes this file panel
     */
    public void refresh() {
        ArrayList<LineEntry> lineEntries = constructListOfFiles();
        this.lineEntries.clear();
        getChildren().clear();
        getChildren().add(statusPanel);
        entriesBox.getChildren().clear();
        if (lineEntries.size() > 0) {
            addLineEntriesFromList(lineEntries);
            getChildren().add(entriesScrollPane);
        } else {
            getChildren().add(emptyFolderPane);
        }
        this.lineEntries = lineEntries;

        if (filePanel != null)
            filePanel.refresh();

        entriesScrollPane.setHvalue(0); // reset scroll position
        entriesScrollPane.setVvalue(0);
    }

    /**
     * Handles double click of the specified symbolicLink entry
     * @param lineEntry the symbolicLink entry to double click
     */
    private void doubleClickDirectoryEntry(final LineEntry lineEntry) {
        CommonFile file = lineEntry.getFile();
        setDirectoryUnchecked(file);
        refresh();
    }

    /**
     * Checks the size of the file and if under 100MB returns true, if over, gets confirmation if file size is ok and returns true
     * @param file the file to check size of
     * @return true if under 100MB or if over, the user confirmed it is ok to open this file
     * @throws FileSystemException if an error occurs checking file size if the file is a remote file
     */
    private boolean checkFileSize(CommonFile file) throws FileSystemException {
        long size = file.getSize();

        if (size >= 100000000)  {
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
            FileStringDownloader fileStringDownloader = new FileStringDownloader(file, fileSystem, this);
            fileStringDownloader.getFileString();
        }
    }

    /**
     * Handles the double click of the specified line entry
     * @param lineEntry the line entry to double click
     */
    private void doubleClick(final LineEntry lineEntry) {
        try {
            String filePath = lineEntry.getFilePath();
            if (!UI.isFileOpened(filePath)) {
                CommonFile file = lineEntry.getFile();
                file.refresh(); // update the existence information if this is a RemoteFile as the status may have changed after this file was loaded
                            // RemoteFile sorts of "caches" the file retrieved from the remote server to update performance rather than retrieving the info from the server every time. But this info may not be up to date
                           // Calling refresh updates the cache of the file if RemoteFile
                if (file.isNormalFile()) {
                    try {
                        doubleClickFileEntry(lineEntry);
                        UI.openFile(filePath); // only open it if an error doesn't occur
                    } catch (FTPException ex) {
                        checkFTPConnectionException(ex);
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
            Throwable cause = ex.getCause();

            if (cause instanceof FTPException)
                checkFTPConnectionException((FTPException)cause);

            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
        }
    }

    /**
     * Provides the click action for the specified line entry
     * @param lineEntry the line entry to click
     */
    public void click(final LineEntry lineEntry) {
        if (filePanel != null)
            filePanel.setComboBoxSelection(lineEntry);
    }

    /**
     * "Opens" the specified lineEntry. This is the equivalent to double clicking it
     * If it is a symbolicLink, it will change the symbolicLink this file panel is in
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
        entriesBox.getChildren().remove(lineEntry);
        lineEntries.remove(lineEntry);
    }

    /**
     * Attempts to delete the specified line entry and the file associated with it
     * @param lineEntry the line entry to remove
     * @return true if successful, false if not
     */
    public boolean deleteEntry(final LineEntry lineEntry) {
        CommonFile file = lineEntry.getFile();

        try {
            if (fileSystem.removeFile(file)) {
                entriesBox.getChildren().remove(lineEntry);
                lineEntries.remove(lineEntry);

                return true;
            }
        } catch (FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
        }

        return false;
    }

    /**
     * Returns the files that this DirectoryPane is displaying
     * @return list of displayed files
     */
    public ArrayList<LineEntry> filesDisplayed() {
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
     * Sets the mask used for displaying files. Refresh should be called afterwards
     * @param fileMask the file mask to show
     */
    public void setFileMask(String fileMask) {
        this.fileMask = fileMask;

        if (fileMask == null) {
            fileMaskRegex = null;
            currentDirectoryLabel.setText("Current Directory:");
            MAX_FILE_PATH_LENGTH = 30;
        } else {
            fileMaskRegex = createRegexFromGlob(fileMask);
            currentDirectoryLabel.setText("Current Directory (masked): ");
            MAX_FILE_PATH_LENGTH = 20;
        }

        setCurrDirText(getCurrentWorkingDirectory());
    }

    /**
     * Creates an instance of DirectoryPane based on the given symbolicLink
     * @param directory the symbolicLink to initialise with
     * @return the constructed file panel
     * @throws NullPointerException if symbolicLink is null
     */
    public static DirectoryPane newInstance(CommonFile directory) throws FileSystemException {
        if (directory == null)
            throw new NullPointerException("The CommonFile object passed in is null");

        if (directory instanceof LocalFile)
            return new LocalDirectoryPane((LocalFile)directory);
        else
            return new RemoteDirectoryPane((RemoteFile)directory);
    }

    /**
     * Checks the given FTPException and determines if the exception is a FTPConnectionFailedException, if so, it "kills" the remote panel in the panel view
     * @param exception the exception to check
     */
    public void checkFTPConnectionException(FTPException exception) {
        if (filePanel != null) {
            PanelView panelView = filePanel.getPanelView();
            if (exception instanceof FTPConnectionFailedException && panelView != null)
                panelView.emptyRemotePanel();
        }
    }
}
