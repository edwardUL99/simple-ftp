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

import com.simpleftp.FTPSystem;
import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.LocalFileSystem;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.RemoteFileSystem;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ftp.exceptions.FTPRemotePathNotFoundException;
import com.simpleftp.local.exceptions.LocalPathNotFoundException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.containers.FilePanelContainer;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Predicate;

/*
The conditions for determining if an exception or error dialog show up may have to be reconsidered
 */

/**
 * Represents a panel of files. Can be used for local or remote directories
 */
public class FilePanel extends VBox {
    /**
     * The status panel which contains all the buttons and current directory label
     */
    private HBox statusPanel;
    /**
     * The label outlining current directory
     */
    private Label currentDirectoryLabel;
    /**
     * The label showing the path to the current directory
     */
    private Label currentDirectory;
    /**
     * A tool tip for displaying the current directory when mouse is hovered over currentDirectory in case it is abbreviated
     */
    private Tooltip currentDirectoryTooltip;
    /**
     * Button for refreshing the FilePanel and its entries
     */
    private Button refresh;
    /**
     * Button for moving up to the parent directory
     */
    private Button up;
    /**
     * The list of line entries inside in entries box
     */
    private ArrayList<LineEntry> lineEntries;
    /**
     * The directory that this FilePanel is currently listing
     */
    @Getter
    private CommonFile directory;
    /**
     * The FilePanelContainer that is holding this FilePanel
     */
    @Getter
    private FilePanelContainer parentContainer;
    /**
     * The ScrollPane that will provide scrolling functionality for the entriesBox
     */
    private ScrollPane entriesScrollPane;
    /**
     * The VBox which will hold all the LineEntries
     */
    private VBox entriesBox;
    /**
     * The file system this FilePanel is connected to
     */
    @Getter
    private FileSystem fileSystem;
    /**
     * The max length for file path before it is shortened and ... added to the end of it
     */
    private static final int MAX_FILE_PATH_LENGTH = 25;
    /**
     * Flag for showing hidden files
     * By default, it is false
     */
    private boolean showHiddenFiles;
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
     * Constructs a FilePanel object with the specified directory
     * @param directory the directory object. Can be Local or Remote file
     * @throws FileSystemException if the directory is not in fact a directory
     */
    public FilePanel(CommonFile directory) throws FileSystemException {
        setStyle(UI.WHITE_BACKGROUND);
        lineEntries = new ArrayList<>();

        initButtons();
        initStatusPanel();
        initEntriesBox();
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
     * Initialises the CurrentDirectory labels of the FilePanel and returns the header label
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
     * Intialises the status panel which contains the buttons and current directory
     */
    private void initStatusPanel() {
        statusPanel = new HBox();
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
        entriesBox.setStyle(UI.WHITE_BACKGROUND);
    }

    /**
     * Initialises the directory that this FilePanel is set up to view
     * @param directory the directory to set as initial directory
     * @throws FileSystemException if the directory is not in fact a directory
     */
    private void initDirectory(CommonFile directory) throws FileSystemException {
        if (!directory.isADirectory()) {
            throw new FileSystemException("The directory provided is not a directory");
        }

        if (directory instanceof RemoteFile) {
            fileSystem = new RemoteFileSystem(FTPSystem.getConnection()); // it is expected that when you have a FilePanel created, at this stage you already have a connection established and logged in
            // the login process should call FTPSystem.setConnection
        } else {
            fileSystem = new LocalFileSystem(FTPSystem.getConnection());
        }

        setDirectory(directory);
        refresh();

        try {
            FTPConnection connection = fileSystem.getFTPConnection();
            if (connection != null && !(directory instanceof LocalFile)) {
                connection.changeWorkingDirectory(directory.getFilePath());
            }
        } catch (FTPException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, true);
        }

        setCurrDirText(directory.getFilePath());
    }

