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

import com.simpleftp.filesystem.FileUtils;
import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.exceptions.FTPError;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * The responsibility of this class is to provided lookup features for info on the server.
 * This can be used for looking up files, status checks on these files, the existence of remote paths etc.
 * <p>
 * This class is intended to be a helper for FTPConnection and so you must have an active and logged in connection before using this.
 * All exceptions here are expected to be caught by the FTPConnection class.
 * All IOExceptions are either FTPConnectionClosedExceptions or IOExceptions.
 * <p>
 * This class is thread-safe. It is intended to be only used as a helper class for FTPConnection and thus, FTPLookup's constructor is protected
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Log4j2
public class FTPLookup {
    /**
     * The client object which will be used by this class
     */
    private final FTPClient ftpClient;

    private void logDebug(String message, Object... options) {
        if (FTPSystem.isDebugEnabled())
            log.debug(message, options);
    }

    private String getParentPath(String path) {
        String parent = new LocalFile(path).getParent();
        parent = parent == null ? "/" : parent;

        return parent;
    }

    /**
     * Retrieves the specified FTPFile from the server
     *
     * @param path the path of the file
     * @return the retrieved file, null if not found
     * @throws IOException if any FTP or IO Exception occurs
     */
    private synchronized FTPFile retrieveFTPFile(String path) throws IOException {
        if (ftpClient.hasFeature("MLST")) {
            // this is the preferred command as it supports more precise timestamps and other features
            logDebug("Using MLST command to retrieve file {}", path);
            return ftpClient.mlistFile(path);
        } else {
            // server doesn't support MLST, using LIST
            logDebug("Using LIST command to attempt to retrieve the file {}", path);
            FTPFile[] files = ftpClient.listFiles(path);

            if (files != null && files.length > 0) {
                FTPFile file;

                if (files.length > 1 && !path.equals("/")) {
                    files = ftpClient.listFiles(getParentPath(path));

                    file = Arrays.stream(files)
                            .filter(e -> e.getName().equals(new File(path).getName()))
                            .findFirst()
                            .orElse(null);
                } else {
                    file = files[0];
                }

                if (file != null)
                    logDebug("File retrieved successfully from server");

                return file;
            } else {
                FTPFile file = getSymbolicFTPFile(path);

                if (file == null) {
                    logDebug("File cannot be retrieved from server");
                    return null;
                } else {
                    return file;
                }
            }
        }
    }

    /**
     * Gets the FTPFile for the symbolic file. Apache FTPClient returns an empty array if listFiles is called on a symbolic link of a file.
     * This method is a workaround
     * @param filePath the file path of the link
     * @return the FTPFile object
     */
    private FTPFile getSymbolicFTPFile(String filePath) throws IOException {
        String parentPath = FileUtils.getParentPath(filePath, false);

        FTPFile[] files = ftpClient.listFiles(parentPath);

        return files != null ? Arrays.stream(files)
                .filter(file -> file.getName().equals(RemoteFile.getName(filePath)))
                .findFirst()
                .orElse(null):null;
    }

    /**
     * Attempts to retrieve the FTP File specified at the path
     *
     * @param path the path to the file
     * @return a FTPFile object representing the specified path, or null if not found
     * @throws IOException if an error occurs.
     */
    public synchronized FTPFile getFTPFile(String path) throws IOException {
        logDebug("Attempting to retrieve file from path {}", path);
        return retrieveFTPFile(path);
    }

    /**
     * Retrieves a list of files for the specified path
     *
     * @param path the path to list
     * @return the array of files
     * @throws IOException if any FTP or IO exception occurs
     */
    private synchronized FTPFile[] retrieveFilesListing(String path) throws IOException {
        if (ftpClient.hasFeature("MLSD")) {
            logDebug("Using MLSD to list files in path {}", path);
            return ftpClient.mlistDir(path);
        } else {
            logDebug("Using LIST to list all files in path {}", path);
            return ftpClient.listFiles(path);
        }
    }

    /**
     * Lists the files in the given path
     *
     * @param path the path to list
     * @return the list of files
     * @throws IOException if an error occurs
     */
    public synchronized FTPFile[] listFTPFiles(String path) throws IOException {
        logDebug("Attempting to return list of files from server with path {}", path);
        FTPFile[] files = retrieveFilesListing(path);
        if (files != null && files.length == 0)
            return null;

        return files;
    }

    /**
     * Attempts to retrieve the current working directory on the ftp server
     *
     * @return current working directory on the ftp server
     * @throws IOException if a connection error or I/O error occurs
     */
    public synchronized String getWorkingDirectory() throws IOException {
        logDebug("Retrieving the current working directory on the server");
        return ftpClient.printWorkingDirectory();
    }

