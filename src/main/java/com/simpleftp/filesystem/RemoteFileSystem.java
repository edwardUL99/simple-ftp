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

package com.simpleftp.filesystem;

import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.util.Arrays;

/**
 * Represents a remote file system "linked" to a remote FTP Connection.
 *
 * For a RemoteFileSystem, it must be connected and logged in before use
 */
@Log4j2
public class RemoteFileSystem extends AbstractFileSystem {
    /**
     * Creates a "system-wide" file system for use with the system's connection.
     * This is the main constructor that you will want to call to create a file system for the main system panes etc.
     * @throws FileSystemException if an error occurs
     */
    public RemoteFileSystem() throws FileSystemException {
        super();
        FTPConnection ftpConnection = FTPSystem.getConnection();
        if (ftpConnection == null) {
            throw new FileSystemException("A FileSystem needs a FTPConnection object to function");
        }
        validateConnection(ftpConnection);
    }

    /**
     * Creates a "temporary" file system for use with a specified connection (useful for background tasks).
     * This file system should not be used for the main system, should only be used on a background task for one task
     * @param connection the connection to use
     * @throws FileSystemException if an error occurs
     */
    public RemoteFileSystem(FTPConnection connection) throws FileSystemException {
        super(connection);
        validateConnection(connection);

        ftpConnection = connection;
    }

    /**
     * Validates that the connection is connected and logged in
     * @param connection the connection to check
     * @throws FileSystemException if not connected and logged in
     */
    private void validateConnection(FTPConnection connection) throws FileSystemException {
        if (connection == null)
            throw new FileSystemException("The provided connection is null");

        if (!connection.isConnected() && !connection.isLoggedIn()) {
            throw new FileSystemException("The backing FTPConnection is not connected and logged in");
        }
    }

    /**
     * Add the specified file to the file system. This method can only add a single file to the filesystem.
     * To add multiple files, use copyFiles or moveFiles
     * @param file the representation of the file to add
     * @param path the path to the dir to add the file to on the system
     * @return true if it was a success, false if not
     * @throws FileSystemException if an error occurs or the file is not an instance of LocalFile
     */
    @Override
    public boolean addFile(CommonFile file, String path) throws FileSystemException {
        if (!(file instanceof LocalFile))
            throw new FileSystemException("Cannot add a file to the Remote File System that is already a RemoteFile. The mapping is Local File to Remote File System");

        try {
            return getFTPConnection().uploadFile((LocalFile)file, path) != null;
        } catch (FTPException ex) {
            throw new FileSystemException("A FTP Exception occurred when adding file to the remote file system", ex);
        }
    }

    /**
     * Recursively deletes the directory from the remote server
     * @param directory the directory to remove recursively
     * @param currentDirectory the current directory in the recursive step, null for initial step
     * @param connection the connection to use for removal
     * @throws FTPException if a FTP Exception occurs
     * @throws FileSystemException if an error occurs causing the deletion to fail
     */
    private void deleteDirectoryRecursively(String directory, String currentDirectory, FTPConnection connection) throws FTPException, FileSystemException {
        String listPath = directory;
        if (currentDirectory != null)
            listPath = FileUtils.appendPath(listPath, currentDirectory, false);

        FTPFile[] files = connection.listFiles(listPath);

        if (files != null && files.length > 0) {
            for (FTPFile file : files) {
                String name = file.getName();

                if (name.equals(".") || name.equals(".."))
                    continue;

                String filePath;
                if (currentDirectory == null) {
                    filePath = FileUtils.appendPath(directory, name, false);
                } else {
                    filePath = FileUtils.appendPath(FileUtils.appendPath(directory, currentDirectory, false), name, false);
                }

                if (file.isDirectory()) {
                    deleteDirectoryRecursively(listPath, name, connection);
                } else {
                    if (!connection.removeFile(filePath))
                        throw new FileSystemException("Failed to remove a file in the directory tree with path: " + filePath);
                }
            }

            if (!connection.removeDirectory(listPath))
                throw new FileSystemException("Failed to remove a directory in the directory tree with path: " + listPath);
        }
    }

