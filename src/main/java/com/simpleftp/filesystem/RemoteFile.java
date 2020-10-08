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
import com.simpleftp.ftp.FTPConnection;
import com.simpleftp.ftp.FTPConnectionManager;
import com.simpleftp.ftp.exceptions.*;
import lombok.AllArgsConstructor;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;

import java.util.Properties;

/**
 * Represents a remote file associated with the provided FTPConnection instance
 * Attempts to connect to a FTP server using details from system properties:
 *      ftp-user=<username>
 *      ftp-pass=<password>
 *      ftp-server=<host>
 *      ftp-port=<port>
 *
 * These should be set before calling this class by using System.setProperty
 * Can be set from command line too using -Dproperty=value but safer programatically as it's not secure passing in password on cli
 */
@AllArgsConstructor
public class RemoteFile extends FTPFile implements CommonFile {
    private FTPConnection connection;
    private FTPConnectionManager ftpConnectionManager;

    /**
     * Constructs a remote file name with the specified file name
     * @param fileName the name of the file
     */
    public RemoteFile(String fileName) throws FileSystemException {
        this(fileName, new FTPConnectionManager());
    }

    /**
     * Creates a RemoteFile with the specified connection manager to establish a connection
     * @param fileName the name of the file
     * @param connectionManager the connection manager
     */
    public RemoteFile(String fileName, FTPConnectionManager connectionManager) throws FileSystemException {
        super.setName(fileName);
        Properties properties = System.getProperties();
        ftpConnectionManager = connectionManager;
        this.connection = ftpConnectionManager.createReadyConnection(properties.getProperty("ftp-server"), properties.getProperty("ftp-user"), properties.getProperty("ftp-pass"), Integer.parseInt(properties.getProperty("ftp-port")));
        if (this.connection == null) {
            throw new FileSystemException("Could not configure the FTP Server, did you set the System properties ftp-server ftp-user ftp-pass and ftp-port?");
        }
    }

    private String getAbsoluteName() {
        return super.getName();
    }

    @Override
    public String getName() {
        String name = super.getName();
        int index = name.indexOf("/");

        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }

        while (index != -1 && index != name.length() - 1) {
            index = name.indexOf("/");
            name = name.substring(index + 1);
        }

        return name;
    }

    @Override
    public String getFilePath() {
        return super.getName();
    }

    /**
     * Checks if this file exists as either a directory or a normal file on the provided FTPConnection
     * @return true if exists, false if not
     * @throws FileSystemException if an error occurs
     */
    @Override
    public boolean exists() throws FileSystemException {
        try {
            String path = getAbsoluteName();
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
            return connection.remotePathExists(getAbsoluteName(), true);
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
            return connection.remotePathExists(getAbsoluteName(), false);
        } catch (FTPException ex) {
            throw new FileSystemException("A FTP Exception occurred, it could not be determined if this file is a normal file", ex);
        }
    }
}
