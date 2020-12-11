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
import com.simpleftp.filesystem.LocalFileSystem;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.editor.FileEditorWindow;

import java.io.File;
import java.io.IOException;

/**
 * This is for "uploading" local files, i.e. saving them to the file syste,
 */
final class LocalFileUploader extends FileUploader {
    /**
     * Constructs a file uploader
     * @param editorWindow the editor window saving the file
     * @param filePath the path to save the file to
     * @param savedFileContents the contents of the file to save
     */
    protected LocalFileUploader(FileEditorWindow editorWindow, String filePath, String savedFileContents) {
        super(editorWindow, filePath, savedFileContents);
    }

    /**
     * Gets a new connection with same details to upload the saved file
     *
     * @return the connection to upload the file with
     */
    @Override
    FTPConnection getUploadingConnection() {
        if (uploadingConnection == null) {
            FileSystem fileSystem = editorWindow.getCreatingPane().getFileSystem();
            uploadingConnection = FTPConnection.createTemporaryConnection(fileSystem.getFTPConnection());
        }

        return uploadingConnection;
    }

    /**
     * Gets the backup path
     *
     * @param filePath the path to backup
     * @return backup path
     */
    @Override
    String getBackupPath(String filePath) {
        String pathSeparator = UI.PATH_SEPARATOR;
        String backupPath = "";
        if (filePath.endsWith(pathSeparator)) {
            filePath = filePath.substring(0, filePath.length() - 1);
        }

        backupPath = filePath + "~";

        int index = 1;
        while (new File(backupPath).exists()) {
            backupPath = backupPath.substring(0, backupPath.length() - 1);
            backupPath = backupPath + "~." + index++;
        }

        return backupPath;
    }

    /**
     * Writes a file backup
     *
     * @param filePath            the file path to back up
     * @param uploadingConnection the connection uploading the file, not used here
     * @return the backup path, null if failure occurs
     */
    @Override
    String writeBackup(String filePath, FTPConnection uploadingConnection) {
        LocalFile file = new LocalFile(filePath);
        if (file.exists()) {
            String backupPath = getBackupPath(filePath);
            LocalFile backup = new LocalFile(backupPath);
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
     * Get the location of where the file should be saved. This is the destination for local files, the temp location for remote files
     *
     * @param filePath the path to the file wanting to be saved
     * @return the path where the file should be saved
     */
    @Override
    String getSaveFilePath(String filePath) {
        return filePath;
    }

    /**
     * Gets the FileSystem to use
     *
     * @param connection the connection to initialise the file system with
     * @return the instance of the file system
     */
    @Override
    FileSystem getFileSystem(FTPConnection connection) {
        return new LocalFileSystem(connection);
    }

    /**
     * Upload the file. For local file, all that needs to be done is clean up, as writeToFile is called by FileUploader class before doing rest. If remote, this filePath is the temp path, The FileUploader.path is the destination
     *
     * @param filePath   the file path to upload.
     * @param backupPath the path to the backup that was created
     * @param fileSystem the file system being used
     */
    @Override
    void uploadFile(String filePath, String backupPath, FileSystem fileSystem){
        boolean deleteBackup = new LocalFile(backupPath).getName().matches(BACKUP_REGEX);

        if (deleteBackup)
            new File(backupPath).delete();
    }
}
