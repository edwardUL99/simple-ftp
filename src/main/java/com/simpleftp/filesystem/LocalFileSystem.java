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

package com.simpleftp.filesystem;

import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a local file system "linked" to a remote FTP Connection
 */
public class LocalFileSystem implements FileSystem {
    /**
     * The connection backing this file system if not using system connection
     */
    private FTPConnection ftpConnection;

    /**
     * Creates a file system for use with shared files, i.e using system's connection
     */
    public LocalFileSystem() throws FileSystemException {
        FTPConnection ftpConnection = FTPSystem.getConnection();
        if (ftpConnection == null) {
            throw new FileSystemException("A FileSystem needs a FTPConnection object to function");
        }
    }

    /**
     * Creates a file system for use with a specified connection (useful for background tasks)
     * @param connection the connection to use
     */
    public LocalFileSystem(FTPConnection connection)  {
        ftpConnection = connection;
    }

    /**
     * Add the specified file to the file system. This method can only add a single file to the filesystem.
     * To add multiple files, use copyFiles or moveFiles
     * @param file the representation of the file to add
     * @return true if successful, false if not
     * @throws FileSystemException if an error occurs or file is not an instance of RemoteFile
     */
    @Override
    public boolean addFile(CommonFile file, String path) throws FileSystemException {
        if (!(file instanceof RemoteFile))
            throw new FileSystemException("Cannot download a file to the LocalFileSystem that already exists locally, the mapping is Remote File to Local File System");

        try {
            File downloaded = getFTPConnection().downloadFile(file.getFilePath(), path);
            return downloaded != null && downloaded.exists();
        } catch (FTPException ex) {
            throw new FileSystemException("A FTPException occurred when adding the specified file to the local file system", ex);
        }
    }

    /**
     * Recursively deletes the directory from the local file system
     * @param directory the directory to remove recursively
     * @throws FileSystemException if an error occurs causing the deletion to fail
     */
    private void deleteDirectoryRecursively(String directory) throws FileSystemException {
        LocalFile file = new LocalFile(directory);
        File[] listings = file.listFiles();
        if (listings != null) {
            for (File file1 : listings) {
                deleteDirectoryRecursively(file1.getAbsolutePath());
            }
        }

        if (!file.delete())
            throw new FileSystemException("A file in the directory tree failed to be deleted with path: " + directory);
    }

    /**
     * Removes the specified file from the local file system
     * @param file the representation of the file to remove
     * @return true if successful
     * @throws FileSystemException if the provided file is not an instance of LocalFile
     */
    @Override
    public boolean removeFile(CommonFile file) throws FileSystemException {
        if (!(file instanceof LocalFile))
            throw new FileSystemException("Cannot remove a remote file from the LocalFileSystem");

        if (!file.isSymbolicLink() && file.isADirectory()) { // if file is a symbolic link just delete the link, not the target directory
            deleteDirectoryRecursively(file.getFilePath());
            return !file.exists();
        } else {
            return ((LocalFile) file).delete();
        }
    }

    /* Removes the specified file from the local file system
     * @param fileName name of the file to remove
     * @return true if successful
     */
    @Override
    public boolean removeFile(String fileName) throws FileSystemException {
        return removeFile(new LocalFile(fileName));
    }

    /**
     * Returns the file specified by the file name if it exists, null otherwise
     * @param fileName the name/path to the file
     * @return file if it exists, null otherwise (LocalFile)
     */
    @Override
    public CommonFile getFile(String fileName) {
        LocalFile file = new LocalFile(fileName);

        if (file.exists())
            return file;
        else
            return null;
    }

    @Override
    public boolean fileExists(String fileName) {
        return new LocalFile(fileName).exists();
    }

    @Override
    public CommonFile[] listFiles(String dir) {
        File file = new File(dir);

        if (file.isDirectory()) {
            File[] files = file.listFiles();

            if (files != null) {
                LocalFile[] localFiles = new LocalFile[files.length];

                int i = 0;
                for (File f : files) {
                    localFiles[i++] = new LocalFile(f.getAbsolutePath());
                }

                return localFiles;
            }

        }
        return null;
    }

