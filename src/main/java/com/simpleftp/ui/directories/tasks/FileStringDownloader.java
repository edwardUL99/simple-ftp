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

import com.simpleftp.filesystem.FileUtils;
import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.LocalFileSystem;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPError;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.background.BackgroundTask;
import com.simpleftp.ui.directories.DirectoryPane;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * This class is a background task for downloading the contents of a file as a string to display in an editor opened by a DirectoryPane
 */
@Log4j2
public class FileStringDownloader implements BackgroundTask {
    private CommonFile file;
    /**
     * Need a separate connection for downloading files so it doesn't hog the main connection
     */
    private FTPConnection readingConnection;
    /**
     * The pane that created this task
     */
    private DirectoryPane creatingPanel;
    /**
     * A flag indicating that an error has occurred
     */
    private boolean errorOccurred;
    /**
     * The download service for downloading the contents
     */
    private Service<String> downloadService;
    /**
     * A flag keeping track of if this task is finished or not
     */
    private boolean finished;

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
        initDownloadService();
    }

    /**
     * Starts the download and when finished opens it
     */
    @Override
    public void start() {
        UI.addBackgroundTask(this);
        finished = false;
        downloadService.start();
    }

    /**
     * FileStringDownloaded does not schedule, instead it just calls start
     */
    @Override
    public void schedule() {
        start();
    }

    /**
     * Stops the background task and the underlying service
     */
    @Override
    public void cancel() {
        downloadService.cancel();
    }

    /**
     * Returns true if this task is running
     *
     * @return true if running, false if not
     */
    @Override
    public boolean isRunning() {
        return downloadService.isRunning();
    }

    /**
     * Use this call to determine if a task is ready
     *
     * @return true if ready, false if not
     */
    @Override
    public boolean isReady() {
        return downloadService.isRunning();
    }

    /**
     * Returns a boolean determining if the task is finished.
     * This should be tracked by a variable in the implementing class and not by checking any underlying JavaFX Service state since that has to be done from the JavaFX thread
     *
     * @return true if finished, false if not
     */
    @Override
    public boolean isFinished() {
        return finished;
    }

    /**
     * Initialises the download service
     */
    private void initDownloadService() {
        downloadService = new Service<>() {
            @Override
            protected Task<String> createTask() {
                return FileStringDownloader.this.createTask();
            }
        };

        downloadService.setOnSucceeded(e -> {
            finished = true;
            if (!errorOccurred) {
                String contents = (String) e.getSource().getValue();
                UI.showFileEditor(creatingPanel, file, contents);
            }
            disconnectConnection();
            UI.removeBackgroundTask(this);
        });

        downloadService.setOnCancelled(e -> {
            finished = true;
            disconnectConnection();
        });
        downloadService.setOnFailed(e -> {
            finished = true;
            disconnectConnection();
        });
    }

    /**
     * Disconnects the ftp connection if it was connected
     */
    private void disconnectConnection() {
        if (file instanceof RemoteFile) {
            try {
                readingConnection.disconnect();
            } catch (FTPException ex) {
                log.warn("Failed to disconnect a connection used to download file contents");
            }
        }
    }

    /**
     * Creates the task for the service
     * @return the task
     */
    private Task<String> createTask() {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                String str = fileToString(file);
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

                LocalFile downloaded;
                if (remoteFile.isSymbolicLink()) {
                    remoteFile = new RemoteFile(remoteFile.getSymbolicLinkTarget());
                    downloaded = new LocalFile(FileUtils.TEMP_DIRECTORY + FileUtils.PATH_SEPARATOR + remoteFile.getName());
                } else {
                    downloaded = new LocalFile(FileUtils.TEMP_DIRECTORY + FileUtils.PATH_SEPARATOR + remoteFile.getName());
                }

                if (new LocalFileSystem(readingConnection).addFile(remoteFile, downloaded.getParentFile().getAbsolutePath())) { // download the file
                    String ret = fileToString(downloaded);
                    downloaded.deleteOnExit();

                    return ret;
                } else {
                    throw new FTPError("Failed to download remote file to temp directory", readingConnection.getReplyString());
                }
            } catch (Exception ex) {
                Platform.runLater(() -> UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled()));
                errorOccurred = true;
                return null;
            }
        }

        return str.toString();
    }
}
