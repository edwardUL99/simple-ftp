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
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.connection.FTPConnectionManager;
import com.simpleftp.ftp.connection.FTPConnectionManagerBuilder;
import com.simpleftp.ftp.exceptions.*;
import com.simpleftp.security.PasswordEncryption;
import com.simpleftp.ui.UI;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

/**
 * Represents a remote file associated with the provided FTPConnection instance
 * Attempts to connect to a FTP server using details from system properties:
 *      ftp-user=<username>
 *      ftp-pass=<password>
 *      ftp-server=<host>
 *      ftp-port=<port>
 * Password is expected to be result of PasswordEncryption.encrypt()
 *
 * These should be set before calling this class by using System.setProperty
 * Can be set from command line too using -Dproperty=value but safer programatically as it's not secure passing in password on cli
 */
@AllArgsConstructor
public class RemoteFile implements CommonFile {
    private FTPConnection connection;
    private FTPConnectionManager ftpConnectionManager;
    @Getter
    private FTPFile ftpFile;
    private String absolutePath;

    /**
     * Constructs a remote file name with the specified file name
     * @param fileName the name of the file (absolute path preferred)
     */
    public RemoteFile(String fileName) throws FileSystemException {
        this(fileName, new FTPConnectionManagerBuilder()
                        .useSystemConnectionManager(true)
                        .setBuiltManagerAsSystemManager(true)
                        .build());
    }

    /**
     * Creates a RemoteFile with the specified connection manager to establish a connection. Uses fileName as path to search on FTPServer to initialise the FTPFile returned by getFtpFile
     * @param fileName the name of the file (absolute path preferred)
     * @param connectionManager the connection manager
     */
    public RemoteFile(String fileName, FTPConnectionManager connectionManager) throws FileSystemException {
        this(fileName, connectionManager, null); // make null to force the constructor to get the file
    }

    /**
     * Creates a RemoteFile with the specified manager and ftp file. Uses fileName as path to search on FTPServer to initialise the FTPFile returned by getFtpFile
     * @param fileName the name of the file (absolute path preferred)
     * @param connectionManager the connection manager managing connections
     * @param ftpFile the FTPFile to initialise this RemoteFile with. Leave null to force a lookup on FTP Server with fileName as path
     * @throws FileSystemException
     */
    public RemoteFile(String fileName, FTPConnectionManager connectionManager, FTPFile ftpFile) throws FileSystemException {
        absolutePath = fileName;
        Properties properties = System.getProperties();
        ftpConnectionManager = connectionManager;
        this.connection = ftpConnectionManager.createReadyConnection(properties.getProperty("ftp-server"), properties.getProperty("ftp-user"), PasswordEncryption.decrypt(properties.getProperty("ftp-pass")), Integer.parseInt(properties.getProperty("ftp-port")));
        if (this.connection == null) {
            throw new FileSystemException("Could not configure the FTP Server, did you set the System properties ftp-server ftp-user ftp-pass and ftp-port?");
        }

        initialiseFTPFile(ftpFile);
    }

    /**
     * Initialises the FTPFile object
     * @param ftpFile the file to initialise with. Leave null if you want to force a lookup on the FTPServer using the filename provided
     */
    private void initialiseFTPFile(FTPFile ftpFile) throws FileSystemException{
       try {
           if (ftpFile == null) {
               this.ftpFile = this.connection.getFTPFile(absolutePath);

               if (this.ftpFile == null)
                   throw new FTPRemotePathNotFoundException("The path specified does not exist", absolutePath);
           } else {
               this.ftpFile = ftpFile;
           }

           if (!absolutePath.equals(".") && !absolutePath.equals(".."))
               this.ftpFile = checkFileForCurrOrParentDir(this.ftpFile);
       } catch (FTPException ex) {
           throw new FileSystemException("Could not create RemoteFile due to a FTP Error", ex);
       }
    }

    /**
     * Checks if the file's name is . or .. (this can happen if a file's name is the same as current directory or .. (parent directory)
     * @param file the file to check
     * @return the correct file if the file is . or .., otherwise the passed in file
     */
    private FTPFile checkFileForCurrOrParentDir(FTPFile file) throws FTPException {
        String name = file.getName();

        if (name.equals(".") || name.equals("..")) {
            String parentPath = UI.getParentPath(absolutePath);
            if (new File(absolutePath).getParent() == null) {
                // we're at root
                FTPFile root = new FTPFile();
                root.setName(absolutePath);
                root.setSize(4096); // default directory size

                return root;
            }

            FTPFile[] files = connection.listFiles(parentPath);

            FTPFile result = Arrays.stream(files).filter(f -> f.getName().equals(getName()))
                                    .findFirst()
                                    .orElse(null);

            if (result == null)
                throw new FTPRemotePathNotFoundException("The path specified does not exist", absolutePath);

            return result;
        } else {
            return file;
        }
    }

    @Override
    public String getName() {
        String name = absolutePath;
        int index = name.indexOf("/");

        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }

        while (index != -1) {
            index = name.indexOf("/");
            name = name.substring(index + 1);
        }

        return name;
    }

    @Override
    public String getFilePath() {
        return absolutePath;
    }

    // the following three method don't check for existence from the ftpFile, as things may have changed since the ftpFile was populated

    /**
     * Checks if this file exists as either a directory or a normal file on the provided FTPConnection
     * @return true if exists, false if not
     * @throws FileSystemException if an error occurs
     */
    @Override
    public boolean exists() throws FileSystemException {
        try {
            String path = absolutePath;
            return connection.remotePathExists(path, true) || connection.remotePathExists(path, false);
        } catch (FTPException ex) {
            throw new FileSystemException("A FTP Exception occurred, it could not be determined if this file exists", ex);
        }
    }

    /**
     * Checks if this file is a directory
     * @return true if a directory, false if not
     * @throws FileSystemException if an error occurs
     */
    @Override
    public boolean isADirectory() throws FileSystemException {
        try {
            return connection.remotePathExists(absolutePath, true);
        } catch (FTPException ex) {
            throw new FileSystemException("A FTP Exception occurred, it could not be determined if this file is a directory", ex);
        }
    }

    /**
     * Checks if this file is a normal file
     * @return true if a directory, false if not
     * @throws FileSystemException if an error occurs
     */
    @Override
    public boolean isNormalFile() throws FileSystemException {
        try {
            if (ftpFile.isSymbolicLink())                                       // make sure the link isn't broken
                return connection.remotePathExists(absolutePath, false) && connection.remotePathExists(ftpFile.getLink()); // always explicitly check for remote path as ftpFile may be "." if the file is the same name as current directory
            else
                return connection.remotePathExists(absolutePath, false);
        } catch (FTPException ex) {
            throw new FileSystemException("A FTP Exception occurred, it could not be determined if this file is a normal file", ex);
        }
    }

    /**
     * Returns the size in bytes of the file
     *
     * @return size in bytes of the file
     */
    @Override
    public long getSize() throws FileSystemException {
        if (ftpFile.isValid()) {
            return ftpFile.getSize();
        } else {
            try {
                String size = connection.getFileSize(absolutePath);
                if (size != null) {
                    return Long.parseLong(size);
                }
            } catch (FTPException | NumberFormatException ex) {
                throw new FileSystemException("An error occurred retrieving size of file", ex);
            }
        }

        return -1;
    }
}
