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
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ftp.connections.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ftp.exceptions.FTPRemotePathNotFoundException;
import com.simpleftp.local.exceptions.LocalPathNotFoundException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.containers.FilePanelContainer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import lombok.Getter;
import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedReader;
import java.io.File;
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
    private HBox statusPanel;
    private Label currentDirectory;
    private Tooltip currentDirectoryTooltip;
    private Button refresh;
    private Button up;
    /**
     * You need to have a flag to enable/disable double click action on file entries inside in entriesBox
     * Therefore, if the statusPanel is clicked (panel or buttons), doubleClickEnabled is set to false, so double clicks here don't propagate to line entries
     * On click event into entriesBox, this should be reset to true
     *
     * This resolves GitHub issue #42
      */
    private boolean doubleClickEnabled;
    private ArrayList<LineEntry> lineEntries;
    private CommonFile directory; // the directory this FilePanel is viewing
    @Getter
    private FilePanelContainer parentContainer;
    // you need a separate VBox for line entries otherwise if in the same VBox as the status panel, any double click on either button seems to get translated to the last file in the panel
    // a separate box keeps these events separate to their own VBoxs
    private VBox entriesBox;

    /**
     * Constructs a FilePanel object with the specified directory
     * @param directory the directory object. Can be Local or Remote file
     * @throws FileSystemException if the directory is not in fact a directory
     */
    public FilePanel(CommonFile directory) throws FileSystemException {
        setStyle("-fx-background-color: white");

        doubleClickEnabled = true;

        statusPanel = new HBox();
        statusPanel.setSpacing(10);

        refresh = new Button("Refresh");
        up = new Button("Up");

        Label currentDirectoryLabel = new Label("Current Directory: ");
        currentDirectoryLabel.setPadding(new Insets(5, 0, 0, 0));
        currentDirectoryLabel.setAlignment(Pos.CENTER_LEFT);
        currentDirectoryLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, currentDirectoryLabel.getFont().getSize()));

        currentDirectory = new Label();
        currentDirectory.setPadding(new Insets(5, 0, 0, 0));
        currentDirectory.setAlignment(Pos.CENTER_LEFT);
        currentDirectory.setFont(Font.font("Monospaced"));

        lineEntries = new ArrayList<>();

        // Set doubleClickEnabled to false on interaction with the buttons to ensure that double clicks don't propagate to the last line entry

        refresh.setOnAction(e -> {
            doubleClickEnabled = false;
            refresh();
        });

        up.setOnAction(e -> {
            doubleClickEnabled = false;
            up();
        });

        statusPanel.getChildren().addAll(refresh, up, currentDirectoryLabel, currentDirectory);
        statusPanel.setBorder(new Border(new BorderStroke(Paint.valueOf("BLACK"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        statusPanel.setStyle("-fx-background-color: lightgrey;");
        statusPanel.setOnMouseClicked(e -> doubleClickEnabled = false);

        entriesBox = new VBox();
        entriesBox.setOnMouseClicked(e -> doubleClickEnabled = true);

        if (!directory.isADirectory()) {
            throw new FileSystemException("The directory provided is not a directory");
        }
        setDirectory(directory);
        refresh();
        try {
            FTPConnection connection = FTPSystem.getConnection();
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

    private void setCurrDirText(String currentDirectory) {
        if (currentDirectory.length() >= 25) {
            String currentDirectoryShortened = currentDirectory.substring(0, 12) + "...";
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
                LocalFile parentFile = new LocalFile(localFile.getParent());
                if (parentFile.exists() && parentFile.canRead()) {
                    setDirectory(parentFile);
                    refresh();
                }
            } else {
                RemoteFile remoteFile;
                FTPConnection connection = FTPSystem.getConnection();
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
                FTPConnection connection = FTPSystem.getConnection();
                if (connection != null) {
                    try {
                        boolean changed = connection.changeWorkingDirectory(directory.getFilePath());

                        if (!changed) {
                            UI.doError("Error changing directory", "Current directory may not have been changed successfully");
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

    private void constructListOfLocalFiles(ArrayList<LineEntry> lineEntries, LocalFile localFile) {
        for (File file : localFile.listFiles()) {
            LocalFile file1 = new LocalFile(file.getAbsolutePath());
            try {
                if (file1.isNormalFile()) {
                    addLineEntry(new FileLineEntry(file1, this), lineEntries);
                } else {
                    addLineEntry(new DirectoryLineEntry(file1, this), lineEntries);
                }
            } catch (FTPRemotePathNotFoundException | LocalPathNotFoundException ex) {
                UI.doException(ex, UI.ExceptionType.ERROR, true);
                lineEntries.clear();
            }
        }
    }

    private void constructListOfRemoteFiles(ArrayList<LineEntry> lineEntries, RemoteFile remoteFile) {
        FTPConnection connection = FTPSystem.getConnection();

        if (connection != null) {
            try {
                String path = remoteFile.getFilePath();
                FTPFile[] files = connection.listFiles(path);
                if (files != null) {
                    for (FTPFile f : files) {
                        String name = f.getName();
                        String filePath = !path.equals("/") ? path + "/" + name:path + name;
                        RemoteFile remFile = new RemoteFile(filePath);
                        if (remFile.isNormalFile()) {
                            addLineEntry(new FileLineEntry(remFile, this), lineEntries);
                        } else {
                            addLineEntry(new DirectoryLineEntry(remFile, this), lineEntries);
                        }
                    }
                }
            } catch (FTPException | FileSystemException ex) {
                UI.doException(ex, UI.ExceptionType.EXCEPTION, true);
                lineEntries.clear();
            }
        }
    }

    private ArrayList<LineEntry> constructListOfFiles() {
        ArrayList<LineEntry> lineEntries = new ArrayList<>();

        if (directory instanceof LocalFile) {
            constructListOfLocalFiles(lineEntries, (LocalFile)directory);
        } else {
            constructListOfRemoteFiles(lineEntries, (RemoteFile)directory);
        }

        if (lineEntries.size() == 0) {
            return null;
        } else {
            Collections.sort(lineEntries);
            return lineEntries;
        }
    }

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
            getChildren().add(entriesBox);
            addLineEntriesFromList(lineEntries);
            this.lineEntries = lineEntries;
        }

        if (parentContainer != null)
            parentContainer.refresh();
    }

    private String fileToString(CommonFile file) throws IOException, FTPException {
        String str = "";

        if (file instanceof LocalFile) {
            LocalFile localFile = (LocalFile)file;
            BufferedReader reader = new BufferedReader(new FileReader(localFile));

            String line;
            while ((line = reader.readLine()) != null) {
                str += line + "\n";
            }
        } else {
            RemoteFile remoteFile = (RemoteFile)file;
                File downloaded = FTPSystem.getConnection().downloadFile(remoteFile.getFilePath(), System.getProperty("java.io.tmpdir"));
            LocalFile localFile = new LocalFile(downloaded.getAbsolutePath());
            String ret = fileToString(localFile);
            downloaded.delete();

            return ret;
        }

        return str;
    }

    private boolean entryStillExists(final LineEntry lineEntry) throws FileSystemException {
        CommonFile file = lineEntry.getFile();

        try {
            return file.exists();
        } catch (FileSystemException ex) {
            throw ex; // throw to be handled by caller
        }
    }

    private void doubleClickDirectoryEntry(final DirectoryLineEntry lineEntry) {
        try {
            setDirectory(lineEntry.getFile());
            refresh();
        } catch (FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.EXCEPTION, true);
        }
    }

    private void doubleClickFileEntry(final FileLineEntry lineEntry) {
        Stage newStage = new Stage();
        newStage.setTitle(lineEntry.file.getFilePath());

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
        } catch (FTPException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, true);
        }
    }

    private void doubleClick(final LineEntry lineEntry) {
        if (doubleClickEnabled) {
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
            if (((LocalFile) file).delete()) {
                getChildren().remove(lineEntry);
                lineEntries.remove(lineEntry);

                return true;
            }
        } else {
            FTPConnection connection = FTPSystem.getConnection();

            if (connection != null) {
                try {
                    if (connection.removeFile(file.getFilePath())) {
                        getChildren().remove(lineEntry);
                        lineEntries.remove(lineEntry);

                        return true;
                    }
                } catch (FTPException ex) {
                    UI.doException(ex, UI.ExceptionType.ERROR, true);
                }
            }
        }

        return false;
    }

    private void addLineEntry(final LineEntry lineEntry, ArrayList<LineEntry> lineEntries) {
        /*lineEntry.setOnMouseClicked(e -> {
            if (e.getButton().equals(MouseButton.PRIMARY)) {
                if (e.getClickCount() == 1) {
                    click(lineEntry);
                } else {
                    doubleClick(lineEntry);
                }
            }
        });*/

        getChildren().add(lineEntry);
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
}
