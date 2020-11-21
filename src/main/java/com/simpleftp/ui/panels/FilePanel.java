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
import com.simpleftp.ui.files.DirectoryLineEntry;
import com.simpleftp.ui.files.FileLineEntry;
import com.simpleftp.ui.files.FilePropertyWindow;
import com.simpleftp.ui.files.LineEntry;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
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

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Predicate;

/*
The conditions for determining if an exception or error dialog show up may have to be reconsidered
 */

/**
 * Represents a panel of files. Can be used for local or remote directories.
 * To create a Local FilePanel, pass in an instance of LocalFile to the constructor.
 * To create a Remote FilePanel, pass in an instance of RemoteFile to the constructor.
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
        entriesBox.setStyle(UI.WHITE_BACKGROUND);
        entriesScrollPane.setPrefHeight(UI.FILE_PANEL_HEIGHT);
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
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
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
     * Attempts to change to the remote parent directory. If not symbolic link this is connection.changeToParentDirectory, if symbolic, it is the parent folder containing symbolic link
     * @throws FTPException if an exception occurs
     */
    private void changeToRemoteParent() throws FileSystemException {
        String parentPath = UI.getParentPath(directory.getFilePath()); // the directory's path should be the current one
        RemoteFile parentFile = new RemoteFile(parentPath);
        setDirectory(parentFile);
        refresh();
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
                changeToRemoteParent();
            }
        } catch (FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
        }
    }

    /**
     * Checks the directory passed in to see if the type matches the current directory
     * @param directory the directory to check
     * @throws IllegalArgumentException if the type of directory is different to the type of the current one
     */
    private void checkFileType(CommonFile directory) throws IllegalArgumentException {
        boolean remote = this.directory instanceof RemoteFile;
        boolean local = this.directory instanceof LocalFile;

        if (remote && directory instanceof LocalFile) {
            throw new IllegalArgumentException("This is a Remote FilePanel. Directory passed in must be an instance of RemoteFile");
        } else if (local && directory instanceof RemoteFile) {
            throw new IllegalArgumentException("This is a Local FilePanel. Directory passed in must be an instance of LocalFile");
        }
    }

    /**
     * Checks if the file represents a symbolic link and throws IllegalArgumentException if not
     * @param symbolicLink the file to check for being a link
     * @throws IllegalArgumentException if it is not a symbolic link
     */
    private void checkSymbolicLink(CommonFile symbolicLink) throws IllegalArgumentException {
        if (!UI.isFileSymbolicLink(symbolicLink))
            throw new IllegalArgumentException("The file provided is not a symbolic link");
    }

    /**
     * Changes directory. Refresh should be called after this action
     * @param directory the directory to change to, local, or remote
     * @throws FileSystemException if the directory is not a directory
     * @throws IllegalArgumentException if type of the directory is different to the type that was initially passed in.
     *              You're not allowed pass in RemoteFile to constructor and then suddenly set directory to a LocalFile
     */
    public void setDirectory(CommonFile directory) throws FileSystemException, IllegalArgumentException {
        checkFileType(directory);

        if (directory.isADirectory()) {
            this.directory = directory;
            String path = directory.getFilePath();

            if (directory instanceof RemoteFile) {
                FTPConnection connection = fileSystem.getFTPConnection();
                if (connection != null) {
                    try {
                        boolean changed = connection.changeWorkingDirectory(directory.getFilePath());

                        if (!changed) {
                            UI.doError("Error changing directory", "Current directory may not have been changed successfully. FTP Reply: " + connection.getReplyString());
                        }
                    } catch (FTPException ex) {
                        UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
                    }
                }
            }

            setCurrDirText(path);
        } else {
            throw new FileSystemException("The directory for a FilePanel must be in fact a directory, not a file");
        }
    }

    /**
     * Gets the target of the directory symbolic link. It is assumed you have already checked if the file is a symbolic link before calling this method
     * @param directory the directory to get target of
     * @return the target of the symbolic link
     * @throws IOException if directory is a local file and the directory provided is not a symbolic link
     */
    private String getSymLinkTargetPath(CommonFile directory) throws IOException {
        String path;
        if (directory instanceof LocalFile) {
            path = Files.readSymbolicLink(((LocalFile)directory).toPath()).toString();
            if (path.startsWith(".") || path.startsWith("..")) {
                String currentPath = this.directory.getFilePath();
                if (currentPath.equals(UI.PATH_SEPARATOR))
                    currentPath = "";
                else if (currentPath.endsWith(UI.PATH_SEPARATOR))
                    currentPath = currentPath.substring(0, currentPath.length() - 1);
                path = currentPath + UI.PATH_SEPARATOR + path;
            }
        } else {
            path = ((RemoteFile)directory).getFtpFile().getLink();
        }

        return path;
    }

    /**
     * This method is for changing to a directory that is a symbolic link and indicates to follow it to the destination.
     * setDirectory called on symbolic link follows it symbolically, represents it as a folder of the parent
     * @param directory the directory to change to
     * @throws FileSystemException if an error occurs
     * @throws IllegalArgumentException if the directory is not in fact a directory and is not a symbolic link
     */
    public void setDirectorySymbolicLink(CommonFile directory) throws FileSystemException, IllegalArgumentException {
        checkFileType(directory);
        checkSymbolicLink(directory);

        try {
            String path = getSymLinkTargetPath(directory);
            path = (String)parentContainer.pathToAbsolute(path, directory instanceof LocalFile, true)[0];
            CommonFile targetFile = fileSystem.getFile(path);
            setDirectory(targetFile);
            refresh();
        } catch (IOException ex) {
            UI.doException(ex, UI.ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled()); // treat this as an exception dialog because this shouldn't happen
        } catch (FTPException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
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
                            LineEntry constructed = createLineEntry(file1);

                            if (constructed != null)
                                lineEntries.add(constructed);
                        }
                    } catch (FTPRemotePathNotFoundException | LocalPathNotFoundException ex) {
                        UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
                        lineEntries.clear();
                    } catch (FTPException ex) {
                        UI.doException(ex, UI.ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled()); // this exception shouldn't happen so indicate it as a major problem
                    }
                }
            }
        } catch (FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled());
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
                    LineEntry constructed = createLineEntry(f);

                    if (constructed != null)
                        lineEntries.add(constructed);
                }
            }
        } catch (FTPException | FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
            lineEntries.clear();
        }
    }

    /**
     * Renames the specified local file
     * @param localFile the file to rename
     */
    private void renameLocalFile(final LocalFile localFile) {
        String filePath = localFile.getFilePath();
        String parentPath = UI.getParentPath(filePath);

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
        String parentPath = UI.getParentPath(filePath);

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
                    UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
                }
            }
        }
    }

    /**
     * Handles when rename is called on the context menu with the specified line entry
     * @param lineEntry the line entry to rename the file of
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
        } else if (file.isADirectory()) {
            lineEntry = new DirectoryLineEntry(file, this);
        } else {
            return null;
        }

        ContextMenu contextMenu = new ContextMenu();
        MenuItem menuItem1 = new MenuItem("Open");
        menuItem1.setOnAction(e -> openLineEntry(lineEntry));
        MenuItem menuItem2 = new MenuItem("Rename");
        menuItem2.setOnAction(e -> renameLineEntry(lineEntry));
        MenuItem menuItem3 = new MenuItem("Delete");
        menuItem3.setOnAction(e -> parentContainer.delete()); // right clicking this would have selected it in the container's combo box. So use containers delete method to display confirmation dialog
        MenuItem menuItem4 = new MenuItem("Properties");
        menuItem4.setOnAction(e -> new FilePropertyWindow(lineEntry).show());
        contextMenu.getItems().addAll(menuItem1, menuItem2, menuItem3, menuItem4);

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
            CommonFile file = lineEntry.getFile();
            setDirectory(file);
            refresh();
        } catch (FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled());
        }
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
    private void doubleClickFileEntry(final FileLineEntry lineEntry) throws FTPException, FileSystemException {
        if (checkFileSize(lineEntry.getFile())) {
            FileStringDownloader fileStringDownloader = new FileStringDownloader(lineEntry.getFile(), fileSystem, this);
            fileStringDownloader.getFileString();
        }
    }

    /**
     * Handles the double click of the specified line entry
     * @param lineEntry the line entry to double click
     */
    private void doubleClick(final LineEntry lineEntry) {
        try {
            if (entryStillExists(lineEntry)) {
                if (lineEntry instanceof FileLineEntry) {
                    try {
                        doubleClickFileEntry((FileLineEntry) lineEntry);
                    } catch (FTPException ex) {
                        UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
                    }
                } else {
                    doubleClickDirectoryEntry((DirectoryLineEntry) lineEntry);
                }
            } else {
                UI.doError("File not found", "The file " + lineEntry.getFile().getFilePath() + " does not exist...");
            }
        } catch (FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
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
                UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
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
                UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
            }
        }

        return false;
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