    /**
     * Removes the specified file from the file system
     *
     * @param file the representation of the file to remove
     * @return true if it was a success, false if not
     * @throws FileSystemException if an error occurs or the file name is not an instance of RemoteFile
     */
    @Override
    public boolean removeFile(CommonFile file) throws FileSystemException {
        if (!(file instanceof RemoteFile))
            throw new FileSystemException("Cannot remove a Local File from a remote file system");

        String filePath = file.getFilePath();

        try {
            FTPConnection connection = getFTPConnection();

            if (file.isNormalFile() || file.isSymbolicLink()) { // if file is a symbolic link just delete the link, not the target directory
                return connection.removeFile(filePath);
            } else if (file.isADirectory()){
                deleteDirectoryRecursively(filePath, null, connection);
                return !connection.remotePathExists(filePath, true);
            } else {
                return false;
            }
        } catch (FTPException ex) {
            throw new FileSystemException("A FTP Exception occurred when removing the specified file", ex);
        }
    }

    /**
     * Attempts to find the specified file and returns it
     *
     * @param fileName the name/path to the file
     * @return the file if found, null if not
     * @throws FileSystemException if an error occurs
     */
    @Override
    public CommonFile getFile(String fileName) throws FileSystemException {
        try {
            FTPConnection connection = getFTPConnection();
            FTPFile ftpFile = connection.getFTPFile(fileName);
            if (ftpFile != null) {
                if (isTemporaryFileSystem())
                    return new RemoteFile(fileName, connection, ftpFile);
                else
                    return new RemoteFile(fileName, ftpFile);
            }

            return null;
        } catch (Exception ex) {
            throw new FileSystemException("A FTP Exception occurred when retrieving file", ex);
        }
    }

    /**
     * Checks if the specified file name exists file/dir
     *
     * @param fileName the name/path to the file
     * @return true if the file exists, false if not
     * @throws FileSystemException is an error occurs
     */
    @Override
    public boolean fileExists(String fileName) throws FileSystemException {
        return getFile(fileName) != null;
    }

    /**
     * List the files in the specified dir in the file system
     *
     * @param dir the directory path
     * @return an array of files if found, null if not
     * @throws FileSystemException if an error occurs
     */
    @Override
    public RemoteFile[] listFiles(String dir) throws FileSystemException {
        try {
            FTPConnection connection = getFTPConnection();

            FTPFile[] files = connection.listFiles(dir);
            if (files != null) {
                RemoteFile[] remoteFiles = new RemoteFile[files.length];
                boolean tempFileSystem = isTemporaryFileSystem();

                int i = 0;
                for (FTPFile f : files) {
                    if (!f.getName().equals(".") && !f.getName().equals("..")) {
                        String path = FileUtils.appendPath(dir, f.getName(), false);
                        if (tempFileSystem)
                            remoteFiles[i++] = new RemoteFile(path, connection, f);
                        else
                            remoteFiles[i++] = new RemoteFile(path, f); // not a temp file system, don't create a temp file
                    }
                }

                if (i < files.length)
                    remoteFiles = Arrays.copyOf(remoteFiles, i);

                return remoteFiles;
            } else {
                return null;
            }
        } catch (FTPException ex) {
            throw new FileSystemException("A FTP Exception occurred when listing files", ex);
        }
    }

