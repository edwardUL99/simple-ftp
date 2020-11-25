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
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.*;
import com.simpleftp.ui.UI;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.util.Arrays;

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
 * FTPConnection.createSharedConnection should be called with a FTPServer object representing the above properties. It must also be connected and logged in before creating this class
 */
@AllArgsConstructor
public class RemoteFile implements CommonFile {
    private FTPConnection connection;
    @Getter
    private FTPFile ftpFile;
    private String absolutePath;
    /**
     * Tracks if this file exists. Calling the exists() method updates it
     */
    private boolean exists;

    /**
     * Constructs a remote file name with the specified file name
     * @param fileName the name of the file (absolute path preferred)
     */
    public RemoteFile(String fileName) throws FileSystemException {
        this.connection = FTPSystem.getConnection();
        if (this.connection == null) {
            throw new FileSystemException("Could not configure the FTP Server, did you set the System properties ftp-server ftp-user ftp-pass and ftp-port?");
        }

        validateConnection(this.connection);

        absolutePath = fileName;
        initialiseFTPFile(null);
    }

    /**
     * Creates a RemoteFile with the specified manager and ftp file. Uses fileName as path to search on FTPServer to initialise the FTPFile returned by getFtpFile
     * @param fileName the name of the file (absolute path preferred)
     * @param ftpConnection the connection to use
     * @param ftpFile the FTPFile to initialise this RemoteFile with. Leave null to force a lookup on FTP Server with fileName as path. This file may not exist. Assumed it does exist if you have the file
     * @throws FileSystemException
     */
    public RemoteFile(String fileName, FTPConnection ftpConnection, FTPFile ftpFile) throws FileSystemException {
        absolutePath = fileName;
        this.connection = ftpConnection;
        if (this.connection == null) {
            throw new FileSystemException("Could not configure the FTP Server, did you set the System properties ftp-server ftp-user ftp-pass and ftp-port?");
        }

        validateConnection(this.connection);
        initialiseFTPFile(ftpFile);
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
     * Initialises the FTPFile object
     * @param ftpFile the file to initialise with. Leave null if you want to force a lookup on the FTPServer using the filename provided
     */
    private void initialiseFTPFile(FTPFile ftpFile) throws FileSystemException{
       try {
           if (ftpFile == null) {
               this.ftpFile = this.connection.getFTPFile(absolutePath);

               exists = this.ftpFile != null;
           } else {
               this.ftpFile = ftpFile;
               exists = true;
           }

           if (!absolutePath.equals(".") && !absolutePath.equals("..") && exists)
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
                exists = false;

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
     * Checks if this file exists as either a directory or a normal file on the provided FTPConnection.
     * This syncs up the isADirectory, isNormalFile and getSize methods with the existsence of the file
     * @return true if exists, false if not
     * @throws FileSystemException if an error occurs
     */
    @Override
    public boolean exists() throws FileSystemException {
        try { // exists should always check up to date info
            String path = absolutePath;
            boolean oldExists = exists;
            exists = connection.remotePathExists(path, true) || connection.remotePathExists(path, false);

            if (!oldExists && exists) {
                // it didn't exist before, but does not, update the file
                try {
                    initialiseFTPFile(null);
                } catch (FileSystemException ex) {
                    ftpFile = null; // if file isn't found, leave null
                }
            }

            return exists;
        } catch (FTPException ex) {
            throw new FileSystemException("A FTP Exception occurred, it could not be determined if this file exists", ex);
        }
    }

    /**
     * Checks if this file is a directory.
     * May return true if the file no longer exists as this RemoteFile may not be an up to date version. Calling exists updates this information
     * @return true if a directory, false if not
     * @throws FileSystemException if an error occurs
     */
    @Override
    public boolean isADirectory() throws FileSystemException {
        if (!exists) {
            return false;
        } else if (ftpFile.isValid() && !ftpFile.isSymbolicLink()) { // if symbolic link, query the ftp server
            return ftpFile.isDirectory();
        } else {
            try {
                return connection.remotePathExists(absolutePath, true);
            } catch (FTPException ex) {
                throw new FileSystemException("A FTP Exception occurred, it could not be determined if this file is a directory", ex);
            }
        }
    }

    /**
     * Checks if this file is a normal file.
     * May return true, even if the file doesn't exist anymore, e.g. if the file is deleted after this RemoteFile is created. Calling exists() should be called if an update on the file is required or just general existence and if you don't need to know if it is a file or directory, calling exists updates this information
     * @return true if a directory, false if not
     * @throws FileSystemException if an error occurs
     */
    @Override
    public boolean isNormalFile() throws FileSystemException {
        if (!exists) {
            return false;
        } else if (ftpFile.isValid() && !ftpFile.isSymbolicLink()) { // if symbolic link, query the ftp server
            return ftpFile.isFile();
        } else {
            try {
                if (ftpFile.isSymbolicLink())                                       // make sure the link isn't broken
                    return connection.remotePathExists(absolutePath, false) && connection.remotePathExists(ftpFile.getLink()); // always explicitly check for remote path as ftpFile may be "." if the file is the same name as current directory
                else
                    return connection.remotePathExists(absolutePath, false);
            } catch (FTPException ex) {
                throw new FileSystemException("A FTP Exception occurred, it could not be determined if this file is a normal file", ex);
            }
        }
    }

    /**
     * Returns the size in bytes of the file
     * This may not be up to date, calling exists() updates the existence status if a file was removed after this RemoteFile was created.
     * @return size in bytes of the file
     */
    @Override
    public long getSize() throws FileSystemException {
        if (!exists) {
            return -1;
        } else if (ftpFile.isValid()) {
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


    @Override
    public int hashCode() {
        return ftpFile.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RemoteFile)) {
            return false;
        } else if (this == obj) {
            return true;
        } else if (this.hashCode() == obj.hashCode()) {
            return true;
        } else {
            RemoteFile remoteFile = (RemoteFile)obj;

            return this.getFilePath().equals(remoteFile.getFilePath());
        }
    }
}
