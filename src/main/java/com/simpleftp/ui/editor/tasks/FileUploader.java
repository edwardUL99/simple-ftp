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

package com.simpleftp.ui.editor.tasks;

import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.background.BackgroundTask;
import com.simpleftp.ui.directories.DirectoryPane;
import com.simpleftp.ui.editor.FileEditorWindow;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

/**
 * The service for uploading the text of a changed file
 * Abstract as uploads differ between file types. E.g local files aren't really uploaded, so their definition of the upload method would be just clean up.
 * This can only be instantiated by calling newInstance which will return the appropriate implementation for that file
 */
public abstract class FileUploader implements BackgroundTask {
    /**
     * The editor window saving the file
     */
    protected FileEditorWindow editorWindow;
    /**
     * The path to save the file to
     */
    protected String filePath;
    /**
     * The contents of the file to upload
     */
    private final String savedFileContents;
    /**
     * Tracks if an exception/error occurred during saving
     */
    private boolean errorOccurred;
    /**
     * The upload service for uploading file contents
     */
    protected Service<Void> uploadService;
    /**
     * The connection to use to create temporary filesystem from and to upload the file if remote
     */
    protected FTPConnection uploadingConnection;
    /**
     * This returns true if there is an upload in progress for the specified FileEditorWindow
     */
    private static final HashMap<FileEditorWindow, Boolean> uploadInProgress = new HashMap<>();
    /**
     * Regex used to identify a backup file
     */
    protected static final String BACKUP_REGEX = "^.+([.]*~(([\\\\.])([0-9]+))*$)";

    /**
     * Constructs a file uploader
     * @param editorWindow the editor window saving the file
     * @param filePath the path to save the file to
     * @param savedFileContents the contents of the file to save
     */
    protected FileUploader(FileEditorWindow editorWindow, String filePath, String savedFileContents) {
        this.editorWindow = editorWindow;
        this.filePath = filePath;
        this.savedFileContents = savedFileContents;
        initUploadService();
    }

    /**
     * Initialises the underlying upload service
     */
    private void initUploadService() {
        uploadService = new Service<>() {
            @Override
            protected Task<Void> createTask() {
                return FileUploader.this.createTask();
            }
        };

        uploadService.setOnSucceeded(e -> doSucceed());
    }

    /**
     * A method to set the value of upload in progress for the specified editor window
     * @param editorWindow the editor to window to set
     * @param uploadInProgress the value for upload in progress
     */
    private static synchronized void setUploadInProgress(FileEditorWindow editorWindow, boolean uploadInProgress) {
        FileUploader.uploadInProgress.put(editorWindow, uploadInProgress);
    }

    /**
     * Defines what should happen when the upload succeeds
     */
    private void doSucceed() {
        UI.removeBackgroundTask(this);
        setUploadInProgress(editorWindow, false);

        if (!errorOccurred) {
            DirectoryPane directoryPane = editorWindow.getCreatingPane();
            String parentPath = new File(filePath).getParent();
            String windowsParent;
            parentPath = parentPath == null ? ((windowsParent = System.getenv("SystemDrive")) != null ? windowsParent:"/"):parentPath; // if windows, find the root
            final String finalParent = parentPath;

            editorWindow.setResetFileContents(savedFileContents); // we have now successfully saved the file contents. Our reset string should now match what is actually saved

            UI.doInfo("File Saved", "File " + filePath + " saved successfully");

            if (finalParent.equals(directoryPane.getDirectory().getFilePath())) {
                directoryPane.refresh();
            }
        }
    }

    /**
     * Starts this background task.
     * The preferred way to start it however is to call UploadScheduler.scheduleSave
     */
    @Override
    public void start() {
        uploadService.start();
        setUploadInProgress(editorWindow, true);
        UI.addBackgroundTask(this);
    }

    /**
     * Returns true if an upload is in progress for the specified editor window.
     * No uploads should be started for that editor window when this method returns true
     * @param editorWindow the editor window to check
     * @return true if an upload is in progress for that window, false if now
     */
    public static synchronized boolean isUploadInProgress(FileEditorWindow editorWindow) {
        Boolean upload = uploadInProgress.get(editorWindow);
        return upload != null ? upload:false;
    }

    /**
     * Constructs the task to execute
     * @return task
     */
    private Task<Void> createTask() {
        return new Task<>() {
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
    abstract FTPConnection getUploadingConnection() throws FTPException;

    /**
     * Gets the backup path
     * @param filePath the path to backup
     * @return backup path
     */
    abstract String getBackupPath(String filePath) throws Exception;

    /**
     * Writes a file backup
     * @param filePath the file path to back up
     * @param uploadingConnection the connection uploading the file
     * @return the backup path, null if failure occurs
     */
    abstract String writeBackup(String filePath, FTPConnection uploadingConnection);

    /**
     * Get the location of where the file should be saved. This is the destination for local files, the temp location for remote files
     * @param filePath the path to the file wanting to be saved
     * @return the path where the file should be saved
     */
    abstract String getSaveFilePath(String filePath);

    /**
     * Gets the FileSystem to use
     * @param connection the connection to initialise the file system with
     * @return the instance of the file system
     */
    abstract FileSystem getFileSystem(FTPConnection connection) throws FileSystemException;

    /**
     * Upload the file. For local file, all that needs to be done is clean up. If remote, this filePath is the temp path, The FileUploader.path is the destination
     * @param filePath the file path to upload.
     * @param backupPath the path to the backup that was created
     * @param fileSystem the file system being used
     */
    abstract void uploadFile(String filePath, String backupPath, FileSystem fileSystem) throws FileSystemException;

    /**
     * Saves the specified file contents in a file specified by filePath.
     */
    private void saveFile() {
        try {
            String saveFilePath = getSaveFilePath(filePath);
            FTPConnection uploadingConnection = getUploadingConnection();
            FileSystem fileSystem = getFileSystem(uploadingConnection);


            String backupPath = writeBackup(filePath, uploadingConnection);
            boolean backupWritten = backupPath != null;

            if (backupWritten) {
                writeToFile(saveFilePath);
                uploadFile(saveFilePath, backupPath, fileSystem);
            } else {
                Platform.runLater(() -> {
                    editorWindow.setSave(false);
                    UI.doError("Save Failed", "Failed to save file " + filePath + " as could not create backup");
                });
                errorOccurred = true;
            }
            // don't need to bother add file to local file system, because write to file would have already added it
        } catch (Exception ex) {
            Platform.runLater(() -> {
                editorWindow.setSave(false);
                UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
            });
            errorOccurred = true;
        }
    }

    /**
     * Creates a FileUploader for the file provided
     * @param editorWindow the editor window
     * @param filePath the path to the file
     * @param savedFileContents the saved file contents
     * @return the FileUploader instance
     */
    public static FileUploader newInstance(FileEditorWindow editorWindow, String filePath, String savedFileContents) {
        boolean local = editorWindow.getFile() instanceof LocalFile;
        if (local)
            return new LocalFileUploader(editorWindow, filePath, savedFileContents);
        else
            return new RemoteFileUploader(editorWindow, filePath, savedFileContents);
    }

    /**
     * Stops the background task and the underlying service
     */
    @Override
    public void cancel() {
        uploadService.cancel();
    }

    /**
     * Returns true if this task is running
     *
     * @return true if running, false if not
     */
    @Override
    public boolean isRunning() {
        return uploadService.isRunning();
    }

    /**
     * Returns the state of the upload service
     * @return the state of the upload service thread
     */
    public Worker.State getState() {
        return uploadService.getState();
    }
}