    /**
     * This method determines the type of operation the source and destination parameters represent.
     * Checks that the parameters match the criteria for a remote file system,
     * throws an IllegalArgumentException if not met.
     * @param source the source file passed into copy/move
     * @param destination the destination file passed into copy/move
     * @return the enum value representing the copy/move type taking place
     */
    private CopyMoveOperation determineCopyMoveOperation(CommonFile source, CommonFile destination) {
        boolean sourceRemote = !source.isLocal(), sourceLocal = !sourceRemote;
        boolean destinationRemote = !destination.isLocal();

        if (sourceLocal && !destinationRemote)
            throw new IllegalArgumentException("The source file and destination file for RemoteFileSystem cannot both be instances of LocalFile");
        else if (!destinationRemote)
            throw new IllegalArgumentException("The destination file for RemoteFileSystem should be an instance of RemoteFile");

        if (sourceRemote)
            return CopyMoveOperation.REMOTE_TO_REMOTE;
        else
            return CopyMoveOperation.LOCAL_TO_REMOTE;
    }

    /**
     * Sets up the files of the directory to be deleted
     * @param directory the directory to delete
     */
    private void deleteTempCopyDirectory(LocalFile directory) {
        File[] contents = directory.listFiles();

        if (contents != null) {
            for (File file : contents) {
                deleteTempCopyDirectory(new LocalFile(file.getAbsolutePath()));
            }
        }

        if (!directory.delete())
            log.warn("Failed to delete a temp copy file from the local file system with path {}", directory.getFilePath());
    }

    /**
     * This method is used to copy files remote to remote. It needs to do a download to local temp folder
     * and then upload to the destination
     * @param source the file representing the source
     * @param destination the file representing the destination directory
     * @return true if successful, false if not
     * @throws FTPException if a FTP error occurs
     */
    private boolean copyRemoteToRemote(RemoteFile source, RemoteFile destination) throws FTPException, FileSystemException {
        String sourceName = source.getName();
        String destinationDir = destination.getFilePath();
        String destPath = FileUtils.appendPath(destinationDir, sourceName, false);
        FTPConnection connection = getFTPConnection();

        if (source.isADirectory()) {
            String localPath = FileUtils.TEMP_DIRECTORY + FileUtils.PATH_SEPARATOR + sourceName;
            LocalFileSystem.recursivelyDownloadDirectory(source.getFilePath(), FileUtils.TEMP_DIRECTORY, null, connection, this, true);

            LocalFile localFile = new LocalFile(localPath);
            if (!localFile.exists())
                throw new FileSystemException("Failed to local temp copy file");

            recursivelyUploadDirectory(localPath, destinationDir, null, connection, true);
            deleteTempCopyDirectory(localFile);
        } else {
            LocalFile downloaded = connection.downloadFile(source.getFilePath(), FileUtils.TEMP_DIRECTORY);

            if (downloaded == null || !downloaded.exists())
                throw new FileSystemException("Failed to local temp copy file");

            downloaded.deleteOnExit();

            if (connection.uploadFile(downloaded, destinationDir) == null)
                throw new FileSystemException("Failed to upload the local temp copy to the destination");
        }

        return isTemporaryFileSystem() ? new RemoteFile(destPath, connection, null).exists():new RemoteFile(destPath).exists();
    }

    /**
     * Performs the remote to remote copy/move of files
     * @param source the source file to copy/move
     * @param destination the destination folder to place the source into
     * @return true if successful, false if not
     * @throws FileSystemException if an exception occurs
     */
    private boolean remoteToRemoteOperation(RemoteFile source, RemoteFile destination, boolean copy) throws FileSystemException {
        String sourceName = source.getName();
        String destinationDir = destination.getFilePath();
        String destinationPath = FileUtils.appendPath(destinationDir, sourceName, false);

        String sourcePath = source.getFilePath();

        if (!source.exists())
            throw new FileSystemException("The source file " + sourcePath + " does not exist");

        if (!destination.isADirectory())
            throw new FileSystemException("The destination directory " + destinationDir + " is either not a directory or it does not exist");

        if (fileExists(destinationPath))
            return false;

        try {
            if (copy) {
                return copyRemoteToRemote(source, destination); // for a copy on a FTP server, you need to do a local temp copy and then re-upload to the destination
            } else {
                return getFTPConnection().renameFile(sourcePath, destinationPath); // for a move, rename is enough
            }
        } catch (FTPException ex) {
            throw new FileSystemException("Failed to " + (copy ? "copy" : "move") + " files: " + ex.getMessage(), ex);
        }
    }

