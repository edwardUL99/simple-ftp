package com.simpleftp.ftp;

import com.simpleftp.ftp.exceptions.FTPConnectionFailedException;
import com.simpleftp.ftp.exceptions.FTPNotConnectedException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This is the main class used for provided a connection to a FTP server
 */
@AllArgsConstructor
@NoArgsConstructor
@With
@Slf4j
public class FTPConnection {
    /**
     * The FTP Client which provides the main FTP functionality
     */
    private FTPClient ftpClient;
    /**
     * The FTPServer object providing all the login details and server parameters
     */
    private FTPServer ftpServer;
    /**
     * Provides details to this connection like page size etc
     */
    private FTPConnectionDetails ftpConnectionDetails;
    /**
     * A boolean flag to indicate if this connection is actively connected or not
     */
    @Getter
    private boolean connected;

    //add methods to connect, disconnect, get files etc

    /**
     * Connects to the FTP Server using the details specified in this object's FTPServer field
     */
    public boolean connect() throws FTPConnectionFailedException {
        if (!connected) {
            String host = ftpServer.getServer();
            int port = ftpServer.getPort();

            try {
                ftpClient.connect(host, port);
                connected = true;
                return true;
            } catch (IOException ex) {
                log.error("Failed to connect to FTP Server with hostname {}, port {} and user {}", host, port, ftpServer.getUser());
                throw new FTPConnectionFailedException("Failed to connect to FTP Server", ftpServer);
            }
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
            return ftpClient.login(ftpServer.getUser(), ftpServer.getPassword());
        } catch (IOException ex) {
            log.error("Connection failed during login with user {}", user);
            throw new FTPConnectionFailedException("A connection error occurred during login", ex, ftpServer);
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