    /**
     * Returns the FTPConnection the file system is linked to.
     * A FTP connection is required for both local and remote file systems as local file system needs to be able to download from the ftp server
     *
     * @return the connection being used
     */
    @Override
    public FTPConnection getFTPConnection() {
        return ftpConnection == null ? FTPSystem.getConnection():ftpConnection;
    }

    /**
     * This method determines the type of operation the source and destination parameters represent.
     * Checks that the parameters match the criteria for a local file system,
     * throws an IllegalArgumentException if not met.
     * @param source the source file passed into copy/move
     * @param destination the destination file passed into copy/move
     * @return the enum value representing the copy/move type taking place
     */
    private CopyMoveOperation determineCopyMoveOperation(CommonFile source, CommonFile destination) {
        boolean sourceLocal = source.isLocal(), sourceRemote = !sourceLocal;
        boolean destinationLocal = destination.isLocal();

        if (sourceRemote && !destinationLocal)
            throw new IllegalArgumentException("The source file and destination file for LocalFileSystem cannot both be instances of RemoteFile");
        else if (!destinationLocal)
            throw new IllegalArgumentException("The destination file for LocalFileSystem should be an instance of LocalFile");

        if (sourceLocal)
            return CopyMoveOperation.LOCAL_TO_LOCAL;
        else
            return CopyMoveOperation.REMOTE_TO_LOCAL;
    }

    /**
     * Performs the local to local copy/move of files
     * @param source the source file to copy/move
     * @param destination the destination folder to place the source into
     * @return true if successful, false if not
     * @throws FileSystemException if an exception occurs
     */
    private boolean localToLocalOperation(LocalFile source, LocalFile destination, boolean copy) throws FileSystemException {
        String sourceName = source.getName();
        String destinationDir = destination.getAbsolutePath();
        String destinationPath = destinationDir + (destinationDir.endsWith(FileUtils.PATH_SEPARATOR) ? "":FileUtils.PATH_SEPARATOR) + sourceName;

        if (fileExists(destinationPath))
            return false;

        Path sourcePath = source.toPath();
        Path destinationNioPath = destination.toPath();

        try {
            if (copy && source.isADirectory()) {
                final Path finalDestinationNioPath = destinationNioPath.resolve(sourcePath.getFileName());
                AtomicReference<IOException> thrownException = new AtomicReference<>(null);

                Files.walk(sourcePath, FileVisitOption.FOLLOW_LINKS)
                        .forEach(source1 -> {
                            try {
                                Path targetPath = finalDestinationNioPath.resolve(sourcePath.relativize(source1));
                                Files.copy(source1, targetPath);
                            } catch (IOException ex) {
                                thrownException.set(ex);
                            }
                        });

                IOException thrown = thrownException.get();
                if (thrown != null)
                    throw thrown;
            } else {
                if (copy) {
                    Files.copy(sourcePath, destinationNioPath.resolve(sourcePath.getFileName()));
                } else {
                    Files.move(sourcePath, destinationNioPath.resolve(sourcePath.getFileName()));
                }
            }

            return Files.exists(Path.of(destinationPath));
        } catch (IOException ex) {
            throw new FileSystemException("Failed to " + (copy ? "copy" : "move") + " files: " + ex.getMessage(), ex);
        }
    }