    /**
     * Recursively uploads an entire directory to the destination directory path.
     * @param sourceDirectory the source directory to upload
     * @param destDirectory the destination directory to upload to
     * @param currentDirectory the current directory traversed, leave null for initial state
     * @param ftpConnection the connection to use
     * @param copy if false, the files will be deleted locally as they are copied
     * @throws FTPException if an error occurs related to the FTP connection
     */
    private void recursivelyUploadDirectory(String sourceDirectory, String destDirectory, String currentDirectory, FTPConnection ftpConnection, boolean copy) throws FTPException, FileSystemException {
        String listPath = sourceDirectory;
        if (currentDirectory != null)
            listPath = FileUtils.appendPath(listPath, currentDirectory, true); // this is a local path we want to append

        LocalFile listFile = new LocalFile(listPath);

        String destPath = FileUtils.appendPath(destDirectory, listFile.getName(), false);

        if (!fileExists(destPath))
            if (!ftpConnection.makeDirectory(destPath))
                throw new FileSystemException("Failed to create a directory in the upload directory structure, path: " + destPath + " " + ftpConnection.getReplyString());

        String[] fileNames = listFile.list();

        if (fileNames != null && fileNames.length > 0) {
            for (String name : fileNames) {
                String filePath;
                if (currentDirectory == null) {
                    filePath = FileUtils.appendPath(sourceDirectory, name, true);
                } else {
                    filePath = FileUtils.appendPath(FileUtils.appendPath(sourceDirectory, currentDirectory, true), name, true);
                }

                LocalFile file = new LocalFile(filePath);

                if (file.isADirectory()) {
                    recursivelyUploadDirectory(listPath, destPath, name, ftpConnection, copy);
                } else if (file.isFile()) {
                    if (ftpConnection.uploadFile(file, destPath) == null)
                        fileOperationErrors.add(new FileOperationError("Failed to upload file to destination", filePath, destPath));

                    if (!copy && !file.delete())
                        fileOperationErrors.add(new FileOperationError("Failed to remove file from local filesystem", filePath, null));
                }
            }

            if (!copy && !new LocalFile(listPath).delete())
                fileOperationErrors.add(new FileOperationError("Failed to remove file from local filesystem", listPath, null));
        }
    }

    /**
     * Performs local to remote copy/move of the source file
     * @param source the file representing the source file/directory
     * @param destination the file representing the destination directory
     * @param copy true if this operation is a copy, false if a move
     * @return true if successful, false if not
     * @throws FileSystemException if any failure occurs
     */
    private boolean localToRemoteOperation(LocalFile source, RemoteFile destination, boolean copy) throws FileSystemException {
        String sourceName = source.getName();
        String destinationDir = destination.getFilePath();
        String destinationPath = FileUtils.appendPath(destinationDir, sourceName, false);

        String sourcePath = source.getFilePath();

        if (!source.exists())
            throw new FileSystemException("The source file path " + sourcePath + " does not exist");

        if (!destination.exists() || !destination.isADirectory())
            throw new FileSystemException("The destination file " + destinationDir + " is either not a directory or it does not exist");

        if (fileExists(destinationPath))
            return false;

        boolean sourceDir = source.isADirectory();

        try {
            FTPConnection connection = getFTPConnection();

            if (sourceDir) {
                recursivelyUploadDirectory(sourcePath, destinationDir, null, connection, copy);
                return fileExists(destinationPath);
            } else {
                if (connection.uploadFile(source, destinationDir) != null) {
                    return copy || source.delete();
                } else {
                    return false;
                }
            }
        } catch (FTPException ex) {
            throw new FileSystemException("Failed to " + (copy ? "copy":"move") + " files: " + ex.getMessage(), ex);
        }
    }

