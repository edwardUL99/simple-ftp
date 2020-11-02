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

import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.connection.FTPConnectionManager;
import com.simpleftp.ftp.connection.FTPConnectionManagerBuilder;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.security.PasswordEncryption;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.Properties;

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
 */
@AllArgsConstructor
public class LocalFileSystem implements FileSystem {
    private final FTPConnection ftpConnection;
    @Setter
    private FTPConnectionManager ftpConnectionManager;

    public LocalFileSystem() throws FileSystemException {
        this(new FTPConnectionManagerBuilder()
                .useSystemConnectionManager(true)
                .setBuiltManagerAsSystemManager(true)
                .build());
    }

    public LocalFileSystem(FTPConnectionManager connectionManager) throws FileSystemException {
        ftpConnectionManager = connectionManager;
        Properties properties = System.getProperties();
        if (!properties.containsKey("ftp-server") || !properties.containsKey("ftp-user") || !properties.containsKey("ftp-pass") || !properties.containsKey("ftp-port")) {
            throw new FileSystemException("Could not initialise the FTP Connection backing this LocalFileSystem");
        }
        ftpConnection = ftpConnectionManager.createReadyConnection(properties.getProperty("ftp-server"),
                properties.getProperty("ftp-user"),
                PasswordEncryption.decrypt(properties.getProperty("ftp-pass")),
                Integer.parseInt(properties.getProperty("ftp-port")));

        if (ftpConnection == null)
            throw new FileSystemException("Could not configure the FTP Server this File system is supposed to connect to, did you set the System properties ftp-server ftp-user ftp-pass and ftp-port?");
    }

    public LocalFileSystem(FTPConnection connection) throws FileSystemException {
        if (!connection.isConnected() && !connection.isLoggedIn()) {
            throw new FileSystemException("The provided connection must be connected and logged in");
        } else {
            ftpConnection = connection;
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
            return ftpConnection.downloadFile(file.getFilePath(), path).exists();
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

        if (!file.isDirectory()) {
            return null;
        } else {
            File[] files = file.listFiles();
            LocalFile[] localFiles = new LocalFile[files.length];

            int i = 0;
            for (File f : files) {
                localFiles[i++] = new LocalFile(f.getAbsolutePath());
            }

            return localFiles;
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
}