    /**
     * Recursively downloads an entire directory to the destination directory path.
     *
     * Package accessible as RemoteFileSystem remote to remote copy requires download of directories to temp folder
     * @param sourceDirectory the source directory to download
     * @param destDirectory the destination directory to download to
     * @param currentDirectory the current directory traversed, leave null for initial state
     * @param ftpConnection the connection to use
     * @param copy if false, the files will be deleted from the server as they are copied
     * @throws FTPException if an error occurs related to the FTP connection
     */
    static void recursivelyDownloadDirectory(String sourceDirectory, String destDirectory, String currentDirectory, FTPConnection ftpConnection, boolean copy) throws FTPException, FileSystemException {
        String listPath = sourceDirectory;
        if (currentDirectory != null)
            listPath += (listPath.endsWith("/") ? "":"/") + currentDirectory;

        String destPath = destDirectory + FileUtils.PATH_SEPARATOR + RemoteFile.getName(listPath);

        LocalFile file = new LocalFile(destPath);
        if (!file.exists())
            if (!file.mkdir())
                throw new FileSystemException("Failed to create a directory in the download directory structure, path: " + destPath);

        FTPFile[] subFiles = ftpConnection.listFiles(listPath);

        if (subFiles != null && subFiles.length > 0) {
            for (FTPFile file1 : subFiles) {
                String currName = file1.getName();

                if (currName.equals(".") || currName.equals(".."))
                    continue;

                String filePath;
                if (currentDirectory == null) {
                    filePath = sourceDirectory + "/" + currName;
                } else {
                    filePath = sourceDirectory + "/" + currentDirectory + "/" + currName;
                }

                RemoteFile file2 = new RemoteFile(filePath, ftpConnection, file1);
                boolean symLink = file1.isSymbolicLink();
                boolean directory = symLink ? file2.isADirectory():file1.isDirectory();
                boolean isFile = symLink ? file2.isNormalFile():file1.isFile();

                if (directory) {
                    recursivelyDownloadDirectory(listPath, destPath, currName, ftpConnection, copy);
                } else if (isFile) {
                    LocalFile downloaded = ftpConnection.downloadFile(filePath, destPath);
                    if (downloaded == null || !downloaded.exists())
                        throw new FileSystemException("Failed to download file: " + filePath + " with FTP Reply: " + ftpConnection.getReplyString());

                    if (!copy && !ftpConnection.removeFile(filePath))
                        throw new FileSystemException("Failed to remove file: " + filePath + " with FTP Reply: " + ftpConnection.getReplyString());
                }
            }

            if (!copy && !ftpConnection.removeDirectory(listPath))
                throw new FileSystemException("Failed to remove file: " + listPath + " with FTP Reply: " + ftpConnection.getReplyString());
        }
    }

    /**
     * Performs the remote to local copy/move of files
     * @param source the source file to copy/move
     * @param destination the destination folder to place the source into
     * @return true if successful, false if not
     * @throws FileSystemException if an exception occurs
     */
    private boolean remoteToLocalOperation(RemoteFile source, LocalFile destination, boolean copy) throws FileSystemException {
        String sourceName = source.getName();
        String destinationDir = destination.getAbsolutePath();
        String destinationPath = destinationDir + (destinationDir.endsWith(FileUtils.PATH_SEPARATOR) ? "":FileUtils.PATH_SEPARATOR) + sourceName;

        String sourcePath = source.getFilePath();

        if (!source.exists())
            throw new FileSystemException("The source file " + sourcePath + " does not exist");

        if (!destination.isADirectory())
            throw new FileSystemException("The destination file " + destinationDir + " is either not a directory or it doesn't exist");

        if (fileExists(destinationPath))
            return false;

        boolean sourceDir = source.isADirectory();

        try {
            FTPConnection ftpConnection = getFTPConnection();

            if (sourceDir) {
                recursivelyDownloadDirectory(sourcePath, destinationDir, null, ftpConnection, copy);
                return fileExists(destinationPath);
            } else {
                LocalFile downloaded = ftpConnection.downloadFile(sourcePath, destinationDir);
                if (downloaded != null && downloaded.exists()) {
                    return copy || ftpConnection.removeFile(sourcePath);
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
     * folder.
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
        if (copyMoveOperation == CopyMoveOperation.LOCAL_TO_LOCAL) {
            return localToLocalOperation((LocalFile)source, (LocalFile)destination, true);
        } else if (copyMoveOperation == CopyMoveOperation.REMOTE_TO_LOCAL) {
            return remoteToLocalOperation((RemoteFile)source, (LocalFile)destination, true);
        } else {
            throw new UnsupportedOperationException("Unsupported copy operation for LocalFileSystem encountered: " + copyMoveOperation);
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
     *
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
        if (copyMoveOperation == CopyMoveOperation.LOCAL_TO_LOCAL) {
            return localToLocalOperation((LocalFile)source, (LocalFile)destination, false);
        } else if (copyMoveOperation == CopyMoveOperation.REMOTE_TO_LOCAL) {
            return remoteToLocalOperation((RemoteFile)source, (LocalFile)destination, false);
        } else {
            throw new UnsupportedOperationException("Unsupported move operation for LocalFileSystem encountered: " + copyMoveOperation);
        }
    }
}
