/*
 *  Copyright (C) 2020-2021 Edward Lynch-Milner
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

import com.simpleftp.filesystem.FileUtils;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.background.BackgroundTask;
import com.simpleftp.ui.background.scheduling.TaskScheduler;
import com.simpleftp.ui.directories.DirectoryPane;
import com.simpleftp.ui.editor.FileEditorWindow;
import com.simpleftp.ui.files.LineEntry;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * The service for uploading the text of a changed file
 * Abstract as uploads differ between file types. E.g local files aren't really uploaded, so their definition of the upload method would be just clean up.
 * This can only be instantiated by calling newInstance which will return the appropriate implementation for that file
 */
public abstract class FileUploader implements BackgroundTask {
    /**
     * The editor window saving the file
     */
    protected final FileEditorWindow editorWindow;
    /**
     * The path to save the file to
     */
    protected final String filePath;
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
     * A boolean to keep track of if this task is finished or not
     */
    private boolean finished;
    /**
     * Regex used to identify a backup file
     */
    protected static final String BACKUP_REGEX = "^.+([.]*~(([\\\\.])([0-9]+))*$)";
    /**
     * A scheduler for scheduling this uploader
     */
    private static final TaskScheduler<FileEditorWindow, FileUploader> scheduler = new TaskScheduler<>();

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
        uploadService.setOnFailed(e -> finished = true);
        uploadService.setOnCancelled(e -> finished = true);
    }

    /**
     * Defines what should happen when the upload succeeds
     */
    private void doSucceed() {
        finished = true;
        UI.removeBackgroundTask(this);

        if (!errorOccurred) {
            DirectoryPane directoryPane = editorWindow.getCreatingPane();
            String parentPath = FileUtils.getParentPath(filePath, directoryPane.isLocal());

            editorWindow.setResetFileContents(savedFileContents); // we have now successfully saved the file contents. Our reset string should now match what is actually saved

            UI.doInfo("File Saved", "File " + filePath + " saved successfully");

            if (parentPath.equals(directoryPane.getCurrentWorkingDirectory())) {
                try {
                    LineEntry lineEntry = editorWindow.getLineEntry();
                    if (directoryPane.filesDisplayed().getLineEntries().contains(lineEntry))
                        editorWindow.getLineEntry().refresh(); // just refresh the line entry. No point refreshing entire FilePanel if this LineEntry is still on it
                    else {
                        directoryPane.refreshCurrentDirectory(); // the directory pane no longer contains this line entry reference, so refresh file panel to refresh the new instance of this line entry
                    }
                } catch (FileSystemException ex) {
                    if (FTPSystem.isDebugEnabled())
                        ex.printStackTrace();
                }
            }
        }

        editorWindow.displaySavingLabel(false);
    }

    /**
     * Starts this background task.
     * The preferred way to start it however is to call UploadScheduler.scheduleSave
     */
    @Override
    public void start() {
        editorWindow.displaySavingLabel(true);
        uploadService.start();
        finished = false;
        UI.addBackgroundTask(this);
    }

    /**
     * Constructs the task to execute
     * @return task
     */
    private Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
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
     * Creates a FileUploader for the file provided
     * @param editorWindow the editor window
     * @param filePath the path to the file
     * @param savedFileContents the saved file contents
     * @return the FileUploader instance
     */
    public static FileUploader newInstance(FileEditorWindow editorWindow, String filePath, String savedFileContents) {
        boolean local = editorWindow.getLineEntry().isLocal();
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
     * Use this call to determine if a task is ready
     *
     * @return true if ready, false if not
     */
    @Override
    public boolean isReady() {
        return uploadService.getState() == Worker.State.READY;
    }

    /**
     * Returns the state of the upload service
     * @return the state of the upload service thread
     */
    public Worker.State getState() {
        return uploadService.getState();
    }

    /**
     * Schedule this uploader to be run by using its editor window as the key, i.e. if another file uploader is used with the same editor window,
     * run them one after another
     */
    @Override
    public void schedule() {
        scheduler.schedule(editorWindow, this);
    }
}
