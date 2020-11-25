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

import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.LocalFileSystem;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.RemoteFileSystem;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.panels.FilePanel;
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
     * Tracks if an exception/error occurred during saving
     */
    private boolean errorOccurred;

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

            if (!errorOccurred) {
                FilePanel filePanel = editorWindow.getCreatingPanel();
                String parentPath = new File(filePath).getParent();
                String windowsParent;
                parentPath = parentPath == null ? ((windowsParent = System.getProperty("SystemDrive")) != null ? windowsParent:"/"):parentPath; // if windows, find the root
                final String finalParent = parentPath;

                // only refresh if the file we're saving is in out current working directory
                Platform.runLater(() -> {
                    UI.doInfo("File Saved", "File " + filePath + " saved successfully");

                    if (finalParent.equals(filePanel.getDirectory().getFilePath())) {
                        filePanel.refresh();
                    }
                });
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
        FTPConnection uploadingConnection = FTPConnection.createTemporaryConnection(fileSystem.getFTPConnection());

        if (editorWindow.getFile() instanceof RemoteFile) {
            uploadingConnection.connect();
            uploadingConnection.login();
            uploadingConnection.setTextTransferMode(true);
        } // we don't need it to be connected for local
        return uploadingConnection;
    }

    /**
     * Writes the backup version (i.e. renames the filePath to filePath~. If filePath~ already exists, it is removed as some servers overwrite, others throw an error. If any failure occurs here, it returns false and saving should abort
     * @param filePath the filePath to backup
     * @return backup path if backed up successfully. If this returns null, you should abort saving
     */
    private String writeLocalBackup(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            String backupPath = getBackupPath(filePath, true);
            File backup = new File(backupPath);
            try {
                if (backup.exists()) {
                    if (!backup.delete())
                        return null;
                }

                if (backup.createNewFile()) {
                    return backupPath;
                } else {
                    return null;
                }
            } catch (IOException e) {
                return null;
            }
        }

        return filePath; // if it's been deleted, no need to make backup, just save it
    }

    /**
     * Gets the backup path
     * @param filePath the path to backup
     * @param local true if local, false if remote
     * @return backup path
     */
    private String getBackupPath(String filePath, boolean local) {
        String pathSeparator = local ? UI.PATH_SEPARATOR:"/";
        String backupPath;
        if (filePath.endsWith(pathSeparator)) {
            backupPath = filePath.substring(0, filePath.length() - 1);
            backupPath = backupPath + "~" + pathSeparator;
        } else {
            backupPath = filePath + "~";
        }

        return backupPath;
    }

    /**
     * Writes the backup version (i.e. renames the filePath to filePath~. If filePath~ already exists, it is removed as some servers overwrite, others throw an error. If any failure occurs here, it returns false and saving should abort
     * @param filePath the filePath to backup
     * @param uploadingConnection the connection being used to upload the file
     * @return the written backup path, null if an error occurs
     */
    private String writeRemoteBackup(String filePath, FTPConnection uploadingConnection) {
        try {
            if (uploadingConnection.remotePathExists(filePath)) {
                String backupPath = getBackupPath(filePath, false);
                if (uploadingConnection.remotePathExists(backupPath)) {
                    if (!uploadingConnection.removeFile(backupPath))
                        return null; // return false as we haven't removed our backup.
                }

                if (uploadingConnection.renameFile(filePath, backupPath)) {
                    return backupPath;
                } else {
                    return null;
                }
            }

            return filePath; // act like it has been backed-up but no need to create backup if it has been deleted before saving
        } catch (FTPException ex) {
            return null;
        }
    }

    /**
     * Saves the specified file contents in a file specified by filePath.
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

            String backupPath = remoteFileSystem ? writeRemoteBackup(filePath, uploadingConnection):writeLocalBackup(filePath);
            boolean backupWritten = backupPath != null;
            boolean deleteBackup = new LocalFile(backupPath).getName().endsWith("~"); // the backup may not have had to be created, so if the backup path is just the file, don't delete it

            if (backupWritten) {
                writeToFile(saveFilePath);

                if (remoteFileSystem) {
                    String parent = new File(filePath).getParent();
                    parent = parent == null ? "/" : parent;
                    fileSystem.addFile(new LocalFile(saveFilePath), parent);
                    file.delete();

                    if (uploadingConnection.remotePathExists(backupPath) && deleteBackup)
                        uploadingConnection.removeFile(backupPath); // after successful upload delete file
                } else {
                    if (deleteBackup)
                        new File(backupPath).delete();
                }
            } else {
                Platform.runLater(() -> {
                    editorWindow.setSave(false);
                    UI.doError("Save Failed", "Failed to save file " + filePath + " as could not create backup");
                });
                errorOccurred = true;
            }

            if (remoteFileSystem)
                uploadingConnection.disconnect(); // only need to disconnect if it was remote
            // don't need to bother add file to local file system, because write to file would have already added it
        } catch (Exception ex) {
            Platform.runLater(() -> {
                editorWindow.setSave(false);
                UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
            });
            errorOccurred = true;
        }
    }
}