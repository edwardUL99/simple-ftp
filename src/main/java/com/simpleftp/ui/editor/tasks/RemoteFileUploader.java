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
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.editor.FileEditorWindow;
import lombok.extern.log4j.Log4j2;

import java.io.File;

/**
 * This is for uploading remote files to a remote server
 */
@Log4j2
final class RemoteFileUploader extends FileUploader {
    /**
     * Constructs a file uploader
     * @param editorWindow the editor window saving the file
     * @param filePath the path to save the file to
     * @param savedFileContents the contents of the file to save
     */
    protected RemoteFileUploader(FileEditorWindow editorWindow, String filePath, String savedFileContents) {
        super(editorWindow, filePath, savedFileContents);
        uploadService.setOnCancelled(e -> disconnectConnection());
        uploadService.setOnFailed(e -> disconnectConnection());
    }

    /**
     * Disconnects the ftp connection if it was connected
     */
    private void disconnectConnection() {
        try {
            getUploadingConnection().disconnect();
        } catch (FTPException ex) {
            log.warn("Failed to disconnect a connection used to download file contents");
        }
    }

    /**
     * Gets a new connection with same details to upload the saved file
     *
     * @return the connection to upload the file with
     */
    @Override
    FTPConnection getUploadingConnection() throws FTPException {
        if (uploadingConnection == null) {
            FileSystem fileSystem = editorWindow.getCreatingPane().getFileSystem();
            uploadingConnection = FTPConnection.createTemporaryConnection(fileSystem.getFTPConnection());
        }

        if (!uploadingConnection.isConnected())
            uploadingConnection.connect();

        if (!uploadingConnection.isLoggedIn())
            uploadingConnection.login();

        uploadingConnection.setTextTransferMode(true);

        return uploadingConnection;
    }

    /**
     * Gets the backup path
     *
     * @param filePath the path to backup
     * @return backup path
     */
    @Override
    String getBackupPath(String filePath) throws Exception {
        String pathSeparator = "/";
        String backupPath;
        if (filePath.endsWith(pathSeparator)) {
            filePath = filePath.substring(0, filePath.length() - 1);
        }

        backupPath = filePath + "~";

        int index = 1;
        FTPConnection connection = getUploadingConnection();

        while (connection.remotePathExists(backupPath)) {
            backupPath = filePath + "~." + index++;
        }

        return backupPath;
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
            if (uploadingConnection.remotePathExists(filePath, false)) { // should always be a file
                String backupPath = getBackupPath(filePath);

                if (uploadingConnection.renameFile(filePath, backupPath)) {
                    return backupPath;
                } else {
                    System.out.println(uploadingConnection.getReplyString());
                    return null;
                }
            }

            return filePath; // act like it has been backed-up but no need to create backup if it has been deleted before saving
        } catch (Exception ex) {
            if (FTPSystem.isDebugEnabled())
                ex.printStackTrace();
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
        tempFile.deleteOnExit();

        boolean deleteBackup = new LocalFile(backupPath).getName().matches(BACKUP_REGEX); // the backup may not have had to be created, so if the backup path is just the file, don't delete it

        try {
            FTPConnection uploadingConnection = getUploadingConnection();
            if (uploadingConnection.remotePathExists(backupPath) && deleteBackup)
                uploadingConnection.removeFile(backupPath); // after successful upload delete file

            uploadingConnection.disconnect();
        } catch (FTPException ex) {
            throw new FileSystemException("An error occurred uploading file", ex);
        }
    }
}