    /**
     * This is the method which will implement the copy operation. It permits copying between different filesystems and also within the same filesystem.
     * The behaviour of the method depends on the implementation types of the source and destination parameters. If the types are both the same,
     * the copying will take place on the same file system those files are defined for.
     * If the types are different, it is for copying between different file systems, i.e. copying from the file defined for that file system to the file system defined for the
     * destination file.
     * If the types provided are in the wrong order, or both the same types but not the matching type for that file system, an IllegalArgumentException should be thrown.
     * <p>
     * A copy to another directory on the same remote file system requires a local copy. I.e. it requires a download of the file to a temp folder and then upload it to the destination
     * folder
     * <p>
     * An IllegalArgumentException can be thrown in the following conditions:
     * <ol>
     *     <li>If the implementing types of source and destination are the same but not LocalFile</li>
     *     <li>If the implementing types are different and source is not RemoteFile and destination is not LocalFile</li>
     * </ol>
     *
     * @param source      the file representing the file to copy. Can be a directory or a file
     * @param destination the file representing the <b>directory</b> the source will be copied to. If this file is not a directory, an IllegalArgumentException should be thrown
     * @return true if a success, false if not
     */
    @Override
    public boolean copyFiles(CommonFile source, CommonFile destination) throws FileSystemException {
        CopyMoveOperation copyMoveOperation = determineCopyMoveOperation(source, destination);
        if (copyMoveOperation == CopyMoveOperation.REMOTE_TO_REMOTE) {
            return remoteToRemoteOperation((RemoteFile)source, (RemoteFile)destination, true);
        } else if (copyMoveOperation == CopyMoveOperation.LOCAL_TO_REMOTE) {
            return localToRemoteOperation((LocalFile)source, (RemoteFile)destination, true);
        } else {
            throw new UnsupportedOperationException("Unsupported copy operation for RemoteFileSystem encountered: " + copyMoveOperation);
        }
    }

    /**
     * This is the method which will implement the move operation. It permits moving between different filesystems and also within the same filesystem.
     * The behaviour of the method depends on the implementation types of the source and destination parameters. If the types are both the same,
     * the moving will take place on the same file system those files are defined for.
     * If the types are different, it is for moving between different file systems, i.e. moving from the file defined for that file system to the file system defined for the
     * destination file.
     * If the types provided are in the wrong order, or both the same types but not the matching type for that file system, an IllegalArgumentException should be thrown.
     * <p>
     * A move from local to remote requires a upload and then deletion on the local file system of the source file. Remote to local requires a download and then deletion on
     * the remote file system of the source file.
     * <p>
     * An IllegalArgumentException can be thrown in the following conditions:
     * <ol>
     *     <li>If the implementing types of source and destination are the same but not LocalFile</li>
     *     <li>If the implementing types are different and source is not RemoteFile and destination is not LocalFile</li>
     * </ol>
     *
     * @param source      the file representing the file to move. Can be a directory or a file
     * @param destination the file representing the <b>directory</b> the source will be copied to. If this file is not a directory, an IllegalArgumentException should be thrown
     * @return true if a success, false if not
     */
    @Override
    public boolean moveFiles(CommonFile source, CommonFile destination) throws FileSystemException {
        CopyMoveOperation copyMoveOperation = determineCopyMoveOperation(source, destination);
        if (copyMoveOperation == CopyMoveOperation.REMOTE_TO_REMOTE) {
            return remoteToRemoteOperation((RemoteFile)source, (RemoteFile)destination, false);
        } else if (copyMoveOperation == CopyMoveOperation.LOCAL_TO_REMOTE) {
            return localToRemoteOperation((LocalFile)source, (RemoteFile)destination, false);
        } else {
            throw new UnsupportedOperationException("Unsupported move operation for RemoteFileSystem encountered: " + copyMoveOperation);
        }
    }
}
