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
import com.simpleftp.filesystem.RemoteFileSystem;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.editor.FileEditorWindow;

import java.io.File;

/**
 * This is for uploading remote files to a remote server
 */
final class RemoteFileUploader extends FileUploader {
    /**
     * Constructs a file uploader
     * @param editorWindow the editor window saving the file
     * @param filePath the path to save the file to
     * @param savedFileContents the contents of the file to save
     */
    protected RemoteFileUploader(FileEditorWindow editorWindow, String filePath, String savedFileContents) {
        super(editorWindow, filePath, savedFileContents);
    }

    /**
     * Gets a new connection with same details to upload the saved file
     *
     * @return the connection to upload the file with
     */
    @Override
    FTPConnection getUploadingConnection() throws FTPException {
        FileSystem fileSystem = editorWindow.getCreatingPane().getFileSystem();
        FTPConnection uploadingConnection = FTPConnection.createTemporaryConnection(fileSystem.getFTPConnection());
        uploadingConnection.connect();
        uploadingConnection.login();
        uploadingConnection.setTextTransferMode(true);

        return uploadingConnection;
    }

    /**
     * Writes a file backup
     *
     * @param filePath            the file path to back up
     * @param uploadingConnection the connection uploading the file
     * @return the backup path, null if failure occurs
     */
    @Override
    String writeBackup(String filePath, FTPConnection uploadingConnection) {
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
     * Get the location of where the file should be saved. This is the destination for local files, the temp location for remote files
     *
     * @param filePath the path to the file wanting to be saved
     * @return the path where the file should be saved
     */
    @Override
    String getSaveFilePath(String filePath) {
        return UI.TEMP_DIRECTORY + UI.PATH_SEPARATOR + new File(filePath).getName();
    }

    /**
     * Gets the FileSystem to use
     *
     * @param connection the connection to initialise the file system with
     * @return the instance of the file system
     */
    @Override
    FileSystem getFileSystem(FTPConnection connection) throws FileSystemException {
        return new RemoteFileSystem(connection);
    }

    /**
     * Upload the file. For local file, all that needs to be done is clean up. If remote, this filePath is the temp path, The FileUploader.path is the destination
     *
     * @param filePath   the file path to upload.
     * @param backupPath the path to the backup that was created
     * @param fileSystem the file system being used. This method uses it's FTPConnection
     */
    @Override
    void uploadFile(String filePath, String backupPath, FileSystem fileSystem) throws FileSystemException {
        String parent = new File(super.filePath).getParent();
        parent = parent == null ? "/" : parent;
        LocalFile tempFile = new LocalFile(filePath);
        fileSystem.addFile(tempFile, parent);
        tempFile.delete();

        boolean deleteBackup = new LocalFile(backupPath).getName().endsWith("~"); // the backup may not have had to be created, so if the backup path is just the file, don't delete it

        try {
            FTPConnection uploadingConnection = fileSystem.getFTPConnection();
            if (uploadingConnection.remotePathExists(backupPath) && deleteBackup)
                uploadingConnection.removeFile(backupPath); // after successful upload delete file

            uploadingConnection.disconnect();
        } catch (FTPException ex) {
            throw new FileSystemException("An error occurred uploading file", ex);
        }
    }
}
