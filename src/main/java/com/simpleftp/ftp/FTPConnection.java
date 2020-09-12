package com.simpleftp.ftp;

import com.simpleftp.ftp.exceptions.FTPConnectionFailedException;
import com.simpleftp.ftp.exceptions.FTPNotConnectedException;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

    //add methods to connect, disconnect, get files, add, remove etc

    /**
     * Connects to the FTP Server using the details specified in this object's FTPServer field
     * @return true if it was successful and false only and only if isConnected() returns true, other errors throw exceptions
     * @throws FTPConnectionFailedException if an error occurs during connection
     */
    public boolean connect() throws FTPConnectionFailedException {
        if (!connected) {
            String host = ftpServer.getServer();
            int port = ftpServer.getPort();

            try {
                log.info("Connecting the FTPConnection to the server");
                ftpClient.connect(host, port);
                connected = true;
                return true;
            } catch (IOException ex) {
                log.error("Failed to connect to FTP Server with hostname {}, port {} and user {}", host, port, ftpServer.getUser());
                throw new FTPConnectionFailedException("Failed to connect to FTP Server", ftpServer);
            }
        }

        log.info("FTPConnection already connected to the server, not about to re-connect");
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
            return ftpClient.login(user, ftpServer.getPassword());
        } catch (IOException ex) {
            log.error("Connection failed during login with user {}", user);
            throw new FTPConnectionFailedException("A connection error occurred during login", ex, ftpServer);
        }
    }

    //logout method here

    /**
     * Attempts to change the current directory to the directory specified by the path
     * @param path the path of the working directory to switch to
     * @return true if the operation was successful
     * @throws FTPNotConnectedException if this is attempted when isConnected() returns false
     * @throws FTPConnectionFailedException if an error occurs
     */
    public boolean changeWorkingDirectory(String path) throws FTPNotConnectedException, FTPConnectionFailedException {
        if (!connected) {
            log.error("Cannot change to directory {} as the FTPConnection is not connected", path);
            throw new FTPNotConnectedException("Cannot change directory as FTPConnection is not connected to the server", FTPNotConnectedException.ActionType.NAVIGATE);
        }

        try {
            log.info("Changing working directory to {}", path);
            return ftpClient.changeWorkingDirectory(path);
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
            log.info("Changing to the parent of the current working directory");
            return ftpClient.changeToParentDirectory();
        } catch (IOException ex) {
            log.error("An error occurred changing to parent of current working directory");
            throw new FTPConnectionFailedException("An error occurred changing to the parent of the current working directory", ex, ftpServer);
        }
    }

    /**
     * Attempts to retrieve the FTP File specified at the path
     * @param path the path to the file
     * @return a FTPFile object representing the specified path, or null if not found
     * @throws FTPNotConnectedException if isConnected() returns false when this is connected
     * @throws FTPConnectionFailedException if an error occurs
     */
    public FTPFile getFile(String path) throws FTPNotConnectedException, FTPConnectionFailedException {
        if (!isConnected()) {
            log.error("Cannot retrieve file specified by {} as the FTPConnection is not connected");
            throw new FTPNotConnectedException("FTPConnection is not connected to the server, so cannot retrieve file", FTPNotConnectedException.ActionType.DOWNLOAD);
        }

        try {
            log.info("Attempting tp retrieve file specified by path {}", path);
            return ftpClient.mlistFile(path);
        } catch (IOException ex) {
            log.error("An error occurred while retrieving the file specified by path {} from the server", path);
            throw new FTPConnectionFailedException("An error occurred retrieving the file from the server", ex, ftpServer);
        }
    }

    /**
     * Saves the file specified to the path on the server
     * @param file the standard Java IO file to add to the server
     * @param path the path to store the file in
     * @return true if the operation was successful
     * @throws FTPNotConnectedException if this is called when isConnected() returns false
     * @throws FTPConnectionFailedException if an error occurred
     */
    public boolean addFile(File file, String path) throws FTPNotConnectedException, FTPConnectionFailedException {
        String name = file.getName();

        if (!connected) {
            log.error("Cannot save file {} to {} as FTPConnection is not connected", name, path);
            throw new FTPNotConnectedException("FTPConnection is not connected to the sever, cannot add file to it", FTPNotConnectedException.ActionType.UPLOAD);
        }

        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            return ftpClient.storeFile(path, fileInputStream);
        } catch (IOException ex) {
            log.error("Cannot save file {} to {} as an error occurred", name, path);
            throw new FTPConnectionFailedException("An error occurred saving file to server", ex, ftpServer);
        }
    }

    //add append to file too

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
