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
import com.simpleftp.ui.dialogs.ErrorDialog;
import com.simpleftp.ui.dialogs.ExceptionDialog;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a panel of files. Can be used for local or remote directories
 */
public class FilePanel extends VBox {
    private HBox buttonPanel;
    private Button refresh;
    private ArrayList<LineEntry> lineEntries;
    private CommonFile directory; // the directory this FilePanel is viewing

    /**
     * Constructs a FilePanel object with the specified directory
     * @param directory the directory object. Can be Local or Remote file
     * @throws FileSystemException if the directory is not in fact a directory
     */
    public FilePanel(CommonFile directory) throws FileSystemException {
        setSpacing(2);
        buttonPanel = new HBox();
        buttonPanel.setSpacing(10);
        refresh = new Button("Refresh");
        lineEntries = new ArrayList<>();
        refresh.setOnAction(e -> refresh());
        buttonPanel.getChildren().add(refresh);
        getChildren().add(buttonPanel);
        buttonPanel.setBorder(new Border(new BorderStroke(Paint.valueOf("BLACK"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        buttonPanel.setStyle("-fx-background-color: lightgrey;");
        setDirectory(directory);
        constructListOfFiles();
    }

    /**
     * Changes directory. Refresh should be called after this action
     * @param directory the directory to change to, local, or remote
     * @throws FileSystemException if the directory is not a directory
     */
    public void setDirectory(CommonFile directory) throws FileSystemException {
        if (directory.isADirectory()) {
            this.directory = directory;
        } else {
            throw new FileSystemException("The directory for a FilePanel must be in fact a directory, not a file");
        }
    }

    private ArrayList<LineEntry> constructListOfFiles() {
        ArrayList<LineEntry> lineEntries = new ArrayList<>();

        if (directory instanceof LocalFile) {
            LocalFile localFile = (LocalFile)directory;
            for (File file : localFile.listFiles()) {
                LocalFile file1 = new LocalFile(file.getAbsolutePath());
                if (file1.isNormalFile()) {
                    try {
                        addLineEntry(new FileLineEntry(file1), lineEntries);
                    } catch (FTPRemotePathNotFoundException | LocalPathNotFoundException ex) {
                        ex.printStackTrace();
                        new ExceptionDialog(ex).showAndDoAction();
                        return null;
                    }
                }
            }
        } else {
            RemoteFile remoteFile = (RemoteFile)directory;
            FTPConnection connection = FTPSystem.getConnection();

            if (connection != null) {
                try {
                    String path = remoteFile.getFilePath();
                    FTPFile[] files = connection.listFiles(remoteFile.getFilePath());
                    if (files != null) {
                        for (FTPFile f : files) {
                            RemoteFile remFile = new RemoteFile(path + "/" + f.getName());
                            if (remFile.isNormalFile()) {
                                addLineEntry(new FileLineEntry(remFile), lineEntries);
                            }
                        }
                    }
                } catch (FTPException | FileSystemException ex) {
                    ex.printStackTrace();
                    new ExceptionDialog(ex).showAndDoAction();
                    return null;
                }
            }
        }

        return lineEntries;
    }

    private void addLineEntriesFromList(ArrayList<LineEntry> lineEntries) {
        for (LineEntry lineEntry : lineEntries) {
            getChildren().add(lineEntry);
        }
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
        }
    }

    private String fileToString(CommonFile file) {
        String str = "";

        if (file instanceof LocalFile) {
            LocalFile localFile = (LocalFile)file;
            try {
                BufferedReader reader = new BufferedReader(new FileReader(localFile));

                String line;
                while ((line = reader.readLine()) != null) {
                    str += line + "\n";
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                new ExceptionDialog(ex).showAndDoAction();
            }
        } else {
            RemoteFile remoteFile = (RemoteFile)file;
            try {
                File downloaded = FTPSystem.getConnection().downloadFile(remoteFile.getFilePath(), "/tmp");
                LocalFile localFile = new LocalFile(downloaded.getAbsolutePath());
                String ret = fileToString(localFile);
                downloaded.delete();

                return ret;
            } catch (FTPException ex) {
                ex.printStackTrace();
                new ExceptionDialog(ex).showAndDoAction();
            }
        }

        return str;
    }

    private boolean entryStillExists(final LineEntry lineEntry) throws FileSystemException {
        CommonFile file = lineEntry.getFile();

        try {
            return file.exists();
        } catch (FileSystemException ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    private void click(final LineEntry lineEntry) {
        try {
            if (entryStillExists(lineEntry)) {
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
            } else {
                new ErrorDialog("Error", "File does not exist", "The file " + lineEntry.getFile().getFilePath() + " either does not exist anymore or cannot be accessed").showAndWait();
            }
        } catch (FileSystemException ex) {
            ex.printStackTrace();
            new ExceptionDialog(ex).showAndDoAction();
        }
    }

    private boolean deleteEntry(final LineEntry lineEntry) {
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
                    ex.printStackTrace();
                }
            }
        }

        return false;
    }

    private void addLineEntry(final LineEntry lineEntry, ArrayList<LineEntry> lineEntries) {
        lineEntry.setOnMouseClicked(e -> {
            if (e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2) {
                click(lineEntry);
            }
        });
        if (lineEntry instanceof FileLineEntry) {
            ((FileLineEntry)lineEntry).setActionMenu(e -> {
                deleteEntry(lineEntry);
            });
        }

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
}