/**
 * A Task for downloading the file in the background
 */
class FileStringDownloader extends Service<String> {
    private CommonFile file;
    /**
     * Need a separate connection for downloading files so it doesn't hog the main connection
     */
    private FTPConnection readingConnection;
    private FilePanel creatingPanel;
    private boolean errorOccurred;

    /**
     * Creates a FileStringDownloader object
     * @param file the file to download contents of. Assumed to be a file, not a directory
     * @param fileSystem the file system to download the file to
     * @param creatingPanel the panel that created this downloader
     */
    FileStringDownloader(CommonFile file, FileSystem fileSystem, FilePanel creatingPanel) throws FTPException {
        this.file = file;
        this.creatingPanel = creatingPanel;
        this.readingConnection = new FTPConnection();
        this.readingConnection.setFtpServer(fileSystem.getFTPConnection().getFtpServer());
        this.readingConnection.setTimeoutTime(200);
        this.readingConnection.connect();
        this.readingConnection.login();
        this.readingConnection.setTextTransferMode(true);
    }

    /**
     * Downloads the file in the background and opens the dialog
     */
    void getFileString() {
        setOnSucceeded(e -> {
            if (!errorOccurred) {
                String contents = (String) e.getSource().getValue();
                Platform.runLater(() -> UI.showFileEditor(creatingPanel, file, contents));
            }
            UI.removeBackgroundTask(this);
        });
        UI.addBackgroundTask(this);
        start();
    }