    /**
     * Sets the FilePanelContainer that is the parent, i.e. contains this FilePanel.
     * This action hooks the two together.
     * Calling setParentContainer automatically links the given container to this file panel
     * @param parentContainer the container to add this FilePanel to
     */
    public void setParentContainer(FilePanelContainer parentContainer) {
        this.parentContainer = parentContainer;
        if (this.parentContainer.getFilePanel() != this) // prevent a cycle of infinite recursion
            this.parentContainer.setFilePanel(this);
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

    /**
     * Controls going up to parent directory
     */
    public void up() {
        try {
            if (directory instanceof LocalFile) {
                LocalFile localFile = (LocalFile) directory;
                String parent = localFile.getParent();
                if (parent != null) {
                    LocalFile parentFile = new LocalFile(parent);
                    if (parentFile.exists() && parentFile.canRead()) {
                        setDirectory(parentFile);
                        refresh();
                    }
                }
            } else {
                RemoteFile remoteFile;
                FTPConnection connection = fileSystem.getFTPConnection();
                if (connection != null) {
                    try {
                        if (!connection.getWorkingDirectory().equals("/")) { // you are not at root, so go up
                            boolean changed = connection.changeToParentDirectory();
                            if (changed) {
                                String currentWorkingDirectory = connection.getWorkingDirectory();
                                remoteFile = new RemoteFile(currentWorkingDirectory);
                                setDirectory(remoteFile);
                                refresh();
                            }
                        }
                    } catch (FTPException ex) {
                        UI.doException(ex, UI.ExceptionType.ERROR, true);
                    }
                }
            }

        } catch (FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.EXCEPTION, true);
        }
    }

    /**
     * Changes directory. Refresh should be called after this action
     * @param directory the directory to change to, local, or remote
     * @throws FileSystemException if the directory is not a directory
     */
    public void setDirectory(CommonFile directory) throws FileSystemException {
        if (directory.isADirectory()) {
            this.directory = directory;

            if (directory instanceof RemoteFile) {
                FTPConnection connection = fileSystem.getFTPConnection();
                if (connection != null) {
                    try {
                        boolean changed = connection.changeWorkingDirectory(directory.getFilePath());

                        if (!changed) {
                            UI.doError("Error changing directory", "Current directory may not have been changed successfully. FTP Reply: " + connection.getReplyString());
                        }
                    } catch (FTPException ex) {
                        UI.doException(ex, UI.ExceptionType.ERROR, true);
                    }
                }
            }

            setCurrDirText(directory.getFilePath());
        } else {
            throw new FileSystemException("The directory for a FilePanel must be in fact a directory, not a file");
        }
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
    private boolean showFile(CommonFile file, Predicate<CommonFile> hiddenChecker) {
        boolean showFile = hiddenChecker.test(file);

        if (!showFile) // if it is hidden, it will never be shown regardless of matching mask or not
            return false;

        if (fileMask != null) {
            showFile = checkNameAgainstMask(file.getName());
        }

        return showFile;
    }

    /**
     * Constructs the list of line entries from the files listed by the local file
     * @param lineEntries the list of line entries to populate
     * @param localFile the file to list
     */
    private void constructListOfLocalFiles(ArrayList<LineEntry> lineEntries, LocalFile localFile) {
        try {
            CommonFile[] files = fileSystem.listFiles(localFile.getFilePath());
            if (files == null || files.length == 0) {
                lineEntries.clear();
            } else {
                for (CommonFile file : fileSystem.listFiles(localFile.getFilePath())) {
                    LocalFile file1 = (LocalFile) file;
                    try {
                        boolean showFile = showFile(file, e -> showHiddenFiles || !file1.isHidden());

                        if (showFile) {
                            addLineEntry(createLineEntry(file), lineEntries);
                        }
                    } catch (FTPRemotePathNotFoundException | LocalPathNotFoundException ex) {
                        UI.doException(ex, UI.ExceptionType.ERROR, true);
                        lineEntries.clear();
                    } catch (FTPException ex) {
                        UI.doException(ex, UI.ExceptionType.EXCEPTION, true); // this exception shouldn't happen so indicate it as a major problem
                    }
                }
            }
        } catch (FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.EXCEPTION, true);
            lineEntries.clear();
        }
    }

