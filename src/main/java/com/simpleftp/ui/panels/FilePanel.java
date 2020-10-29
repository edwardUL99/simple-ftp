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
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import org.apache.commons.net.ftp.FTPFile;

import javax.sound.sampled.Line;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

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
    private ArrayList<LineEntry> lineEntries;
    private CommonFile directory; // the directory this FilePanel is viewing
    private FilePanelContainer parentContainer;

    /**
     * Constructs a FilePanel object with the specified directory
     * @param directory the directory object. Can be Local or Remote file
     * @throws FileSystemException if the directory is not in fact a directory
     */
    public FilePanel(CommonFile directory) throws FileSystemException {
        //setSpacing(2);
        setStyle("-fx-background-color: white");
        statusPanel = new HBox();
        statusPanel.setSpacing(10);
        refresh = new Button("Refresh");
        up = new Button("Up");
        Label currentDirectoryLabel = new Label("Current Directory: ");
        currentDirectoryLabel.setAlignment(Pos.CENTER_LEFT);
        currentDirectoryLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, currentDirectoryLabel.getFont().getSize()));
        currentDirectory = new Label();
        currentDirectory.setAlignment(Pos.CENTER_LEFT);
        currentDirectory.setFont(Font.font("Monospaced"));
        currentDirectory.setTextAlignment(TextAlignment.CENTER);
        lineEntries = new ArrayList<>();
        refresh.setOnAction(e -> refresh());
        up.setOnAction(e -> up());
        statusPanel.getChildren().addAll(refresh, up, currentDirectoryLabel, currentDirectory);
        getChildren().add(statusPanel);
        statusPanel.setBorder(new Border(new BorderStroke(Paint.valueOf("BLACK"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        statusPanel.setStyle("-fx-background-color: lightgrey;");
        if (!directory.isADirectory()) {
            throw new FileSystemException("The directory provided is not a directory");
        }
        this.directory = directory;
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
     * If this isn't set but is inside a FilePanelContainer, actions performed here, (like refreshing, or changing directory), won't propagate up to the FilePanelContainer
     * @param parentContainer the container to add this FilePanel to
     */
    public void setParentContainer(FilePanelContainer parentContainer) {
        this.parentContainer = parentContainer;
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
        lineEntries.forEach(e -> getChildren().add(e));
    }

    /**
     * Refreshes this file panel
     */
    public void refresh() {
        ArrayList<LineEntry> lineEntries = constructListOfFiles();
        if (lineEntries != null) {
            this.lineEntries.clear();
            getChildren().remove(1, getChildren().size());
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
        try {
            if (entryStillExists(lineEntry)) {
                if (lineEntry instanceof FileLineEntry) {
                    doubleClickFileEntry((FileLineEntry)lineEntry);
                } else {
                    doubleClickDirectoryEntry((DirectoryLineEntry)lineEntry);
                }
            } else {
                UI.doError("File not found", "The file " + lineEntry.getFile().getFilePath() + " does not exist...");
            }
        } catch (FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.EXCEPTION, true);
        }
    }

    private void click(final LineEntry lineEntry) {
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
        lineEntry.setOnMouseClicked(e -> {
            if (e.getButton().equals(MouseButton.PRIMARY)) {
                if (e.getClickCount() == 1) {
                    click(lineEntry);
                } else {
                    doubleClick(lineEntry);
                }
            }
        });

        lineEntry.setOnMouseEntered(e -> lineEntry.setStyle("-fx-background-color: lightgrey;"));
        lineEntry.setOnMouseExited(e -> lineEntry.setStyle("-fx-background-color: white;"));

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
