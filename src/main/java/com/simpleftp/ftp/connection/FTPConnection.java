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

package com.simpleftp.ftp.connection;

import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.exceptions.*;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.net.ftp.*;
import org.apache.commons.net.io.CopyStreamException;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This is the main class used for provided a client connection to a FTP server
 * It provides a subset of the features provided by org.apache.commons.net.ftp.FTPClient
 * This class also abstracts some calls of that library to hide any unnecessary details
 * It also doesn't include any functions that are not required by the system so not to complicate things
 *
 * The library also needs to be wrapped so that exceptions for this system can be thrown and certain constraints to be
 * put on the calls
 * Also required to provide logging
 *
 * Note that if a FTPConnectionFailedException is thrown by any method of this class, it indicates a serious connection error occurred.
 * This means that this FTPConnection is no longer viable as isConnected() and isLoggedIn() will now return false.
 *
 *
 * Outside of the connection package, the only option to create a connection are by the createSharedConnection and createTemporaryConnection methods.
 * This is to enforce the purpose of the connection. I.e. for a connection to be used among filesystems etc., you want to share it. For temporary uploading.downloading for example, it's temporary
 */
@Log4j2
@EqualsAndHashCode(of = {"server", "ftpConnectionDetails", "connected", "loggedIn"})
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FTPConnection {
    /**
     * The FTP Client which provides the main FTP functionality
     */
    @Getter
    @Setter
    private FTPClient ftpClient;
    /**
     * The lookup object backing retrieval services in terms of information, not downloads
     */
    @Setter
    private FTPLookup ftpLookup;
    /**
     * The Server object providing all the login details and server parameters
     */
    @Getter
    @Setter
    private Server server;
    /**
     * Provides details to this connection like page size etc
     */
    @Getter
    @Setter
    private FTPConnectionDetails ftpConnectionDetails;
    /**
     * A boolean flag to indicate if this connection is actively connected or not
     */
    @Getter
    protected boolean connected;
    @Getter
    protected boolean loggedIn;

    private NoopDriver noopDriver;

    /**
     * Constructs a default object
     */
    protected FTPConnection() {
        ftpClient = new FTPClient();
        if (!FTPSystem.isSystemTesting()) // dont enable as if under test, hidden files causes issues
            ftpClient.setListHiddenFiles(true); // show hidden files and leave it up to UI to hide them or not
        ftpLookup = new FTPLookup(ftpClient);
        server = new Server();
        ftpConnectionDetails = new FTPConnectionDetails();
        setNoopDriver();
        setTimeoutTime();
    }

    /**
     * Constructs a connection object with the specified parameters.
     * Can be used if you want to specify a different FTPClient rather than a default one
     * @param ftpClient the client object to back this connection
     * @param server the object storing the server parameters
     * @param ftpConnectionDetails the object storing the connection details
     * @param ftpLookup the ftp lookup object to use
     */
    protected FTPConnection(FTPClient ftpClient, Server server, FTPConnectionDetails ftpConnectionDetails, FTPLookup ftpLookup) {
        this.ftpClient = ftpClient;
        if (!FTPSystem.isSystemTesting()) // dont enable as if under test, hidden files causes issues
            ftpClient.setListHiddenFiles(true); // show hidden files and leave it up to UI to hide them or not
        this.ftpLookup = ftpLookup;
        this.server = server;
        this.ftpConnectionDetails = ftpConnectionDetails == null ? new FTPConnectionDetails():ftpConnectionDetails;
        setNoopDriver();
        setTimeoutTime();
    }

    /**
     * Constructs a FTPConnection object with the specified ftp server details and connection details.
     * This is the constructor that guarantees the most correct functionality
     * @param server the object storing the server parameters
     * @param ftpConnectionDetails the object storing the connection details
     */
    protected FTPConnection(Server server, FTPConnectionDetails ftpConnectionDetails) {
        this.ftpClient = new FTPClient();
        if (!FTPSystem.isSystemTesting()) // dont enable as if under test, hidden files causes issues
            ftpClient.setListHiddenFiles(true); // show hidden files and leave it up to UI to hide them or not
        this.ftpLookup = new FTPLookup(ftpClient);
        this.server = server;
        this.ftpConnectionDetails = ftpConnectionDetails == null ? new FTPConnectionDetails():ftpConnectionDetails;
        setNoopDriver();
        setTimeoutTime();
    }

    private void logDebug(String message, Object... options) {
        if (FTPSystem.isDebugEnabled())
            log.debug(message, options);
    }

    private void setNoopDriver() {
        int timeout = ftpConnectionDetails.getTimeout();
        if (timeout == 0) {
            timeout = 300;
        }

        noopDriver = new NoopDriver(timeout);
    }

    /**
     * Connects to the FTP Server using the details specified in this object's Server field
     * @return true if it was successful and false only and only if isConnected() returns true, other errors throw exceptions
     * @throws FTPConnectionFailedException if an error occurs during connection
     */
    public boolean connect() throws FTPConnectionFailedException {
        boolean connectionFailed = false;

        if (!connected) {
            String host = server.getServer();
            int port = server.getPort();

            try {
                logDebug("Connecting the FTPConnection to the server");
                ftpClient.connect(host, port);

                if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                    log.info("FTPConnection successfully connected to the server");
                    connected = true;
                    ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                    return true;
                }

                connectionFailed = true;
                ftpClient.disconnect();
            } catch (IOException ex) {
                log.error("Failed to connect to FTP Server with hostname {}, port {} and user {}", host, port, server.getUser());
                resetConnectionValues();
                throw new FTPConnectionFailedException("Failed to connect to FTP Server", ftpClient.getReplyString(), server);
            }
        }

        if (!connectionFailed) {
            logDebug("FTPConnection already connected to the server, not about to re-connect");
        } else {
            log.error("FTP Server refused connection, connection failed");
            throw new FTPConnectionFailedException("FTP Server refused connection to FTPConnection, failed to connect", ftpClient.getReplyString(), server);
        }
        return false;
    }

    /**
     * To be only used in the case of FTPConnectionClosedException
     */
    private void resetConnectionValues() {
        connected = false;
        loggedIn = false;
    }

    /**
     * Disconnects from the server, making this FTPConnection inactive
     * @throws FTPNotConnectedException if this operation is attempted when not connected to the server
     * @throws FTPConnectionFailedException if the connection failed during disconnect
     * @throws FTPCommandFailedException if an error occurs when disconnecting from the server
     */
    public void disconnect() throws FTPNotConnectedException, FTPConnectionFailedException, FTPCommandFailedException {
        if (!connected) {
            log.error("Attempted to disconnect from a connection that doesn't exist");
            loggedIn = false;
            throw new FTPNotConnectedException("Cannot disconnect from a server that wasn't connected to in the first place", FTPNotConnectedException.ActionType.DISCONNECT);
        }

        try {
            if (loggedIn) {
                logDebug("A user is logged into the FTP server, logging out before disconnecting");
                logout();
                if (loggedIn)
                    loggedIn = false; //in case logout fails, enforce that loggedIn should be false
            }
            ftpClient.disconnect();
            connected = false;
            log.info("FTPConnection is now disconnected from the server");
        } catch (IOException e) {
            log.error("An error occurred causing disconnect operation to fail");
            throw new FTPCommandFailedException("An error occurred while disconnecting from the server", ftpClient.getReplyString(), e);
        }
    }

    /**
     * Attempts to login to the Server using the details found in the passed in Server object
     * @return login success
     * @throws FTPNotConnectedException if login is called when isConnected() returns false
     * @throws FTPConnectionFailedException if a connection failure occurs during the login process
     * @throws FTPCommandFailedException if an error occurs sending the login command or retrieving a reply
     */
    public boolean login() throws FTPNotConnectedException, FTPConnectionFailedException, FTPCommandFailedException {
        String user = server.getUser();

        if (!connected) {
            log.error("FTPConnection is not connected to the server, cannot continue login process with user {}", user);
            loggedIn = false;
            throw new FTPNotConnectedException("Cannot login as the FTPConnection is not connected to the server", FTPNotConnectedException.ActionType.LOGIN);
        }

        try {
            logDebug("Logging in to ftp server with user {}", user);
            if (!loggedIn) {
                loggedIn = ftpClient.login(user, server.getPassword());
                if (loggedIn) {
                    logDebug("User {} logged into the ftp Server", user);
                    if (!FTPSystem.isSystemTesting())
                        noopDriver.start();
                    return true;
                }
            }

            return false;
        } catch (FTPConnectionClosedException cl) {
            log.error("The FTPConnection unexpectedly closed, cannot login");
            resetConnectionValues();
            throw new FTPConnectionFailedException("The FTPConnection unexpectedly closed while logging in", ftpClient.getReplyString(), cl, server);
        } catch (IOException ex) {
            log.error("Error occurred during login with user {}", user);
            throw new FTPCommandFailedException("A connection error occurred during login", ftpClient.getReplyString(), ex);
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
     * @throws FTPConnectionFailedException if a connection error occurs when trying to logout
     * @throws FTPCommandFailedException if an error occurred sending the logout command or receiving a response from the server
     */
    public boolean logout() throws FTPNotConnectedException, FTPConnectionFailedException, FTPCommandFailedException {
        if (loggedIn) {
            if (!connected) {
                log.error("FTPConnection is not connected, cannot log out from server.\n\tRAISE A BUG REPORT FOR THIS, AS THIS SHOULD NOT HAPPEN.\n\t\tProgram logic is wrong somewhere");
                loggedIn = false; // set it to false, as loggedIn should not be true if connected is false
                throw new FTPNotConnectedException("FTPConnection is not connected to server, cannot logout", FTPNotConnectedException.ActionType.LOGIN);
            }

            try {
                loggedIn = !ftpClient.logout();
                logDebug("Status of login to the server is {}", loggedIn);
                if (!FTPSystem.isSystemTesting())
                    noopDriver.stop();
                return !loggedIn;
            } catch (FTPConnectionClosedException cl) {
                log.error("The FTPConnection unexpectedly closed while logging out");
                resetConnectionValues();
                throw new FTPConnectionFailedException("The FTPConnection unexpectedly closed while logging out", ftpClient.getReplyString(), cl, server);
            } catch (IOException ex) {
                log.error("An error occurred logging out from the server");
                throw new FTPCommandFailedException("Error occurred logging out from server", ftpClient.getReplyString(), ex);
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
     * @throws FTPConnectionFailedException if a connection error occurs
     * @throws FTPCommandFailedException if an error occurs sending the command or receiving a reply from the server
     */
    public boolean changeWorkingDirectory(String path) throws FTPNotConnectedException, FTPConnectionFailedException, FTPCommandFailedException {
        if (!connected) {
            log.error("Cannot change to directory {} as the FTPConnection is not connected", path);
            loggedIn = false;
            throw new FTPNotConnectedException("Cannot change directory as FTPConnection is not connected to the server", FTPNotConnectedException.ActionType.NAVIGATE);
        }

        try {
            if (loggedIn) {
                logDebug("Changing working directory to {}", path);
                return ftpClient.changeWorkingDirectory(path);
            }

            logDebug("Aborting changing working directory to {} as user is not logged in", path);
            return false;
        } catch (FTPConnectionClosedException cl) {
            log.error("The FTPConnection unexpectedly closed the connection when changing working directory");
            resetConnectionValues();
            throw new FTPConnectionFailedException("The FTPConnection unexpectedly closed while changing working directory", ftpClient.getReplyString(), cl, server);
        } catch (IOException ex) {
            log.error("An error occurred when changing working directory to {}", path);
            throw new FTPCommandFailedException("An error occurred changing working directory", ftpClient.getReplyString(), ex);
        }
    }

    /**
     * Attempts to change to the parent directory of the current working directory.
     * Expected to check if the path exists before calling this method
     * @return true if the operation was successful
     * @throws FTPNotConnectedException if this is attempted when isConnected() returns false
     * @throws FTPConnectionFailedException if a connection error occurs
     * @throws FTPCommandFailedException if an error occurs executing the command
     */
    public boolean changeToParentDirectory() throws FTPNotConnectedException, FTPConnectionFailedException, FTPCommandFailedException {
        if (!connected) {
            log.error("Cannot change to parent directory as the FTPConnection is not connected");
            loggedIn = false;
            throw new FTPNotConnectedException("Cannot change to parent directory as FTPConnection is not connected to the server", FTPNotConnectedException.ActionType.NAVIGATE);
        }

        try {
            if (loggedIn) {
                logDebug("Changing to the parent of the current working directory");
                return ftpClient.changeToParentDirectory();
            }

            logDebug("User is not logged in, aborting changing to parent of working directory");
            return false;
        } catch (FTPConnectionClosedException cl) {
            log.error("The FTPConnection unexpectedly closed the connection when changing to parent directory");
            resetConnectionValues();
            throw new FTPConnectionFailedException("The FTPConnection unexpectedly closed while changing to parent directory", ftpClient.getReplyString(), cl, server);
        } catch (IOException ex) {
            log.error("An error occurred changing to parent of current working directory");
            throw new FTPCommandFailedException("An error occurred changing to the parent of the current working directory", ftpClient.getReplyString(), ex);
        }
    }

    /**
     * Retrieves the current working directory on the FTP server
     * @return current ftp working directory, null if cannot be determined
     * @throws FTPNotConnectedException if isConnected() returns false when this is called
     * @throws FTPConnectionFailedException if a connection error occurs on execution of this command
     * @throws FTPCommandFailedException if an error occurs sending the command
     */
    public String getWorkingDirectory() throws FTPNotConnectedException, FTPConnectionFailedException, FTPCommandFailedException {
        if (!connected) {
            log.error("FTPConnection is not connected to the server, cannot retrieve current working directory");
            loggedIn = false;
            throw new FTPNotConnectedException("FTPConnection is not connected to the server, cannot retrieve current working directory", FTPNotConnectedException.ActionType.STATUS_CHECK);
        }

        try {
            if (loggedIn) {
                String workingDirectory = ftpLookup.getWorkingDirectory();
                logDebug("Retrieved current working directory as {}", workingDirectory);
                return workingDirectory;
            }

            logDebug("User is not logged in, cannot retrieve current working directory");
            return null;
        } catch (FTPConnectionClosedException cl) {
            log.error("The connection was unexpectedly closed when retrieving the current working directory");
            resetConnectionValues();
            throw new FTPConnectionFailedException("The FTPConnection unexpectedly closed the connection when retrieving current working directory", ftpClient.getReplyString(), cl, server);
        } catch (IOException ex) {
            log.error("An error occurred when retrieving the current working directory");
            throw new FTPCommandFailedException("An error occurred retrieving the current working directory", ftpClient.getReplyString(), ex);
        }
    }

    /**
     * Attempts to retrieve the FTP File specified at the path. If path is not found, it returns null
     * @param path the path to the file
     * @return a FTPFile object representing the specified path, or null if not found
     * @throws FTPNotConnectedException if isConnected() returns false when this is connected
     * @throws FTPConnectionFailedException if a connection error occurs
     * @throws FTPCommandFailedException if an error occurs executing the command
     */
    public FTPFile getFTPFile(String path) throws FTPNotConnectedException, FTPConnectionFailedException, FTPCommandFailedException {
        if (!connected) {
            log.error("Cannot retrieve file specified by {} as the FTPConnection is not connected", path);
            loggedIn = false;
            throw new FTPNotConnectedException("FTPConnection is not connected to the server, so cannot retrieve file", FTPNotConnectedException.ActionType.DOWNLOAD);
        }

        try {
            if (loggedIn) {
                logDebug("Retrieving FTPFile for path {} from server", path);
                return ftpLookup.getFTPFile(path);
            }

            logDebug("User is not logged in, cannot get FTPFile");
            return null;
        } catch (FTPConnectionClosedException cl) {
            log.error("FTPConnection unexpectedly closed the connection when retrieving FTP file");
            resetConnectionValues();
            throw new FTPConnectionFailedException("The FTPConnection unexpectedly closed while retrieving FTPFile", ftpClient.getReplyString(), cl, server);
        } catch (IOException ex) {
            log.error("An error occurred while retrieving the file specified by path {} from the server", path);
            throw new FTPCommandFailedException("An error occurred retrieving the file from the server", ftpClient.getReplyString(), ex);
        }
    }

    /**
     * Lists files in the given path
     * @param path the path to list
     * @return the array of retrieved files, null if path doesn't exist or not logged in
     * @throws FTPNotConnectedException if isConnected() returns false
     * @throws FTPConnectionFailedException if connection fails
     * @throws FTPCommandFailedException if an error occurs sending or receiving the command
     */
    public FTPFile[] listFiles(String path) throws FTPNotConnectedException, FTPConnectionFailedException, FTPCommandFailedException {
        if (!connected) {
            log.error("FTPConnection not connected to the server, cannot list files for path {}", path);
            loggedIn = false;
            throw new FTPNotConnectedException("FTPConnection not connected to the server, cannot list files", FTPNotConnectedException.ActionType.DOWNLOAD);
        }

        try {
            if (loggedIn) {
                return ftpLookup.listFTPFiles(path);
            }

            logDebug("User is not logged in, so cannot list files");
            return null;
        } catch (FTPConnectionClosedException cl) {
            log.error("FTPConnection unexpectedly closed the connection, cannot retrieve files");
            resetConnectionValues();
            throw new FTPConnectionFailedException("FTPConnection unexpectedly closed the connection, cannot retrieve files", ftpClient.getReplyString(), cl, server);
        } catch (IOException ex) {
            log.error("An error occurred when listing files");
            throw new FTPCommandFailedException("An error occurred when listing files", ftpClient.getReplyString(), ex);
        }
    }

    /**
     * Writes the specified file locally to remote path
     * @param file the local file
     * @param path the path to store the file on
     * @return the FTPFile representing the uploaded file
     * @throws IOException if an error occurs
     */
    private FTPFile writeLocalFileToRemote(File file, String path) throws IOException {
        String name = file.getName();
        String remoteFileName;
        if (path.equals("/")) {
            remoteFileName = path + name;
        } else {
            remoteFileName = path + "/" + name;
        }

        FileInputStream fileInputStream = new FileInputStream(file);
        boolean stored = ftpClient.storeFile(remoteFileName, fileInputStream);

        if (stored) {
            logDebug("File {} was uploaded successfully to {}", name, path);
            return ftpLookup.getFTPFile(remoteFileName);
        } else {
            logDebug("File {} was not uploaded successfully to {}", name, path);
            return null;
        }
    }

    /**
     * Saves the file specified to the path on the server and returns the FTPFile representation of it
     * @param file the standard Java IO file to add to the server
     * @param path the path to store the file in (directory)
     * @return FTPFile representation of uploaded file, null if the file provided doesn't exist/is a directory, path doesn't exist or user not logged in
     * @throws FTPNotConnectedException if this is called when isConnected() returns false
     * @throws FTPConnectionFailedException if an error occurred
     * @throws FTPError if an error occurs when uploading files
     * @throws FTPCommandFailedException if the upload failed
     */
    public FTPFile uploadFile(LocalFile file, String path) throws FTPNotConnectedException,
                                                             FTPConnectionFailedException,
                                                             FTPError,
                                                             FTPCommandFailedException {
        String name = file.getName();

        if (!connected) {
            log.error("Cannot save file {} to {} as FTPConnection is not connected", name, path);
            loggedIn = false;
            throw new FTPNotConnectedException("FTPConnection is not connected to the sever, cannot add file to it", FTPNotConnectedException.ActionType.UPLOAD);
        }

        try {
            if (!file.exists() || file.isDirectory() || !ftpLookup.remotePathExists(path, true)  || !loggedIn) {
                logDebug("Local file does not exist or is a directory, or user is not logged in or the remote path doesn't exist, aborting upload");
                return null;
            }

            return writeLocalFileToRemote(file, path);
        } catch (FTPConnectionClosedException cl) {
            log.error("FTPConnection unexpectedly closed the connection when uploading file");
            resetConnectionValues();
            throw new FTPConnectionFailedException("The FTPConnection unexpectedly closed while uploading file", ftpClient.getReplyString(), cl, server);
        } catch (FileNotFoundException fn) {
            log.error("File does not exist, cannot upload file");
            throw new FTPError("An error occurred creating an input stream for the provided file", ftpClient.getReplyString(), fn);
        } catch (CopyStreamException cs) {
            log.error("An error occurred transferring the file");
            throw new FTPError("An error occurred in file transmission", ftpClient.getReplyString(), cs);
        } catch (IOException ex) {
            log.error("Cannot save file {} to {} as an error occurred", name, path);
            throw new FTPCommandFailedException("An error occurred saving file to server", ftpClient.getReplyString(), ex);
        }
    }

    /**
     * Overloaded version of uploadFile(java.io.File, java.lang.String) but converts localPath to file and calls the other method
     * @param localPath the path to the local file
     * @param path the path on the server to store the file in
     * @return the FTPFile representation of the uploaded file, will be null if local file doesn't exist/is a directory, path doesn't exist or user isn't logged in
     * @throws FTPNotConnectedException if called when isConnected() returns false
     * @throws FTPConnectionFailedException if a connection error occurs
     * @throws FTPError if an error occurs in transferring the file
     * @throws FTPCommandFailedException if an error occurs executing the command
     */
    public FTPFile uploadFile(String localPath, String path) throws FTPNotConnectedException,
                                                                    FTPConnectionFailedException,
                                                                    FTPError,
                                                                    FTPCommandFailedException {
        return uploadFile(new LocalFile(localPath), path);
    }

    /**
     * Gets the basename of the file, i.e. name without path
     * @param file the file to get the name of
     * @return the name of the file
     * @throws FTPError if an unknown error occurs
     */
    private String getFileBasename(FTPFile file) throws FTPError {
        String name = file.getName();

        if (name.contains("/")) {
            if (name.endsWith("/")) {
                name = name.substring(0, name.length() - 1);
            }

            int index = name.lastIndexOf("/");

            if (index == -1) // defensive programming
                throw new FTPError("Cannot determine the basename of the FTPFile. Is it a correct format? (i.e. path/to/file)", ftpClient.getReplyString());
            return name.substring(index + 1);
        } else {
            return name;
        }
    }

    /**
     * Adds the name of the file to the local directory path
     * @param localPath the name of the directory the file will be stored in locally
     * @param file the file to store locally
     * @return the path to the local file that will be created
     * @throws FTPError if an error occurs
     */
    private String addFileNameToLocalPath(String localPath, FTPFile file) throws FTPError {
        String separator = System.getProperty("file.separator");
        if (!localPath.endsWith(separator))
            localPath += separator;
        return localPath + getFileBasename(file);
    }

    /**
     * Writes a remote file to the local path specified
     * @param remotePath the path of the remote file to write locally
     * @param localPath the path where to store the file locally
     * @return the file representing the written local file
     * @throws IOException if any FTP or IO exception occurs
     */
    private LocalFile writeRemoteFileToLocal(String remotePath, String localPath) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(new File(localPath));

        boolean retrieved = ftpClient.retrieveFile(remotePath, fileOutputStream);

        if (retrieved) {
            logDebug("Retrieved file successfully from server");
            return new LocalFile(localPath); // the File object representing the file that was written to
        } else {
            logDebug("Did not retrieve the file successfully from server");
            return null;
        }
    }

    /**
     * This method retrieves the file specified by the remotePath, downloads it to the path specified by localPath (excluding filename as it will use the same filename that's on the server)
     * It then returns a java IO File object representing the downloaded file
     * You need to be logged in, otherwise this will always return null
     * If either remotePath or localPath does not exist, this method will return null
     * @param remotePath the path to the remote file
     * @param localPath the path to where to save the remote file to locally (without filename, i.e. directory)
     * @return a File object representing the local file that was downloaded
     * @throws FTPNotConnectedException if isConnected() returns false when called
     * @throws FTPConnectionFailedException if a connection error occurs
     * @throws FTPError if an error occurs determining if remotePath exists
     * @throws FTPCommandFailedException if an error occurs sending the command
     */
    public LocalFile downloadFile(String remotePath, String localPath) throws FTPNotConnectedException,
                                                                    FTPConnectionFailedException,
                                                                    FTPError,
                                                                    FTPCommandFailedException {
        if (!connected) {
            log.error("FTPConnection is not connected to the server, cannot get file from remote path {}", remotePath);
            loggedIn = false;
            throw new FTPNotConnectedException("FTPConnection is not connected to the server, cannot download file", FTPNotConnectedException.ActionType.DOWNLOAD);
        }

        LocalFile local = new LocalFile(localPath);
        try {
            if (ftpLookup.remotePathExists(remotePath, true) || !local.isDirectory() || !loggedIn) {
                logDebug("Either remote path {} or local path {} does not exist or remote path is a directory or the user is not logged in", remotePath, localPath);
                return null;
            }

            FTPFile remoteFile = ftpLookup.getFTPFile(remotePath);

            if (remoteFile == null) {
                logDebug("The remote file {} does not exist", remotePath);
                return null;
            }

            localPath = addFileNameToLocalPath(localPath, remoteFile);

            return writeRemoteFileToLocal(remotePath, localPath);
        } catch (FTPConnectionClosedException cl) {
            log.error("FTPConnection unexpectedly closed the connection while downloading file");
            resetConnectionValues();
            throw new FTPConnectionFailedException("The FTPConnection unexpectedly closed while downloading the file", ftpClient.getReplyString(), cl, server);
        } catch (FileNotFoundException ex) {
            log.error("A file not found exception error occurred with creating an output stream for {}", localPath);
            throw new FTPError("An output stream could not be created for local file", ftpClient.getReplyString(), ex);
        } catch (CopyStreamException cs) {
            log.error("An error occurred in transferring the file {} from server to local {}", remotePath, localPath);
            throw new FTPError("An error occurred transferring the file from server to local machine", ftpClient.getReplyString(), cs);
        } catch (IOException ex1) {
            log.error("An error occurred when downloading file {} from server to {}", remotePath, localPath);
            throw new FTPCommandFailedException("An error occurred when downloading remote file to local path", ftpClient.getReplyString(), ex1);
        }
    }

    /**
     * Attempts to make a directory specified by the path provided. Expected in the format path/to/[directory-name] or as an abstract path
     * If the path already exists as either a directory or a file, this returns false.
     * @param path the path for the new directory
     * @return true if successful, false if not
     * @throws FTPNotConnectedException if called when isConnected() returns false
     * @throws FTPConnectionFailedException if a connection error occurs
     * @throws FTPCommandFailedException if an error occurs sending the command or receiving a reply
     * @throws FTPError if a general ftp error occurs
     */
    public boolean makeDirectory(String path) throws FTPNotConnectedException, FTPConnectionFailedException, FTPCommandFailedException, FTPError {
        if (!connected) {
            log.error("FTPConnection is not connected to the server, cannot make directory {}", path);
            loggedIn = false;
            throw new FTPNotConnectedException("FTPConnection is not connected to the server, cannot make directory", FTPNotConnectedException.ActionType.MODIFICATION);
        }

        try {
            if (loggedIn) {
                if (ftpLookup.remotePathExists(path, true) || ftpLookup.remotePathExists(path, false)) {
                    logDebug("Path {} exists as a directory already or a file", path);
                    return false;
                }

                logDebug("Creating directory {} on the server", path);
                return ftpClient.makeDirectory(path);
            }

            log.info("User is not logged into the server, cannot create a directory");
            return false;
        } catch (FTPConnectionClosedException cl) {
            log.error("FTPConnection unexpectedly closed the connection while making directory {}", path);
            resetConnectionValues();
            throw new FTPConnectionFailedException("FTPConnection unexpectedly closed the connection while making a directory", ftpClient.getReplyString(), cl, server);
        } catch (IOException ex) {
            log.error("An error occurred making directory {}", path);
            throw new FTPCommandFailedException("An error occurred sending the make directory command", ftpClient.getReplyString(), ex);
        }
    }

    /**
     * Renames the file specified by from to the name specified by to
     * @param from the old path/name (abstract)
     * @param to the new path/name (abstract)
     * @return true if successful, false if not
     * @throws FTPNotConnectedException if isConnected() returns false when this is called
     * @throws FTPConnectionFailedException if a connection error occurs
     * @throws FTPCommandFailedException if an error occurs sending or receiving a reply from the server
     */
    public boolean renameFile(String from, String to) throws FTPNotConnectedException, FTPConnectionFailedException, FTPCommandFailedException {
        if (!connected) {
            log.error("Cannot rename from {} to {}, as FTPConnection is not connected to the server", from, to);
            loggedIn = false;
            throw new FTPNotConnectedException("FTPConnection not connected to server, cannot rename file", FTPNotConnectedException.ActionType.MODIFICATION);
        }

        try {
            if (loggedIn) {
                logDebug("Renaming file from {} to {}", from, to);
                return ftpClient.rename(from, to);
            }

            logDebug("Cannot rename file from {} to {} as user is not logged into the server", from, to);
            return false;
        } catch (FTPConnectionClosedException cl) {
            log.error("The FTPConnection unexpectedly closed the connection when renaming file");
            resetConnectionValues();
            throw new FTPConnectionFailedException("The FTPConnection unexpectedly closed the connection when renaming file", ftpClient.getReplyString(), cl, server);
        } catch (IOException ex) {
            log.error("An error occurred renaming the file");
            throw new FTPCommandFailedException("An error occurred renaming the file", ftpClient.getReplyString(), ex);
        }
    }

    /**
     * Attempts to remove the file specified by the path from the FTP server
     * @param filePath the path to the file on the server
     * @return true if the operation was a success
     * @throws FTPNotConnectedException if isConnected() returns false when this operation is called
     * @throws FTPConnectionFailedException if a connection error occurs
     * @throws FTPCommandFailedException if an error occurs executing the command
     */
    public boolean removeFile(String filePath) throws FTPNotConnectedException, FTPConnectionFailedException, FTPCommandFailedException {
        if (!connected) {
            log.error("Cannot remove file {} from the server as FTPConnection is not connected", filePath);
            loggedIn = false;
            throw new FTPNotConnectedException("FTPConnection not connected to server so cannot remove file", FTPNotConnectedException.ActionType.MODIFICATION);
        }

        //may need to check if the file path exists here like above methods, or maybe its enough for IO exception to be thrown, research docs

        try {
            if (loggedIn) {
                logDebug("Removing file {} from the server", filePath);
                return ftpClient.deleteFile(filePath);
            }

            logDebug("Not removing file {} from server as user is not logged in", filePath);
            return false;
        } catch (FTPConnectionClosedException cl) {
            log.error("FTPConnection unexpectedly closed the connection while removing file");
            resetConnectionValues();
            throw new FTPConnectionFailedException("The FTPConnection unexpectedly closed while removing file", ftpClient.getReplyString(), cl, server);
        } catch (IOException ex) {
            log.error("An error occurred removing file {}", filePath);
            throw new FTPCommandFailedException("An error occurred when removing file", ftpClient.getReplyString(), ex);
        }
    }

    /**
     * Attempts to remove a directory if it is empty
     * @param path the path to the empty directory
     * @return true if successful, false if not
     * @throws FTPNotConnectedException if isConnected() returns false when this is called
     * @throws FTPConnectionFailedException if a connection error occurs when removing the directory
     * @throws FTPCommandFailedException if an error occurs sending the command or receiving a reply
     */
    public boolean removeDirectory(String path) throws FTPNotConnectedException, FTPConnectionFailedException, FTPCommandFailedException {
        if (!connected) {
            log.error("FTPConnection is not connected to the server, cannot remove directory {}", path);
            loggedIn = false;
            throw new FTPNotConnectedException("FTPConnection is not connected to the server, cannot remove directory", FTPNotConnectedException.ActionType.MODIFICATION);
        }

        try {
            if (loggedIn) {
                logDebug("Attempting to remove directory {}", path);
                return ftpClient.removeDirectory(path);
            }

            logDebug("User is not logged in, cannot remove directory {}", path);
            return false;
        } catch (FTPConnectionClosedException cl) {
            log.error("The FTPConnection unexpectedly closed the connection when removing directory");
            resetConnectionValues();
            throw new FTPConnectionFailedException("The FTPConnection unexpectedly closed the connection when removing directory", ftpClient.getReplyString(), cl, server);
        } catch (IOException ex) {
            log.error("An error occurred sending the command or receiving a reply from the server");
            throw new FTPCommandFailedException("An error occurred sending the command or receiving a reply from the server", ftpClient.getReplyString(), ex);
        }
    }

    /**
     * Checks if the specified remote path exists and returns the outcome
     * @param remotePath the remote path to check
     * @param dir true if this path is a directory
     * @return true if path exists, false otherwise
     * @throws FTPNotConnectedException if this is called when isConnected() returns true
     * @throws FTPConnectionFailedException if a connection error occurs
     * @throws FTPError if the existence of the remote path cannot be determined
     * @throws FTPCommandFailedException if an error occurs sending the command
     */
    public boolean remotePathExists(String remotePath, boolean dir) throws FTPNotConnectedException,
                                                                           FTPConnectionFailedException,
                                                                           FTPError,
                                                                           FTPCommandFailedException {
        if (!connected) {
            log.error("FTPConnection not connected to the server, cannot check if path {} exists", remotePath);
            loggedIn = false;
            throw new FTPNotConnectedException("FTPConnection not connected to the server, cannot check if path exists", FTPNotConnectedException.ActionType.STATUS_CHECK);
        }

        try {
            logDebug("Querying if remote path {} exists", remotePath);

            if (loggedIn) {
                return ftpLookup.remotePathExists(remotePath, dir);
            }

            logDebug("User is not logged in, cannot check if remote path exists");
            return false;
        } catch (FTPConnectionClosedException cl) {
            log.error("FTPConnection unexpectedly closed the connection while checking if path exists");
            resetConnectionValues();
            throw new FTPConnectionFailedException("The FTPConnection unexpectedly closed while checking if path exists", ftpClient.getReplyString(), cl, server);
        } catch (IOException ex) {
            log.error("An error occurred when checking if path {} exists", remotePath);
            throw new FTPCommandFailedException("An error occurred when checking if path exists", ftpClient.getReplyString(), ex);
        }
    }

    /**
     * Checks for general existence of a file and not specifically as a directory or file
     * @param remotePath the path to check
     * @return true if the path exists
     * @throws FTPNotConnectedException if this is called when isConnected returns false
     * @throws FTPConnectionFailedException if a connection error occurs
     * @throws FTPError if a general error occurs
     * @throws FTPCommandFailedException if an error occurs sending or receiving the command
     */
    public boolean remotePathExists(String remotePath) throws FTPNotConnectedException,
                                                              FTPConnectionFailedException,
                                                              FTPError,
                                                              FTPCommandFailedException {
        return remotePathExists(remotePath, true) || remotePathExists(remotePath, false);
    }

    /**
     * Retrieves the status of the FTP server this connection is connected to
     * @return the status of the FTP server this connection is connected to
     * @throws FTPNotConnectedException if isConnected() returns false
     * @throws FTPConnectionFailedException if a connection error occurs
     * @throws FTPCommandFailedException if an error occurs sending the command
     */
    public String getStatus() throws FTPNotConnectedException, FTPConnectionFailedException, FTPCommandFailedException {
        if (!connected) {
            log.error("Cannot retrieve status from server as the FTPConnection is not connected to the server");
            loggedIn = false;
            throw new FTPNotConnectedException("Failed retrieving status as FTPConnection is not connected to the server", FTPNotConnectedException.ActionType.STATUS_CHECK);
        }

        try {
            logDebug("Retrieving server status");
            if (loggedIn) {
                return ftpLookup.getStatus();
            }

            logDebug("User is not logged in, cannot check status");
            return null;
        } catch (FTPConnectionClosedException cl) {
            log.error("FTPConnection unexpectedly closed the connection while retrieving status");
            resetConnectionValues();
            throw new FTPConnectionFailedException("The FTPConnection unexpectedly closed while retrieving status", ftpClient.getReplyString(), cl, server);
        } catch (IOException ex) {
            log.error("An error occurred when retrieving the server status");
            throw new FTPCommandFailedException("An error occurred when attempting to retrieve status from the server", ftpClient.getReplyString(), ex);
        }
    }

    /**
     * Retrieves the file status of the file specified by the path
     * @param filePath the path of the file to query the status of
     * @return status for the specified file
     * @throws FTPNotConnectedException if isConnected() returns false
     * @throws FTPConnectionFailedException if a connection error occurred during retrieval of file status
     * @throws FTPCommandFailedException if an error occurred retrieving the status
     */
    public String getFileStatus(String filePath) throws FTPNotConnectedException, FTPConnectionFailedException, FTPCommandFailedException {
        if (!connected) {
            log.error("FTPConnection is not connected to the server, failed to retrieve file status of path {}", filePath);
            loggedIn = false;
            throw new FTPNotConnectedException("FTPConnection is not connected to the server, cannot retrieve status for the file", FTPNotConnectedException.ActionType.STATUS_CHECK);
        }

        try {
            if (loggedIn) {
                logDebug("Retrieving file status of file with path {}", filePath);
                return ftpLookup.getFileStatus(filePath);
            }

            logDebug("Cannot retrieve file status as user is not logged in");
            return null;
        } catch (FTPConnectionClosedException cl) {
            log.error("FTPConnection unexpectedly closed the connection while retrieving file status");
            resetConnectionValues();
            throw new FTPConnectionFailedException("The FTPConnection unexpectedly closed while retrieving file status", ftpClient.getReplyString(), cl, server);
        } catch (IOException ex) {
            log.error("An error occurred retrieving file status with path {}", filePath);
            throw new FTPCommandFailedException("An error occurred retrieving status for file", ftpClient.getReplyString(), ex);
        }
    }

    /**
     * Gets the size of the file specified by the path
     * @param path the path to the file
     * @return the size of the file specified by the path on the server
     * @throws FTPNotConnectedException if isConnected() returns false
     * @throws FTPConnectionFailedException if a connection error occurs retrieving the file size
     * @throws FTPCommandFailedException if an error occurs executing the command
     */
    public String getFileSize(String path) throws FTPNotConnectedException, FTPConnectionFailedException, FTPCommandFailedException {
        if (!connected) {
            log.error("FTPConnection is not connected to the server, cannot retrieve file size of path {}", path);
            loggedIn = false;
            throw new FTPNotConnectedException("FTPConnection is not connected to the server, cannot retrieve file size", FTPNotConnectedException.ActionType.STATUS_CHECK);
        }

        try {
            if (loggedIn) {
                logDebug("Retrieving size for the file with path {}", path);
                return ftpLookup.getFileSize(path);
            }

            logDebug("Cannot retrieve file size as user is not logged on");
            return null;
        } catch (FTPConnectionClosedException cl) {
            log.error("FTPConnection unexpectedly closed the connection while retrieving file size");
            resetConnectionValues();
            throw new FTPConnectionFailedException("The FTPConnection unexpectedly closed while retrieving file size", ftpClient.getReplyString(), cl, server);
        } catch (IOException ex) {
            log.error("An error occurred retrieving file size for path {}", path);
            throw new FTPCommandFailedException("An error occurred retrieving file size", ftpClient.getReplyString(), ex);
        }
    }

    /**
     * Returns the modification time for the file specified by the path
     * @param path the path of the file
     * @return last modified time of the file specified by path in the format HH:mm:ss dd/MM/yyyy
     * @throws FTPNotConnectedException if isConnected() returns false
     * @throws FTPConnectionFailedException if a connection error occurred retrieving modification time
     * @throws FTPCommandFailedException if an error occurs executing the command
     */
    public String getModificationTime(String path) throws FTPNotConnectedException, FTPConnectionFailedException, FTPCommandFailedException {
        if (!connected) {
            log.error("FTPConnection is not connected to the server, cannot retrieve modification time for file with path {}", path);
            loggedIn = false;
            throw new FTPNotConnectedException("FTPConnection is not connected to server, cannot retrieve modification time", FTPNotConnectedException.ActionType.STATUS_CHECK);
        }

        try {
            if (loggedIn) {
                logDebug("Retrieving modification time for file with path {}", path);
                return ftpLookup.getModificationTime(path);
            }

            logDebug("Cannot retrieve modification time as user is not logged on");
            return null;
        } catch (FTPConnectionClosedException cl) {
            log.error("FTPConnection unexpectedly closed the connection while retrieving modification time");
            resetConnectionValues();
            throw new FTPConnectionFailedException("The FTPConnection unexpectedly closed while retrieving modification time", ftpClient.getReplyString(), cl, server);
        } catch (IOException ex) {
            log.error("A connection error occurred while retrieving modification time for file {}", path);
            throw new FTPCommandFailedException("An error occurred while retrieving modification time", ftpClient.getReplyString(), ex);
        }
    }

    /**
     * Gets a FTPPathStats object for the specified path
     * This is the equivalent to calling getFileStatus(filePath), getFileSize(filePath) and getModificationTime(filePath) at once and return it as one object
     * @param filePath the path to query
     * @return a FTPPathStats object for the specified file path
     * @throws FTPNotConnectedException if isConnected() returns false
     * @throws FTPConnectionFailedException if a connection error occurs
     * @throws FTPCommandFailedException if an error occurs
     */
    public FTPPathStats getPathStats(String filePath) throws FTPNotConnectedException, FTPConnectionFailedException, FTPCommandFailedException {
        try {
            if (!connected) {
                log.error("FTPConnection is not connected, cannot retrieve path stats");
                loggedIn = false;
                throw new FTPNotConnectedException("FTPConnection is not connected, cannot retrieve path stats", FTPNotConnectedException.ActionType.STATUS_CHECK);
            }

            if (loggedIn) {
                logDebug("Retrieving path stats for path {}", filePath);
                return ftpLookup.getPathStats(filePath);
            }

            logDebug("User is not logged in, cannot retrieve path stats");
            return null;
        } catch (FTPConnectionClosedException cl) {
            log.error("FTPConnection unexpectedly closed the connection when retrieving path stats");
            resetConnectionValues();
            throw new FTPConnectionFailedException("A connection error occurred, so cannot retrieve FTPPathStats", ftpClient.getReplyString(), cl, server);
        } catch (IOException ex) {
            log.error("An error occurred retrieving file stats");
            throw new FTPCommandFailedException("An error occurred, so cannot retrieve FTPPathStats", ftpClient.getReplyString(), ex);
        }
    }

    /**
     * Sets the timeout time for this server
     * This timeout time is for all possible time outs with a FTP server
     *
     * If isConnected() returns true, this is a no-op, You should call disconnect() and then set the time out
     *
     * SHOULD BE CALLED BEFORE connect(), if not the server may time out too soon or have unexpected behaviour
     *
     * This client uses keep alive with this timeout
     * Uses the seconds from FTPConnectionDetails. This can be called if it was changed and to refresh it after disconnecting
     */
    public void setTimeoutTime() {
        if (!connected) {
            loggedIn = false;

            if (ftpConnectionDetails == null) {
                ftpConnectionDetails = new FTPConnectionDetails();
            }

            int seconds = ftpConnectionDetails.getTimeout();
            logDebug("Setting FTPConnection timeout time to {} seconds", seconds);
            noopDriver.timeoutSecs = seconds;
            int mSeconds = seconds * 1000;

            ftpClient.setDefaultTimeout(mSeconds);
            ftpClient.setControlKeepAliveTimeout(seconds);
            ftpClient.setControlKeepAliveReplyTimeout(mSeconds);
            ftpClient.setDataTimeout(0);
        }
    }

    /**
     * Returns the reply code of the last command called. This is a code that is defined in the org.apache.commons.net.ftp.FTPReply class
     * @return reply code of the last command
     */
    public int getReplyCode() {
        logDebug("Retrieving reply code of last command");
        return ftpClient.getReplyCode();
    }

    /**
     * Returns the reply string from the previously executed command
     * @return reply string from last command
     */
    public String getReplyString() {
        logDebug("Retrieving reply string from the last executed command");
        return ftpClient.getReplyString();
    }

    /**
     * This should be used with a true value when editing text files to ensure line endings get transformed for the appropriate system.
     * @param textTransfer true to set text transfer(Ascii mode on), false if not
     * @return success of operation
     */
    public boolean setTextTransferMode(boolean textTransfer) throws FTPNotConnectedException, FTPConnectionFailedException, FTPCommandFailedException {
        if (!connected) {
            log.error("FTPConnection is not connected, cannot set text mode");
            loggedIn = false;
            throw new FTPNotConnectedException("FTPConnection is not connected, cannot set text transfer mode", FTPNotConnectedException.ActionType.MODIFICATION);
        }

        try {
            if (loggedIn) {
                int fileType = textTransfer ? FTPClient.ASCII_FILE_TYPE : FTPClient.BINARY_FILE_TYPE;
                logDebug(textTransfer ? "Setting the connection to transfer files with ASCII file type" : "Setting the connection to transfer files as BINARY type");

                return ftpClient.setFileType(fileType);
            }

            logDebug("User not logged in, can't set file type");
            return false;
        } catch (FTPConnectionClosedException ex) {
            log.error("FTPConnection unexpectedly closed the connection when setting the text transfer mode");
            resetConnectionValues();
            throw new FTPConnectionFailedException("FTPConnection unexpectedly closed the connection when setting the text transfer mode", ftpClient.getReplyString(), ex, server);
        } catch (IOException ex) {
            log.error("An error occurred when setting the text transfer mode");
            throw new FTPCommandFailedException("An error occurred when setting the text transfer mode", ftpClient.getReplyString(), ex);
        }
    }

    /**
     * Checks if the FTPSystem.getConnection matches the server details provided
     * @param ftpServer the server details
     * @return true if matches, false if not
     */
    private static boolean connectionMatches(Server ftpServer) {
        FTPConnection systemConnection = FTPSystem.getConnection();
        if (systemConnection == null) {
            return false;
        } else {
            Server server = systemConnection.getServer();

            return server.equals(ftpServer);
        }
    }

    /**
     * This method sets the connection, but doesn't connect or log in
     * @param server the details to create the connection with
     * @param connectionDetails the connection details to use
     * @return the created connection
     */
    private static FTPConnection createConnection(Server server, FTPConnectionDetails connectionDetails) {
        FTPConnection connection = new FTPConnection(server, connectionDetails);
        ConnectionFTPSystem.setConnection(connection);

        return connection;
    }

    /**
     * Creates a shared FTPConnection. It does this by setting the FTPSystem.setConnection.
     * This should be used for when you want a connection to be shared among filesystem classes as the default constructors for the files and file systems uses the FTPSystem connection
     * It checks if the system's connection already matches the server details and returns that, if not, it creates a new one.
     * It does not connect or log it in.
     * @param serverDetails the server details to check/create a connection with
     * @param connectionDetails the connection details without timeout time etc
     * @return the created connection. This connection should equal FTPSystem.getConnection
     */
    public static FTPConnection createSharedConnection(Server serverDetails, FTPConnectionDetails connectionDetails) {
        serverDetails = serverDetails == null ? new Server():serverDetails;
        connectionDetails = connectionDetails == null ? new FTPConnectionDetails():connectionDetails;
        if (!connectionMatches(serverDetails)) {
            return createConnection(serverDetails, connectionDetails);
        } else {
            return FTPSystem.getConnection();
        }
    }

    /**
     * Creates a temporary FTPConnection based on the provided connection.
     * This doesn't share it with other classes. Useful for creating a separate connection for upload/download but based on the same details
     * @param connectionBasis the connection to base the temporary connection on
     * @return the temporary connection
     */
    public static FTPConnection createTemporaryConnection(FTPConnection connectionBasis) {
        return new FTPConnection(connectionBasis.server, connectionBasis.ftpConnectionDetails);
    }

    /**
     * This allows creating a temporary FTPConnection not shared with other classes from the provided details.
     * This could be useful for creating an uploading/downloading connection as another temporary user
     * @param server the Server object containing the login details
     * @param ftpConnectionDetails the details for timeout etc.
     * @return the temporary connection
     */
    public static FTPConnection createTemporaryConnection(Server server, FTPConnectionDetails ftpConnectionDetails) {
        return new FTPConnection(server, ftpConnectionDetails);
    }

    /**
     * This class is responsible for periodically sending a no-op to the server
     */
    private class NoopDriver {
        /**
         * The time the timer should run at
         */
        private int timeoutSecs;
        /**
         * The timer driving the noops
         */
        private Timer timer;
        /**
         * True if the timer has been started
         */
        private boolean started = false;
        /**
         * A flag to detect if a thread has been cancelled and on the next run, it should stop
         */
        private boolean cancelled = false;

        /**
         * Creates the NoopDriver
         * @param timeoutSecs the timeout seconds at which to send the noop
         */
        private NoopDriver(int timeoutSecs) {
            this.timeoutSecs = timeoutSecs;
            timer = new Timer();
        }

        /**
         * Starts the timer
         */
        private void start() {
            if (!started) {
                if (cancelled) {
                    timer = new Timer();
                    cancelled = false;
                }

                started = true;
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (cancelled) {
                            cancel();
                            timer.cancel();
                            started = false;
                        } else {
                            try {
                                logDebug("Sending no-op to server");
                                ftpClient.noop();
                            } catch (IOException ex) {
                                log.warn("An exception occurred sending a no-op, server may time-out");
                            }
                        }
                    }
                }, 10000, timeoutSecs * 1000);
            }
        }

        /**
         * Sets the cancelled flag to true, so that on the next run, the timer should stop
         */
        private void stop() {
            if (started)
                cancelled = true;
        }
    }
}

/**
 * This class is done so that only FTPConnection is allowed set the system connection.
 * It is a design fault to extend FTPSystem elsewhere
 */
final class ConnectionFTPSystem extends FTPSystem {
    static void setConnection(FTPConnection connection) {
        if (FTPSystem.connection != null) {
            try {
                FTPSystem.connection.disconnect();
            } catch (FTPException ex) {
                ex.printStackTrace();
            }
        }
        FTPSystem.connection = connection;
    }
}
