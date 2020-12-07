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

package com.simpleftp.ui.directories.tasks;

import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.LocalFileSystem;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.directories.DirectoryPane;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * This class is a background task for downloading the contents of a file as a string to display in an editor opened by a DirectoryPane
 */
public class FileStringDownloader extends Service<String> {
    private CommonFile file;
    /**
     * Need a separate connection for downloading files so it doesn't hog the main connection
     */
    private FTPConnection readingConnection;
    private DirectoryPane creatingPanel;
    private boolean errorOccurred;

    /**
     * Creates a FileStringDownloader object
     * @param file the file to download contents of. Assumed to be a file, not a directory
     * @param fileSystem the file system to download the file to
     * @param creatingPanel the panel that created this downloader
     */
    public FileStringDownloader(CommonFile file, FileSystem fileSystem, DirectoryPane creatingPanel) throws FTPException {
        this.file = file;
        this.creatingPanel = creatingPanel;
        if (file instanceof RemoteFile) {
            this.readingConnection = FTPConnection.createTemporaryConnection(fileSystem.getFTPConnection());
            this.readingConnection.connect();
            this.readingConnection.login();
            this.readingConnection.setTextTransferMode(true);
        } // only need connection for remote file
    }

    /**
     * Downloads the file in the background and opens the dialog
     */
    public void getFileString() {
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
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                String str = fileToString(file);
                if (file instanceof RemoteFile)
                    readingConnection.disconnect();
                return str;
            }
        };
    }

    /**
     * Opens the file and returns it as a string
     * @param file the file to display
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
                downloaded.deleteOnExit();

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
