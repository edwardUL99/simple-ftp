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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

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
        Label currentDirectoryLabel = new Label("Current Directory:");
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
        refresh = new Button("Refresh");
        up = new Button("Up");
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
    private void up() {
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
                        boolean changed = connection.changeToParentDirectory();
                        if (changed) {
                            String currentWorkingDirectory = connection.getWorkingDirectory();
                            remoteFile = new RemoteFile(currentWorkingDirectory);
                            setDirectory(remoteFile);
                            refresh();
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
                        boolean showFile = showHiddenFiles || !file1.isHidden();

                        if (showFile) {
                            if (file1.isNormalFile()) {
                                addLineEntry(new FileLineEntry(file1, this), lineEntries);
                            } else {
                                addLineEntry(new DirectoryLineEntry(file1, this), lineEntries);
                            }
                        }
                    } catch (FTPRemotePathNotFoundException | LocalPathNotFoundException ex) {
                        UI.doException(ex, UI.ExceptionType.ERROR, true);
                        lineEntries.clear();
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
                RemoteFile remFile = (RemoteFile)f;
                String name = f.getName();
                boolean showFile = (showHiddenFiles && !name.equals(".") && !name.startsWith("..")) || !name.startsWith(".");

                if (showFile) {
                    if (remFile.isNormalFile()) {
                        addLineEntry(new FileLineEntry(remFile, this), lineEntries);
                    } else {
                        addLineEntry(new DirectoryLineEntry(remFile, this), lineEntries);
                    }
                }
            }
        } catch (FTPException | FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, true);
            lineEntries.clear();
        }
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
     * Opens the specified file and returns it as a string
     * @param file the file to open
     * @return the file contents as a String
     * @throws IOException if the reader fails to read the file
     */
    private String fileToString(CommonFile file) throws IOException {
        String str = "";

        if (file instanceof LocalFile) {
            LocalFile localFile = (LocalFile)file;
            BufferedReader reader = new BufferedReader(new FileReader(localFile));

            String line;
            while ((line = reader.readLine()) != null) {
                str += line + "\n";
            }
        } else {
            try {
                RemoteFile remoteFile = (RemoteFile) file;
                LocalFile downloaded = new LocalFile(System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + remoteFile.getName());
                new LocalFileSystem(fileSystem.getFTPConnection()).addFile(remoteFile, downloaded.getParentFile().getAbsolutePath());
                String ret = fileToString(downloaded);
                downloaded.delete();

                return ret;
            } catch (FileSystemException ex) {
                UI.doException(ex, UI.ExceptionType.EXCEPTION, true);
            }
        }

        return str;
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
     * Gets the file path to display as a title on the opened file window
     * @param lineEntry the line entry being opened
     * @return the file path
     */
    private String getFilePath(final LineEntry lineEntry) {
        CommonFile file = lineEntry.getFile();
        String filePath = file.getFilePath();

        if (file instanceof RemoteFile) {
            filePath = "ftp://" + filePath;
        }

        return filePath;
    }

    /**
     * Handles double clicks of the specified file entry
     * @param lineEntry the file entry to double click
     */
    private void doubleClickFileEntry(final FileLineEntry lineEntry) {
        Stage newStage = new Stage();
        newStage.setTitle(getFilePath(lineEntry));

        ScrollPane scrollPane = new ScrollPane();
        newStage.setOnCloseRequest(e -> {
            try {
                lineEntry.refresh();
            } catch (FTPRemotePathNotFoundException | LocalPathNotFoundException ex) {
                deleteEntry(lineEntry);
            }
        });

        try {
            StackPane stackPane = new StackPane();
            TextArea textArea = new TextArea();
            textArea.setEditable(false);
            textArea.setText(fileToString(lineEntry.getFile()));
            stackPane.getChildren().add(textArea);
            scrollPane.setContent(stackPane);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            newStage.setScene(new Scene(scrollPane, 700, 700));
            newStage.show();
        } catch (IOException ex) {
            UI.doException(ex, UI.ExceptionType.EXCEPTION, true);
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
}
