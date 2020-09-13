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

package com.simpleftp.ftp;

import com.simpleftp.ftp.exceptions.FTPConnectionFailedException;
import com.simpleftp.ftp.exceptions.FTPError;
import com.simpleftp.ftp.exceptions.FTPNotConnectedException;
import com.simpleftp.ftp.exceptions.FTPRemotePathNotFoundException;
import com.simpleftp.local.exceptions.LocalPathNotFoundException;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamException;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This is the main class used for provided a client connection to a FTP server
 * It provides a subset of the features provided by org.apache.commons.net.ftp.FTPClient
 * This class also abstracts some calls of that library to hide any unnecessary details
 * It also doesn't include any functions that are not required by the system so not to complicate things
 *
 * The library also needs to be wrapped so that exceptions for this system can be thrown and certain constraints to be
 * put on the calls
 * Also required to provide logging
 */
@NoArgsConstructor
@AllArgsConstructor
@With
@Slf4j
public class FTPConnection {
    /**
     * The FTP Client which provides the main FTP functionality
     */
    protected FTPClient ftpClient;
    /**
     * The FTPServer object providing all the login details and server parameters
     */
    @Getter
    protected FTPServer ftpServer;
    /**
     * Provides details to this connection like page size etc
     */
    @Getter
    @Setter
    protected FTPConnectionDetails ftpConnectionDetails;
    /**
     * A boolean flag to indicate if this connection is actively connected or not
     */
    @Getter
    protected boolean connected;
    @Getter
    protected boolean loggedIn;

    //add methods to connect, disconnect, get files, add, remove etc

    /**
     * Connects to the FTP Server using the details specified in this object's FTPServer field
     * @return true if it was successful and false only and only if isConnected() returns true, other errors throw exceptions
     * @throws FTPConnectionFailedException if an error occurs during connection
     */
    public boolean connect() throws FTPConnectionFailedException {
        boolean connectionFailed = false;

        if (!connected) {
            String host = ftpServer.getServer();
            int port = ftpServer.getPort();

            try {
                log.info("Connecting the FTPConnection to the server");
                ftpClient.connect(host, port);

                if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                    log.info("FTPConnection successfully connected to the server");
                    connected = true;
                    return true;
                }

                connectionFailed = true;
                ftpClient.disconnect();
            } catch (IOException ex) {
                log.error("Failed to connect to FTP Server with hostname {}, port {} and user {}", host, port, ftpServer.getUser());
                throw new FTPConnectionFailedException("Failed to connect to FTP Server", ftpServer);
            }
        }

