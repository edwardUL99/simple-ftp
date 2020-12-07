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
import com.simpleftp.ftp.exceptions.*;
import org.apache.commons.net.ftp.FTPFile;

import java.util.Arrays;

/**
 * Represents a remote file system "linked" to a remote FTP Connection.
 * This connection is configured by setting system properties (programatically at runtime preferably).
 * Set the following properties
 *        ftp-user=<username>
 *        ftp-pass=<password>
 *        ftp-server=<host>
 *        ftp-port=<port>
 *
 * Password is expected to be result of PasswordEncryption.encrypt()
 *
 * These should be set before calling this class by using System.setProperty.
 * The connection should be created by calling FTPConnection.createSharedConnection where the server object represents the above properties (i.e. FTPSystem.getPropertiesDefinedDetails())
 * For a RemoteFileSystem, it must be connected and logged in before use
 */
public class RemoteFileSystem implements FileSystem {
    private FTPConnection ftpConnection;

    public RemoteFileSystem() throws FileSystemException {
        ftpConnection = FTPSystem.getConnection();
        if (ftpConnection == null) {
            throw new FileSystemException("A FileSystem needs a FTPConnection object to function");
        }
        validateConnection(ftpConnection);
    }

    public RemoteFileSystem(FTPConnection connection) throws FileSystemException {
        ftpConnection = connection;
        validateConnection(ftpConnection);
    }

    /**
     * Validates that the connection is connected and logged in
     * @param connection the connection to check
     * @throws FileSystemException if not connected and logged in
     */
    private void validateConnection(FTPConnection connection) throws FileSystemException {
        if (!connection.isConnected() && !connection.isLoggedIn()) {
            throw new FileSystemException("The backing FTPConnection is not connected and logged in");
        }
    }

    /**
     * Add the specified file to the file system
     *
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
            return ftpConnection.uploadFile((LocalFile)file, path) != null;
        } catch (FTPException ex) {
            throw new FileSystemException("A FTP Exception occurred when adding file to the remote file system", ex);
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

        return removeFile(file.getFilePath());
    }

    /**
     * Removes the file specified by the path name
     *
     * @param fileName the name of the file (can be a path)
     * @return true if it was a success, false if not
     * @throws FileSystemException if an error occurs
     */
    @Override
    public boolean removeFile(String fileName) throws FileSystemException {
        try {
            RemoteFile file = (RemoteFile)getFile(fileName);
            if (file != null) {
                if (file.isNormalFile()) {
                    return ftpConnection.removeFile(fileName);
                } else {
                    return ftpConnection.removeDirectory(fileName);
                }
            }

            return false;
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
            FTPFile ftpFile = ftpConnection.getFTPFile(fileName);
            if (ftpFile != null) {
                return new RemoteFile(fileName, ftpConnection, ftpFile);
            }

            return null;
        } catch (FTPException ex) {
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
        try {
            return ftpConnection.remotePathExists(fileName);
        } catch (FTPException ex) {
            throw new FileSystemException("A FTP Exception occurred when checking if the remote file exists", ex);
        }
    }

    /**
     * List the files in the specified dir in the file system
     *
     * @param dir the directory path
     * @return an array of files if found, null if not
     * @throws FileSystemException if an error occurs
     */
    @Override
    public CommonFile[] listFiles(String dir) throws FileSystemException {
        try {
            FTPFile[] files = ftpConnection.listFiles(dir);
            if (files != null) {
                RemoteFile[] remoteFiles = new RemoteFile[files.length];

                int i = 0;
                for (FTPFile f : files) {
                    if (!f.getName().equals(".") && !f.getName().equals("..")) {
                        String path = dir.equals("/") || dir.endsWith("/") ? dir + f.getName() : dir + "/" + f.getName();
                        remoteFiles[i++] = new RemoteFile(path, ftpConnection, f);
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
     * Returns the FTPConnection the file system is linked to.
     * A FTP connection is required for both local and remote file systems as local file system needs to be able to download from the ftp server
     *
     * @return the connection being used
     */
    @Override
    public FTPConnection getFTPConnection() {
        return ftpConnection;
    }

    /**
     * Sets the connection for the file system
     *
     * @param connection the connection to set
     */
    @Override
    public void setFTPConnection(FTPConnection connection) {
        this.ftpConnection = connection;
    }
}