    /**
     * Invoked after the Service is started on the JavaFX Application Thread.
     * Implementations should save off any state into final variables prior to
     * creating the Task, since accessing properties defined on the Service
     * within the background thread code of the Task will result in exceptions.
     *
     * @return the Task to execute
     */
    @Override
    protected Task<String> createTask() {
        return new Task<String>() {
            @Override
            protected String call() throws Exception {
                String str = fileToString(file);
                readingConnection.disconnect();
                return str;
            }
        };
    }

    /**
     * Opens the file and returns it as a string
     * @file the file to display
     * @return the file contents as a String
     * @throws IOException if the reader fails to read the file
     */
    private String fileToString(CommonFile file) throws IOException {
        StringBuilder str = new StringBuilder();

        if (file instanceof LocalFile) {
            LocalFile localFile = (LocalFile)file;

            if (UI.canOpenFile(localFile)) {
                BufferedReader reader = new BufferedReader(new FileReader(localFile));

                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        str.append(line).append("\n");
                    }
                } catch (IOException ex) {
                    Platform.runLater(() -> UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled()));
                    errorOccurred = true;
                }
            } else {
                errorOccurred = true;
                Platform.runLater(() -> UI.doError("File Unsupported", "The file " + file.getName() + " is not a supported file type"));

                return null;
            }
        } else {
            try {
                RemoteFile remoteFile = (RemoteFile) file;

                LocalFile downloaded = new LocalFile(UI.TEMP_DIRECTORY + UI.PATH_SEPARATOR + remoteFile.getName());
                new LocalFileSystem(readingConnection).addFile(remoteFile, downloaded.getParentFile().getAbsolutePath()); // download the file
                String ret = fileToString(downloaded);
                downloaded.delete();

                return ret;
            } catch (Exception ex) {
                Platform.runLater(() -> UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled()));
                errorOccurred = true;
                return null;
            }
        }

        return str.toString();
    }
}