    /**
     * Checks if the specified remote path exists and returns the outcome
     *
     * @param remotePath the remote path to check
     * @param dir        true if this path is a directory, false if a single file
     * @return true if path exists, false otherwise
     * @throws IOException if an I/O or connection exception occurs
     * @throws FTPError    if the current working directory could not be determined
     */
    public synchronized boolean remotePathExists(String remotePath, boolean dir) throws IOException, FTPError {
        logDebug("Querying if remote path {} exists as a {}", remotePath, dir ? "directory" : "file");

        boolean remotePathExists;

        if (dir) {
            String currentWorkingDirectory = ftpClient.printWorkingDirectory();

            if (currentWorkingDirectory == null) {
                log.error("Cannot determine the current working directory, cannot proceed with checking if directory exists");
                throw new FTPError("Cannot determine if remotePath exists on this server", null);
            }

            remotePathExists = ftpClient.changeWorkingDirectory(remotePath); // if you changed to the directory successfully, it exists
            ftpClient.changeWorkingDirectory(currentWorkingDirectory); //return to the current directory
        } else {
            FTPFile[] files = ftpClient.listFiles(remotePath);
            if (files == null) {
                logDebug("Remote path does not exist");
                return false;
            }

            FTPFile fileToCheck;
            if (files.length > 0) {
                if ((files[0].getName().equals(".") || files[0].getName().equals("..")) && files.length == 2) {
                    fileToCheck = files[1]; // if the file is the same name as the parent folder or same name as folder in the next level above (i.e. ..), ftpClient.listFiles returns a "." or ".." as files[0] as . is equivalent to the name of the parent folder or .. is equivalent to the parent of the parent folder
                } else {
                    fileToCheck = files[0];
                }

                remotePathExists = (fileToCheck.getName().equals(new File(remotePath).getName()) || fileToCheck.getName().equals(remotePath));
            } else {
                remotePathExists = false;
            }
        }

        return remotePathExists;
    }

    /**
     * Checks if the path exists as either a directory or a file.
     * This method is useful for when you want to do a check but don't care if it is a directory or a file, just want to know it exists
     *
     * @param remotePath the path to check existence of
     * @return true if it exists
     * @throws IOException if an error occurs sending or retrieving the command
     * @throws FTPError    if an error retrieving working directory occurs
     */
    public synchronized boolean remotePathExists(String remotePath) throws IOException, FTPError {
        return remotePathExists(remotePath, true) || remotePathExists(remotePath, false);
    }

    /**
     * Retrieves the status of the FTP server this connection is connected to
     *
     * @return the status of the FTP server this connection is connected to
     * @throws IOException if a connection or IO error occurs
     */
    public synchronized String getStatus() throws IOException {
        logDebug("Retrieving server status");
        return ftpClient.getStatus();
    }

    /**
     * Retrieves the file status of the file/directory specified by the path
     *
     * @param filePath the path of the file/directory to query the status of
     * @return status for the specified file/directory
     * @throws IOException if a connection or io error occurs
     */
    public synchronized String getFileStatus(String filePath) throws IOException {
        logDebug("Retrieving file status of file with path {}", filePath);
        return ftpClient.getStatus(filePath);
    }

    /**
     * Gets the size of the file specified by the path
     *
     * @param path the path to the file
     * @return the size of the file specified by the path on the server
     * @throws IOException if an IO or connection error occurs
     */
    public synchronized String getFileSize(String path) throws IOException {
        logDebug("Retrieving size for the file with path {}", path);
        return ftpClient.getSize(path);
    }

    /**
     * Returns the modification time for the file specified by the path
     *
     * @param path the path of the file
     * @return last modified time of the file specified by path in the format HH:mm:ss dd/MM/yyyy
     * @throws IOException if a connection error occurs or an IO error
     */
    public synchronized String getModificationTime(String path) throws IOException {
        logDebug("Retrieving modification time for file with path {}", path);
        String timestamp = ftpClient.getModificationTime(path);
        if (timestamp == null) {
            logDebug("Could not retrieve modification time for {}", path);
            return null;
        }

        LocalDateTime time = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return time.format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
    }

    /**
     * Gets a FTPPathStats object for the specified path
     * This is the equivalent to calling getFileStatus(filePath), getFileSize(filePath) and getModificationTime(filePath) at once and return it as one object
     *
     * @param filePath the path to query
     * @return a FTPPathStats object for the specified file path
     * @throws IOException if an IO error occurs
     */
    public synchronized FTPPathStats getPathStats(String filePath) throws IOException {
        return new FTPPathStats(filePath, getModificationTime(filePath), getFileStatus(filePath), getFileSize(filePath));
    }
}