        if (!connectionFailed) {
            log.info("FTPConnection already connected to the server, not about to re-connect");
        } else {
            log.error("FTP Server refused connection, connection failed");
            throw new FTPConnectionFailedException("FTP Server refused connection to FTPConnection, failed to connect", ftpServer);
        }
        return false;
    }

    /**
     * Disconnects from the server, making this FTPConnection inactive
     * @throws FTPNotConnectedException if this operation is attempted when not connected to the server
     * @throws FTPConnectionFailedException if a connection error occurs when disconnecting from the server
     */
    public void disconnect() throws FTPNotConnectedException, FTPConnectionFailedException {
        if (!connected) {
            log.error("Attempted to disconnect from a connection that doesn't exist");
            throw new FTPNotConnectedException("Cannot disconnect from a server that wasn't connected to in the first place", FTPNotConnectedException.ActionType.DISCONNECT);
        }

        try {
            if (loggedIn) {
                log.info("A user is logged into the FTP server, logging out before disconnecting");
                logout();
            }
            ftpClient.disconnect();
            connected = false;
            log.info("FTPConnection is now disconnected from the server");
        } catch (IOException e) {
            log.error("A connection error occurred causing disconnect operation to fail");
            throw new FTPConnectionFailedException("A connection error occurred while disconnecting from the server", e, ftpServer);
        }
    }

    /**
     * Attempts to login to the FTPServer using the details found in the passed in FTPServer object
     * @return login success
     * @throws FTPNotConnectedException if login is called when isConnected() returns false
     * @throws FTPConnectionFailedException if a connection failure occurs during the login process
     */
    public boolean login() throws FTPNotConnectedException, FTPConnectionFailedException {
        String user = ftpServer.getUser();

        if (!connected) {
            log.error("FTPConnection is not connected to the server, cannot continue login process with user {}", user);
            throw new FTPNotConnectedException("Cannot login as the FTPConnection is not connected to the server", FTPNotConnectedException.ActionType.LOGIN);
        }

        try {
            log.info("Logging in to ftp server with user {}", user);
            if (!loggedIn) {
                loggedIn = ftpClient.login(user, ftpServer.getPassword());
                if (loggedIn) {
                    log.info("User {} logged into the ftp Server", user);
                    return true;
                }
            }

            return false;
        } catch (IOException ex) {
            log.error("Connection failed during login with user {}", user);
            throw new FTPConnectionFailedException("A connection error occurred during login", ex, ftpServer);
        }
    }

    /**
     * Attempts to log the user out of the FTP server
     * This method, unlike most of the others is guaranteed not to throw a FTPNotConnectedException for the following reasons:
     * <ul>
     *     <li>To login, in the first place, you must be connected to the server. If not, you will never be logged in and this will be a no-op</li>
     *     <li>If disconnect() is called and a user is logged in, the user is logged out. Thus, this method won't do anything, as it will again be a no-op</li>
     * </ul>
     *
     * However, the method is still configured to throw the exception JUST IN CASE, it ever happens that somehow, a login was processed without being connected, then you have to catch that
     * This is a bug if that exception is thrown though, so if it's ever come across, you need to raise a bug report. Included to support defensive programming however
     * @return true if logout was a success, false otherwise. If the user is not logged in, this is a no-op
     * @throws FTPNotConnectedException if isConnected() returns false but isLoggedIn() returns true (should never happen, if it does, a bug should be raised). If this is thrown, the method sets loggedIn to false, to try and resume with a normal state
     * @throws FTPConnectionFailedException if an error occurs when trying to logout
     */
    public boolean logout() throws FTPNotConnectedException, FTPConnectionFailedException {
        if (loggedIn) {
            if (!connected) {
                log.error("FTPConnection is not connected, cannot log out from server.\n\tRAISE A BUG REPORT FOR THIS, AS THIS SHOULD NOT HAPPEN.\n\t\tProgram logic is wrong somewhere");
                loggedIn = false; // set it to false, as loggedIn should not be true if connected is false
                throw new FTPNotConnectedException("FTPConnection is not connected to server, cannot logout", FTPNotConnectedException.ActionType.LOGIN);
            }

            try {
                loggedIn = !ftpClient.logout();
                log.info("Status of login to the server is {}", loggedIn);
                return !loggedIn;
            } catch (IOException ex) {
                log.error("An error occurred logging out from the server");
                throw new FTPConnectionFailedException("Error occurred logging out from server", ex, ftpServer);
            }
        }

        log.info("Cannot log out from ftp server as there is no user logged in");
        return false;
    }

    /**
     * Attempts to change the current directory to the directory specified by the path
     * login() must be called before running this method or else it will (intuitively as you cannot change a directory if not logged in) always return false
     * @param path the path of the working directory to switch to
     * @return true if the operation was successful, false if no
     * @throws FTPNotConnectedException if this is attempted when isConnected() returns false
     * @throws FTPConnectionFailedException if an error occurs
     */
    public boolean changeWorkingDirectory(String path) throws FTPNotConnectedException, FTPConnectionFailedException {
        if (!connected) {
            log.error("Cannot change to directory {} as the FTPConnection is not connected", path);
            throw new FTPNotConnectedException("Cannot change directory as FTPConnection is not connected to the server", FTPNotConnectedException.ActionType.NAVIGATE);
        }

        try {
            if (loggedIn) {
                log.info("Changing working directory to {}", path);
                return ftpClient.changeWorkingDirectory(path);
            }

            log.info("Aborting changing working directory to {} as user is not logged in", path);
            return false;
        } catch (IOException ex) {
            log.error("An error occurred when changing working directory to {}", path);
            throw new FTPConnectionFailedException("An error occurred changing working directory", ex, ftpServer);
        }
    }

    /**
     * Attempts to change to the parent directory of the current working directory
     * @return true if the operation was successful
     * @throws FTPNotConnectedException if this is attempted when isConnected() returns false
     * @throws FTPConnectionFailedException if an error occurs
     */
    public boolean changeToParentDirectory() throws FTPNotConnectedException, FTPConnectionFailedException {
        if (!connected) {
            log.error("Cannot change to parent directory as the FTPConnection is not connected");
            throw new FTPNotConnectedException("Cannot change to parent directory as FTPConnection is not connected to the server", FTPNotConnectedException.ActionType.NAVIGATE);
        }

        try {
            if (loggedIn) {
                log.info("Changing to the parent of the current working directory");
                return ftpClient.changeToParentDirectory();
            }

            log.info("User is not logged in, aborting changing to parent of working directory");
            return false;
        } catch (IOException ex) {
            log.error("An error occurred changing to parent of current working directory");
            throw new FTPConnectionFailedException("An error occurred changing to the parent of the current working directory", ex, ftpServer);
        }
    }

    private FTPFile retrieveFTPFile(String path) throws IOException {
        if (ftpClient.hasFeature("MLST")) {
            // this is the preferred command as it supports more precise timestamps and other features
            log.info("Using MLST command to retrieve file {}", path);
            return ftpClient.mlistFile(path);
        } else {
            // server doesn't support MLST, using LIST
            log.info("Using LIST command to attempt to retrieve the file {}", path);
            FTPFile[] file = ftpClient.listFiles(path);

            if (file != null && file.length > 0) {
                log.info("File retrieved successfully from server");
                return file[0];
            } else {
                log.info("File cannot be retrieved from server");
                return null;
            }
        }
    }

    /**
     * Attempts to retrieve the FTP File specified at the path
     * @param path the path to the file
     * @return a FTPFile object representing the specified path, or null if not found
     * @throws FTPNotConnectedException if isConnected() returns false when this is connected
     * @throws FTPConnectionFailedException if an error occurs
     */
    public FTPFile getFTPFile(String path) throws FTPNotConnectedException, FTPConnectionFailedException {
        if (!isConnected()) {
            log.error("Cannot retrieve file specified by {} as the FTPConnection is not connected", path);
            throw new FTPNotConnectedException("FTPConnection is not connected to the server, so cannot retrieve file", FTPNotConnectedException.ActionType.DOWNLOAD);
        }

        try {
            if (loggedIn) {
                return retrieveFTPFile(path);
            }

            log.info("User is not logged in, cannot get FTPFile");
            return null;
        } catch (IOException ex) {
            log.error("An error occurred while retrieving the file specified by path {} from the server", path);
            throw new FTPConnectionFailedException("An error occurred retrieving the file from the server", ex, ftpServer);
        }
    }

    private FTPFile writeLocalFileToRemote(File file, String path) throws IOException, FTPConnectionFailedException, FTPNotConnectedException {
        String name = file.getName();

        FileInputStream fileInputStream = new FileInputStream(file);
        boolean stored = ftpClient.storeFile(path, fileInputStream);

        if (stored) {
            log.info("File {} was uploaded successfully to {}", name, path);
            return getFTPFile(path);
        } else {
            log.info("File {} was not uploaded successfully to {}", name, path);
            return null;
        }
    }

    /**
     * Saves the file specified to the path on the server and returns the FTPFile representation of it
     * @param file the standard Java IO file to add to the server
     * @param path the path to store the file in
     * @return FTPFile representation of uploaded file, null if the file provided doesn't exist/is a directory, path doesn't exist or user not logged in
     * @throws FTPNotConnectedException if this is called when isConnected() returns false
     * @throws FTPConnectionFailedException if an error occurred
     */
    public FTPFile uploadFile(File file, String path) throws FTPNotConnectedException, FTPConnectionFailedException, FTPError {
        String name = file.getName();

        if (!file.exists() || file.isDirectory() || !remotePathExists(path, true) || !remotePathExists(path, false) || !loggedIn) {
            log.info("File does not exist or is a directory, or user is not logged in or the remote path doesn't exist, aborting upload");
            return null;
        }

        if (!connected) {
            log.error("Cannot save file {} to {} as FTPConnection is not connected", name, path);
            throw new FTPNotConnectedException("FTPConnection is not connected to the sever, cannot add file to it", FTPNotConnectedException.ActionType.UPLOAD);
        }

        try {
            return writeLocalFileToRemote(file, path);
        } catch (FileNotFoundException fn) {
            log.error("File does not exist, cannot upload file");
            throw new FTPError("An error occurred creating an input stream for the provided file", fn);
        } catch (CopyStreamException cs) {
            log.error("An error occurred transferring the file");
            throw new FTPError("An error occurred in file transmission", cs);
        } catch (IOException ex) {
            log.error("Cannot save file {} to {} as an error occurred", name, path);
            throw new FTPConnectionFailedException("An error occurred saving file to server", ex, ftpServer);
        }
    }

    /**
     * Overloaded version of uploadFile(java.io.File, java.lang.String) but converts localPath to file and calls the other method
     * @param localPath the path to the local file
     * @param path the path on the server to store the file in
     * @return the FTPFile representation of the uploaded file, will be null if local file doesn't exist/is a directory, path doesn't exist or user isn't logged in
     * @throws FTPNotConnectedException if called when isConnected() returns false
     * @throws FTPConnectionFailedException if an error occurs sending or receiving the upload command
     * @throws FTPError if an error occurs in transferring the file
     */
    public FTPFile uploadFile(String localPath, String path) throws FTPNotConnectedException, FTPConnectionFailedException, FTPError {
        return uploadFile(new File(localPath), path);
    }

    //add append to file too

    private boolean localFileExists(String localPath) {
        return new File(localPath).exists();
    }

    private String getFileBasename(FTPFile file) throws FTPError {
        String name = file.getName();

        if (name.contains("/")) {
            if (name.endsWith("/")) {
                name = name.substring(0, name.length() - 1);
            }

            int index = name.lastIndexOf("/");

            if (index == -1) // defensive programming
                throw new FTPError("Cannot determine the basename of the FTPFile. Is it a correct format? (i.e. path/to/file)");
            return name.substring(index + 1);
        } else {
            return name;
        }
    }

    private String addFileNameToLocalPath(String localPath, FTPFile file) throws FTPError {
        String separator = System.getProperty("path.separator");
        if (!localPath.endsWith(separator))
            localPath += separator;
        return localPath + getFileBasename(file);
    }

    private File writeRemoteFileToLocal(String remotePath, String localPath) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(new File(localPath));

        boolean retrieved = ftpClient.retrieveFile(remotePath, fileOutputStream);

        if (retrieved) {
            log.info("Retrieved file successfully from server");
            return new File(localPath); // the File object representing the file that was written to
        } else {
            log.info("Did not retrieve the file successfully from server");
            return null;
        }
    }

    /**
     * This method retrieves the file specified by the remotePath, downloads it to the path specified by localPath (excluding filename as it will use the same filename that's on the server)
     * It then returns a java IO File object representing the downloaded file
     * You need to be logged in, otherwise this will always return null
     * If either remotePath or localPath does not exist, this method will return null
     * @param remotePath the path to the remote file
     * @param localPath the path to where to save the remote file to locally (without filename)
     * @return a File object representing the local file that was downloaded
     * @throws FTPNotConnectedException if isConnected() returns false when called
     * @throws FTPConnectionFailedException if an error occurs retrieving the file
     * @throws FTPError if an error occurs determining if remotePath exists
     */
    public File downloadFile(String remotePath, String localPath) throws FTPNotConnectedException,
                                                                    FTPConnectionFailedException,
                                                                    FTPError {
        if (!connected) {
            log.error("FTPConnection is not connected to the server, cannot get file from remote path {}", remotePath);
            throw new FTPNotConnectedException("FTPConnection is not connected to the server, cannot download file", FTPNotConnectedException.ActionType.DOWNLOAD);
        }

        FTPFile remoteFile = getFTPFile(remotePath);

        if (remoteFile == null) {
            log.info("THe remote file {} does not exist", remotePath);
            return null;
        }
        localPath = addFileNameToLocalPath(localPath, remoteFile);

        if (remotePathExists(remotePath, true) || !localFileExists(localPath) || !loggedIn) {
            log.info("Either remote path {} or local path {} does not exist or remote path is a directory or the user is not logged in", remotePath, localPath);
            return null;
        } else {
            try {
                return writeRemoteFileToLocal(remotePath, localPath);
            } catch (FileNotFoundException ex) {
                log.error("A file not found exception error occurred with creating an output stream for {}", localPath);
                throw new FTPError("An output stream could not be created for local file", ex);
            } catch (CopyStreamException cs) {
                log.error("An error occurred in transferring the file {} from server to local {}", remotePath, localPath);
                throw new FTPError("An error occurred transferring the file from server to local machine", cs);
            } catch (IOException ex1) {
                log.error("An error occurred when downloading file {} from server to {}", remotePath, localPath);
                throw new FTPConnectionFailedException("An error occurred when downloading remote file to local path", ex1, ftpServer);
            }
        }
    }

    /**
     * Attempts to remove the file specified by the path from the FTP server
     * @param filePath the path to the file on the server
     * @return true if the operation was a success
     * @throws FTPNotConnectedException if isConnected() returns false when this operation is called
     * @throws FTPConnectionFailedException if an error occurs
     */
    public boolean removeFile(String filePath) throws FTPNotConnectedException, FTPConnectionFailedException {
        if (!connected) {
            log.error("Cannot remove file {} from the server as FTPConnection is not connected", filePath);
            throw new FTPNotConnectedException("FTPConnection not connected to server so cannot remove file", FTPNotConnectedException.ActionType.MODIFICATION);
        }

        try {
            log.info("Removing file {} from the server", filePath);
            return ftpClient.deleteFile(filePath);
        } catch (IOException ex) {
            log.error("An error occurred removing file {}", filePath);
            throw new FTPConnectionFailedException("An error occurred when removing file", ex, ftpServer);
        }
    }

    /**
     * Retrieves the status of the FTP server this connection is connected to
     * @return the status of the FTP server this connection is connected to
     * @throws FTPNotConnectedException if isConnected() returns false
     * @throws FTPConnectionFailedException if an error occurs retrieving the status
     */
    public String getStatus() throws FTPNotConnectedException, FTPConnectionFailedException {
        if (!connected) {
            log.error("Cannot retrieve status from server as the FTPConnection is not connected to the server");
            throw new FTPNotConnectedException("Failed retrieving status as FTPConnection is not connected to the server", FTPNotConnectedException.ActionType.STATUS_CHECK);
        }

        try {
            log.info("Retrieving server status");
            return ftpClient.getStatus();
        } catch (IOException ex) {
            log.error("A connection failure occurred when retrieving the server status");
            throw new FTPConnectionFailedException("Connection Failure when attempting to retrieve status from the server", ex, ftpServer);
        }
    }

    //implement get working directory

    /**
     * Checks if the specified remote path exists and returns the outcome
     * @param remotePath the remote path to check
     * @param dir true if this path is a directory
     * @return true if path exists, false otherwise
     * @throws FTPNotConnectedException if this is called when isConnected() returns true
     * @throws FTPConnectionFailedException if a connection error occurs
     * @throws FTPError if the existence of the remote path cannot be determined
     */
    public boolean remotePathExists(String remotePath, boolean dir) throws FTPNotConnectedException, FTPConnectionFailedException, FTPError {
        if (!connected) {
            log.error("FTPConnection not connected to the server, cannot check if path {} exists", remotePath);
            throw new FTPNotConnectedException("FTPConnection not connected to the server, cannot check if path exists", FTPNotConnectedException.ActionType.STATUS_CHECK);
        }

        try {
            log.info("Querying if remote path {} exists", remotePath);

            boolean remotePathExists;

            if (dir) {
                String currentWorkingDirectory = ftpClient.printWorkingDirectory();

                if (currentWorkingDirectory == null) {
                    log.error("Cannot determine the current working directory, cannot proceed with checking if directory exists");
                    throw new FTPError("Cannot determine if remotePath exists on this server");
                }

                remotePathExists = ftpClient.changeWorkingDirectory(remotePath);
                ftpClient.changeWorkingDirectory(currentWorkingDirectory); //return to the current directory
            } else {
                remotePathExists = ftpClient.listFiles(remotePath).length >= 1;
            }

            return remotePathExists;
        } catch (IOException ex) {
            log.error("An error occurred when checking if path {} exists", remotePath);
            throw new FTPConnectionFailedException("An error occurred when checking if path {} exists", ex, ftpServer);
        }
    }

    /**
     * Retrieves the file status of the file specified by the path
     * @param filePath the path of the file to query the status of
     * @return status for the specified file
     * @throws FTPNotConnectedException if isConnected() returns false
     * @throws FTPConnectionFailedException if a connection error occurred during retrieval of file status
     */
    public String getFileStatus(String filePath) throws FTPNotConnectedException, FTPConnectionFailedException {
        if (!connected) {
            log.error("FTPConnection is not connected to the server, failed to retrieve file status of path {}", filePath);
            throw new FTPNotConnectedException("FTPConnection is not connected to the server, cannot retrieve status for the file", FTPNotConnectedException.ActionType.STATUS_CHECK);
        }

        try {
            log.info("Retrieving file status of file with path {}", filePath);
            return ftpClient.getStatus(filePath);
        } catch (IOException ex) {
            log.error("A connection error occurred retrieving file status with path {}", filePath);
            throw new FTPConnectionFailedException("Connection error occurred retrieving status for file", ex, ftpServer);
        }
    }

    /**
     * Gets the size of the file specified by the path
     * @param path the path to the file
     * @return the size of the file specified by the path on the server
     * @throws FTPNotConnectedException if isConnected() returns false
     * @throws FTPConnectionFailedException if a connection error occurs retrieving the file size
     */
    public String getFileSize(String path) throws FTPNotConnectedException, FTPConnectionFailedException {
        if (!connected) {
            log.error("FTPConnection is not connected to the server, cannot retrieve file size of path {}", path);
            throw new FTPNotConnectedException("FTPConnection is not connected to the server, cannot retrieve file size", FTPNotConnectedException.ActionType.STATUS_CHECK);
        }

        try {
            log.info("Retrieving size for the file with path {}", path);
            return ftpClient.getSize(path);
        } catch (IOException ex) {
            log.error("A connection error occurred retrieving file size for path {}", path);
            throw new FTPConnectionFailedException("A connection error occurred retrieving file size", ex, ftpServer);
        }
    }

    /**
     * Returns the modification time for the file specified by the path
     * @param path the path of the file
     * @return last modified time of the file specified by path in the format DAY NAME, DAY MONTH NAME YEAR hh:mm:ss e.g. Tue, 3 Jun 2008 11:05:30 GMT
     * @throws FTPNotConnectedException if isConnected() returns false
     * @throws FTPConnectionFailedException if a connection error occurred retrieving modification time
     */
    public String getModificationTime(String path) throws FTPNotConnectedException, FTPConnectionFailedException {
        if (!connected) {
            log.error("FTPConnection is not connected to the server, cannot retrieve modification time for file with path {}", path);
            throw new FTPNotConnectedException("FTPConnection is not connected to server, cannot retrieve modification time", FTPNotConnectedException.ActionType.STATUS_CHECK);
        }

        try {
            log.info("Retrieving modification time for file with path {}", path);
            String timestamp = ftpClient.getModificationTime(path);
            LocalDateTime dateTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("YYYYMMDDhhmmss"));
            String targetTime = dateTime.format(DateTimeFormatter.RFC_1123_DATE_TIME);
            return targetTime;
        } catch (IOException ex) {
            log.error("A connection error occurred while retrieving modification time for file {}", path);
            throw new FTPConnectionFailedException("A connection error occurred while retrieving modification time", ex, ftpServer);
        }
    }

    /**
     * Gets a FTPPathStats object for the specified path
     * This is the equivalent to calling getFileStatus(filePath), getFileSize(filePath) and getModificationTime(filePath) at once and return it as one object
     * @param filePath the path to query
     * @return a FTPPathStats object for the specified file path
     * @throws FTPNotConnectedException if isConnected() returns false
     * @throws FTPConnectionFailedException if a connection error occurs
     */
    public FTPPathStats getPathStats(String filePath) throws FTPNotConnectedException, FTPConnectionFailedException {
        try {
            return new FTPPathStats(filePath, getModificationTime(filePath), getFileStatus(filePath), getFileSize(filePath));
        } catch (FTPNotConnectedException nc) {
            throw new FTPNotConnectedException("FTPConnection is not connected, so cannot retrieve FTPPathStats", nc, FTPNotConnectedException.ActionType.STATUS_CHECK);
        } catch (FTPConnectionFailedException cf) {
            throw new FTPConnectionFailedException("A connection error occurred, so cannot retrieve FTPPathStats", cf, ftpServer);
        }
    }
}
