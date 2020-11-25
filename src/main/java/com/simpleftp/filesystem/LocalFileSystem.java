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

import java.io.File;

/**
 * Represents a local file system "linked" to a remote FTP Connection.
 * This connection is configured by setting system properties (programatically at runtime preferably).
 * Set the following properties
 *        ftp-user=<username>
 *        ftp-pass=<password>
 *        ftp-server=<host>
 *        ftp-port=<port>
 *
 * Password is expected to be result of PasswordEncryption.encrypt()
 * These should be set before calling this class by using System.setProperty.
 * The connection should be created by calling FTPConnection.createSharedConnection where the server object represents the above properties (i.e. FTPSystem.getPropertiesDefinedDetails()) before creating this class
 * It only has to be connection and logged in for addFile in LocalFileSystem
 */
public class LocalFileSystem implements FileSystem {
    private final FTPConnection ftpConnection;

    public LocalFileSystem() throws FileSystemException {
        ftpConnection = FTPSystem.getConnection();
        if (ftpConnection == null) {
            throw new FileSystemException("The FTPSystem needs to have a connection for the FileSystem to use.");
        }
    }

    public LocalFileSystem(FTPConnection connection)  {
        ftpConnection = connection;
    }

    /**
     * Validates that the connection is connected and logged in
     * @param connection the connection to check
     * @throws FileSystemException if not connected and logged in
     */
    private void validateConnection(FTPConnection connection) throws FileSystemException {
        if (!connection.isConnected() && !connection.isLoggedIn()) {
            throw new FileSystemException("The backing FTPConnection needs to be logged in to download a file");
        }
    }

    /**
     * Adds the specified file to the local file system from the remote server
     * @param file the representation of the file to add
     * @return true if successful, false if not
     * @throws FileSystemException if an error occurs or file is not an instance of RemoteFile
     */
    @Override
    public boolean addFile(CommonFile file, String path) throws FileSystemException {
        if (!(file instanceof RemoteFile))
            throw new FileSystemException("Cannot download a file to the LocalFileSystem that already exists locally, the mapping is Remote File to Local File System");

        try {
            validateConnection(ftpConnection);
            File downloaded = ftpConnection.downloadFile(file.getFilePath(), path);
            return downloaded != null && downloaded.exists();
        } catch (FTPException ex) {
            throw new FileSystemException("A FTPException occurred when adding the specified file to the local file system", ex);
        }
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

        return ((LocalFile) file).delete();
    }

    /* Removes the specified file from the local file system
     * @param fileName name of the file to remove
     * @return true if successful
     */
    @Override
    public boolean removeFile(String fileName) {
        return new LocalFile(fileName).delete();
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
        return ftpConnection;
    }
}