    /**
     * Constructs the list of line entries from the files listed by the remote file
     * @param lineEntries the list of line entries to populate
     * @param remoteFile the file to list
     */
    private void constructListOfRemoteFiles(ArrayList<LineEntry> lineEntries, RemoteFile remoteFile) {
        try {
            String path = remoteFile.getFilePath();
            for (CommonFile f : fileSystem.listFiles(path)) {
                boolean showFile = showFile(f, e -> {
                    String fileName = f.getName();
                    return (showHiddenFiles && !fileName.equals(".") && !fileName.startsWith("..")) || !fileName.startsWith(".");
                });

                if (showFile) {
                    addLineEntry(createLineEntry(f), lineEntries);
                }
            }
        } catch (FTPException | FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, true);
            lineEntries.clear();
        }
    }

    /**
     * Renames the specified local file
     * @param localFile the file to rename
     */
    private void renameLocalFile(final LocalFile localFile) {
        String filePath = localFile.getFilePath();
        String parentPath = new File(filePath).getParent();
        String fileName = localFile.getName();
        String newPath = UI.doRenameDialog(fileName);

        if (newPath != null) {
            newPath = new File(newPath).getName(); // ensure it is just the base name
            if (!newPath.equals(fileName)) {
                newPath = parentPath + System.getProperty("file.separator") + newPath;

                if (localFile.renameTo(new File(newPath))) {
                    UI.doInfo("File Renamed", "File has been renamed successfully");
                    double vPosition = entriesScrollPane.getVvalue();
                    double hPosition = entriesScrollPane.getHvalue();
                    refresh();
                    entriesScrollPane.setVvalue(vPosition);
                    entriesScrollPane.setHvalue(hPosition); // resets the position of the scrollbars to where they were before the refresh
                } else {
                    UI.doError("Rename Failed", "Failed to rename file");
                }
            }
        }
    }

    /**
     * Renames the specified remote file
     * @param remoteFile the file to rename
     */
    private void renameRemoteFile(final RemoteFile remoteFile) {
        String filePath = remoteFile.getFilePath();
        String parentPath = new File(filePath).getParent();
        String fileName = remoteFile.getName();
        String newPath = UI.doRenameDialog(fileName);

        if (newPath != null) {
            newPath = new File(newPath).getName(); // ensure it is just the base name
            if (!newPath.equals(fileName)) {
                newPath = parentPath + "/" + newPath;

                try {
                    FTPConnection connection = fileSystem.getFTPConnection();
                    if (connection.renameFile(filePath, newPath)) {
                        UI.doInfo("File Renamed", "File has been renamed successfully");
                        double vPosition = entriesScrollPane.getVvalue();
                        double hPosition = entriesScrollPane.getHvalue();
                        refresh();
                        entriesScrollPane.setVvalue(vPosition);
                        entriesScrollPane.setHvalue(hPosition); // resets the position of the scrollbars to where they were before the refresh
                    } else {
                        String replyString = connection.getReplyString();
                        UI.doError("Rename Failed", "Failed to rename file with error code: " + replyString);
                    }
                } catch (FTPException ex) {
                    UI.doException(ex, UI.ExceptionType.ERROR, true);
                }
            }
        }
    }

    /**
     * Handles when rename is called on the context menu with the specified line entry
     * @param lineEntry
     */
    private void renameLineEntry(final LineEntry lineEntry) {
        CommonFile file = lineEntry.getFile();
        if (file instanceof LocalFile) {
            renameLocalFile((LocalFile)file);
        } else {
            renameRemoteFile((RemoteFile)file);
        }
    }

    /**
     * Creates a line entry, adding a context menu to it and returns it
     * @param file the file the create the line entry from
     * @return the line entry created
     * @throws FileSystemException if an error occurs
     */
    private LineEntry createLineEntry(CommonFile file) throws FileSystemException, FTPException {
        LineEntry lineEntry;
        if (file.isNormalFile()) {
            lineEntry = new FileLineEntry(file, this);
        } else {
            lineEntry = new DirectoryLineEntry(file, this);
        }

        ContextMenu contextMenu = new ContextMenu();
        MenuItem menuItem1 = new MenuItem("Open");
        menuItem1.setOnAction(e -> openLineEntry(lineEntry));
        MenuItem menuItem2 = new MenuItem("Rename");
        menuItem2.setOnAction(e -> renameLineEntry(lineEntry));
        MenuItem menuItem3 = new MenuItem("Delete");
        menuItem3.setOnAction(e -> parentContainer.delete()); // right clicking this would have selected it in the container's combo box. So use containers delete method to display confirmation dialog
        contextMenu.getItems().addAll(menuItem1, menuItem2, menuItem3);

        lineEntry.setOnContextMenuRequested(e -> contextMenu.show(lineEntry, e.getScreenX(), e.getScreenY()));

        return lineEntry;
    }

    /**
     * Constructs the list of line entries to display
     * @return the list of constructed line entries
     */
    private ArrayList<LineEntry> constructListOfFiles() {
        ArrayList<LineEntry> lineEntries = new ArrayList<>();

        if (directory instanceof LocalFile) {
            constructListOfLocalFiles(lineEntries, (LocalFile)directory);
        } else {
            constructListOfRemoteFiles(lineEntries, (RemoteFile)directory);
        }

        if (lineEntries.size() > 0) {
            Collections.sort(lineEntries);
        }

        return lineEntries;
    }

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
        if (lineEntries != null) {
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
        }

        if (parentContainer != null)
            parentContainer.refresh();

        entriesScrollPane.setHvalue(0); // reset scroll position
        entriesScrollPane.setVvalue(0);
    }

    /**
     * Checks if the line entry is still valid, i.e. the file specified by it is still on the file system (local or remote)
     * @param lineEntry the line entry to check
     * @return true if it still exists
     * @throws FileSystemException
     */
    private boolean entryStillExists(final LineEntry lineEntry) throws FileSystemException {
        CommonFile file = lineEntry.getFile();

        try {
            return file.exists();
        } catch (FileSystemException ex) {
            throw ex; // throw to be handled by caller
        }
    }

    /**
     * Handles double click of the specified directory entry
     * @param lineEntry the directory entry to double click
     */
    private void doubleClickDirectoryEntry(final DirectoryLineEntry lineEntry) {
        try {
            setDirectory(lineEntry.getFile());
            refresh();
        } catch (FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.EXCEPTION, true);
        }
    }

    /**
     * Handles double clicks of the specified file entry
     * @param lineEntry the file entry to double click
     */
    private void doubleClickFileEntry(final FileLineEntry lineEntry) {
        UI.showFileEditor(this, lineEntry.getFile());
    }

    /**
     * Handles the double click of the specified line entry
     * @param lineEntry the line entry to double click
     */
    private void doubleClick(final LineEntry lineEntry) {
        try {
            if (entryStillExists(lineEntry)) {
                if (lineEntry instanceof FileLineEntry) {
                    doubleClickFileEntry((FileLineEntry) lineEntry);
                } else {
                    doubleClickDirectoryEntry((DirectoryLineEntry) lineEntry);
                }
            } else {
                UI.doError("File not found", "The file " + lineEntry.getFile().getFilePath() + " does not exist...");
            }
        } catch (FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.EXCEPTION, true);
        }
    }

    /**
     * Provides the click action for the specified line entry
     * @param lineEntry the line entry to click
     */
    public void click(final LineEntry lineEntry) {
        if (parentContainer != null)
            parentContainer.setComboBoxSelection(lineEntry);
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
     * Attempts to delete the specified line entry and the file associated with it
     * @param lineEntry the line entry to remove
     * @return true if successful, false if not
     */
    public boolean deleteEntry(final LineEntry lineEntry) {
        CommonFile file = lineEntry.getFile();
        if (file instanceof LocalFile) {
            try {
                if (fileSystem.removeFile(file)) {
                    entriesBox.getChildren().remove(lineEntry);
                    lineEntries.remove(lineEntry);

                    refresh();

                    return true;
                }
            } catch (FileSystemException ex) {
                UI.doException(ex, UI.ExceptionType.ERROR, true);
            }
        } else {
            try {
                if (fileSystem.removeFile(lineEntry.getFile())) {
                    entriesBox.getChildren().remove(lineEntry);
                    lineEntries.remove(lineEntry);

                    refresh();

                    return true;
                }
            } catch (FileSystemException ex) {
                UI.doException(ex, UI.ExceptionType.ERROR, true);
            }
        }

        return false;
    }

    /**
     * Adds the specified line entry to the entries box and specified list
     * @param lineEntry the line entry to add
     * @param lineEntries the list to also add the entry to
     */
    private void addLineEntry(final LineEntry lineEntry, ArrayList<LineEntry> lineEntries) {
        entriesBox.getChildren().add(lineEntry);
        lineEntries.add(lineEntry);
    }

    /**
     * Adds the specified line entry to this panel
     * @param lineEntry the line entry to add
     */
    public void addLineEntry(final LineEntry lineEntry) {
        addLineEntry(lineEntry, lineEntries);
    }

    /**
     * Returns the files that this FilePanel is displaying
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
        } else {
            fileMaskRegex = createRegexFromGlob(fileMask);
            currentDirectoryLabel.setText("CurrentDirectory (masked): ");
        }
    }
}
