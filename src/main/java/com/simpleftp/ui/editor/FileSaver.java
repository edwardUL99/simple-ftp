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

package com.simpleftp.ui.editor;

import com.simpleftp.FTPSystem;
import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.LocalFileSystem;
import com.simpleftp.filesystem.RemoteFileSystem;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ui.UI;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.*;

/**
 * This class will provide functionality for saving a file that has been opened.
 * If the file is remote, it is expected to be saved locally in a temp folder. after saving the file can be deleted
 */
public class FileSaver {
    /**
     * The editor window saving the file
     */
    private FileEditorWindow editorWindow;
    /**
     * Constructs a file saver object
     * @param editorWindow the editor window saving the file
     */
    public FileSaver(FileEditorWindow editorWindow) {
        this.editorWindow = editorWindow;
    }

    /**
     * Saves the specified file contents in a file specified by filePath.
     * @param filePath the path to save the file to. If this is a remote file system, the changes will be saved in a temporary location and then uploaded to the specified path
     * @param savedFileContents the contents of the file to save
     */
    public void saveFile(String filePath, String savedFileContents) {
        FileUploader uploader = new FileUploader(editorWindow, filePath, savedFileContents);
        UI.addBackgroundTask(uploader);
        uploader.start();
    }
}

/**
 * The service for uploading a changed file
 */
class FileUploader extends Service<Void> {
    /**
     * The editor window saving the file
     */
    private FileEditorWindow editorWindow;
    /**
     * The path to save the file to
     */
    private String filePath;
    /**
     * The contents of the file to upload
     */
    private String savedFileContents;
    /**
     * Tracks if an exception occurred during saving
     */
    private boolean exceptionOccurred;

    /**
     * Constructs a file uploader
     * @param editorWindow the editor window saving the file
     * @param filePath the path to save the file to
     * @param savedFileContents the contents of the file to save
     */
    public FileUploader(FileEditorWindow editorWindow, String filePath, String savedFileContents) {
        this.editorWindow = editorWindow;
        this.filePath = filePath;
        this.savedFileContents = savedFileContents;
        setOnSucceeded(e -> {
            UI.removeBackgroundTask(this);

            if (!exceptionOccurred) {
                editorWindow.getCreatingPanel().refresh();
                Platform.runLater(() -> UI.doInfo("File Saved", "File " + filePath + " saved successfully"));
            }
        });
    }

    /**
     * Constructs the task to execute
     * @return task
     */
    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                saveFile();
                return null;
            }
        };
    }

    /**
     * Writes to the specified file the file contents
     * @param localFilePath the path of the temp file being edited
     * @throws IOException if an error occurs
     */
    private void writeToFile(String localFilePath) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(localFilePath));
        bufferedWriter.write(savedFileContents);
        bufferedWriter.close();
    }

    /**
     * Gets a new connection with same details to upload the saved file
     * @return the connection to upload the file with
     */
    private FTPConnection getUploadingConnection() throws FTPException {
        FileSystem fileSystem = editorWindow.getCreatingPanel().getFileSystem();
        FTPConnection uploadingConnection = new FTPConnection();
        uploadingConnection.setFtpServer(fileSystem.getFTPConnection().getFtpServer());
        uploadingConnection.connect();
        uploadingConnection.login();
        uploadingConnection.setTextTransferMode(true);
        return uploadingConnection;
    }

    /**
     * Saves the specified file contents in a file specified by filePath.
     * @throwa Exception if any error occurs
     */
    private void saveFile() {
        try {
            File file = new File(filePath);
            String fileName = file.getName();
            String saveFilePath;
            FileSystem fileSystem = editorWindow.getCreatingPanel().getFileSystem();
            boolean remoteFileSystem = fileSystem instanceof RemoteFileSystem;
            FTPConnection uploadingConnection = getUploadingConnection();

            if (remoteFileSystem) {
                saveFilePath = UI.TEMP_DIRECTORY + UI.PATH_SEPARATOR + fileName;
                file = new File(saveFilePath);
                fileSystem = new RemoteFileSystem(uploadingConnection);
            } else {
                saveFilePath = filePath;
                fileSystem = new LocalFileSystem(uploadingConnection);
            }

            writeToFile(saveFilePath);

            if (remoteFileSystem) {
                fileSystem.removeFile(filePath); // remove as we are overwriting
                String parent = new File(filePath).getParent();
                parent = parent == null ? "/" : parent;
                fileSystem.addFile(new LocalFile(saveFilePath), parent);
                file.delete();
            }

            uploadingConnection.disconnect();
            // don't need to bother add file to local file system, because write to file would have already added it
        } catch (Exception ex) {
            Platform.runLater(() -> UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled()));
            exceptionOccurred = true;
        }
    }
}