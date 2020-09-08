package com.simpleftp.ftp;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;
import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This is the main class used for provided a connection to a FTP server
 */
@AllArgsConstructor
@NoArgsConstructor
@With
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

    //add methods to connect, disconnect, get files etc

    /**
     * Attempts to login to the FTPServer using the details found in the passed in FTPServer object
     * @return login success
     * @throws IOException
     */
    public boolean login() throws IOException {
        return ftpClient.login(ftpServer.getUser(), ftpServer.getPassword());
    }

    /**
     * Retrieves the status of the FTP server this connection is connected to
     * @return the status of the FTP server this connection is connected to
     * @throws IOException
     */
    public String getStatus() throws IOException {
        return ftpClient.getStatus();
    }

    /**
     * Retrieves the file status of the file specified by the path
     * @param filePath the path of the file to query the status of
     * @return status for the specified file
     * @throws IOException
     */
    public String getFileStatus(String filePath) throws IOException {
        return ftpClient.getStatus(filePath);
    }

    /**
     * Gets the size of the file specified by the path
     * @param path the path to the file
     * @return the size of the file specified by the path on the server
     * @throws IOException
     */
    public String getFileSize(String path) throws IOException {
        return ftpClient.getSize(path);
    }

    /**
     * Returns the modification time for the file specified by the path
     * @param path the path of the file
     * @return last modified time of the file specified by path in the format DAY NAME, DAY MONTH NAME YEAR hh:mm:ss e.g. Tue, 3 Jun 2008 11:05:30 GMT
     * @throws IOException
     */
    public String getModificationTime(String path) throws IOException {
        String timestamp = ftpClient.getModificationTime(path);
        LocalDateTime dateTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("YYYYMMDDhhmmss"));
        String targetTime = dateTime.format(DateTimeFormatter.RFC_1123_DATE_TIME);
        return targetTime;
    }

    /**
     * Gets a FTPPathStats object for the specified path
     * This is the equivalent to calling getFileStatus(filePath), getFileSize(filePath) and getModificationTime(filePath) at once and return it as one object
     * @param filePath the path to query
     * @return a FTPPathStats object for the specified file path
     * @throws IOException
     */
    public FTPPathStats getPathStats(String filePath) throws IOException {
        return new FTPPathStats(filePath, getModificationTime(filePath), getFileStatus(filePath), getFileSize(filePath));
    }
}
