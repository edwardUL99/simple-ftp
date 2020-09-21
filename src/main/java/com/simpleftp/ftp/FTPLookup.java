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

import com.simpleftp.ftp.exceptions.FTPError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 *
 * This class is intended to be a helper for FTPConnection and so you must have an active and logged in connection before using this
 * All exceptions here are expected to be caught by the FTPConnection class
 * All IOExceptions are either FTPConnectionClosedExceptions or IOExceptions
 */
@RequiredArgsConstructor
@Slf4j
public class FTPLookup {
    /**
     * The client object which will be used by this class
     */
    private final FTPClient ftpClient;

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
     * @throws IOException if an error occurs.
     */
    public FTPFile getFTPFile(String path) throws IOException {
        log.info("Attempting to retrieve file from path {}", path);
        return retrieveFTPFile(path);
    }

    private FTPFile[] retrieveFilesListing(String path) throws IOException {
        if (ftpClient.hasFeature("MLSD")) {
            log.info("Using MLSD to list files in path {}", path);
            return ftpClient.mlistDir(path);
        } else {
            log.info("Using LIST to list all files in path {}", path);
            return ftpClient.listFiles(path);
        }
    }

    public FTPFile[] listFTPFiles(String path) throws IOException {
        log.info("Attempting to return list of files from server with path {}", path);
        return retrieveFilesListing(path);
    }

    /**
     * Attempts to retrieve the current working directory on the ftp server
     * @return current working directory on the ftp server
     * @throws IOException if a connection error or I/O error occurs
     */
    public String getWorkingDirectory() throws IOException {
        log.info("Retrieving the current working directory on the server");
        return ftpClient.printWorkingDirectory();
    }

    /**
     * Checks if the specified remote path exists and returns the outcome
     * @param remotePath the remote path to check
     * @param dir true if this path is a directory, false if a single file
     * @return true if path exists, false otherwise
     * @throws IOException if an I/O or connection exception occurs
     * @throws FTPError if the current working directory could not be determined
     */
    public boolean remotePathExists(String remotePath, boolean dir) throws IOException, FTPError {
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
            FTPFile[] files = ftpClient.listFiles(remotePath);
            if (files == null) {
                log.info("Remote path does not exist");
                return false;
            }

            remotePathExists = Arrays.asList(files)
                                    .stream()
                                    .map(FTPFile::getName)
                                    .anyMatch(name -> name.equals(new File(remotePath).getName()) || name.equals(remotePath));
            /* This can be replaced with remotePathExists = files.length == 1 && files[0].getName().equals(new File(remotePath).getName()) || files[0].getName().equals(remotePath);
                But being defensive in case more than one result is returned in files
             */
        }

        return remotePathExists;
    }

    /**
     * Retrieves the status of the FTP server this connection is connected to
     * @return the status of the FTP server this connection is connected to
     * @throws IOException if a connection or IO error occurs
     */
    public String getStatus() throws IOException {
        log.info("Retrieving server status");
        return ftpClient.getStatus();
    }

    /**
     * Retrieves the file status of the file/directory specified by the path
     * @param filePath the path of the file/directory to query the status of
     * @return status for the specified file/directory
     * @throws IOException if a connection or io error occurs
     */
    public String getFileStatus(String filePath) throws IOException {
        log.info("Retrieving file status of file with path {}", filePath);
        return ftpClient.getStatus(filePath);
    }

    /**
     * Gets the size of the file specified by the path
     * @param path the path to the file
     * @return the size of the file specified by the path on the server
     * @throws IOException if an IO or connection error occurs
     */
    public String getFileSize(String path) throws IOException {
        log.info("Retrieving size for the file with path {}", path);
        return ftpClient.getSize(path);
    }

    /**
     * Returns the modification time for the file specified by the path
     * @param path the path of the file
     * @return last modified time of the file specified by path in the format HH:mm:ss dd/MM/yyyy
     * @throws IOException if a connection error occurs or an IO error
     */
    public String getModificationTime(String path) throws IOException {
        log.info("Retrieving modification time for file with path {}", path);
        String timestamp = ftpClient.getModificationTime(path);
        if (timestamp == null) {
            log.info("Could not retrieve modification time for {}", path);
            return null;
        }

        LocalDateTime time = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return time.format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
    }

    /**
     * Gets a FTPPathStats object for the specified path
     * This is the equivalent to calling getFileStatus(filePath), getFileSize(filePath) and getModificationTime(filePath) at once and return it as one object
     * @param filePath the path to query
     * @return a FTPPathStats object for the specified file path
     * @throws IOException if an IO error occurs
     */
    public FTPPathStats getPathStats(String filePath) throws IOException {
        return new FTPPathStats(filePath, getModificationTime(filePath), getFileStatus(filePath), getFileSize(filePath));
    }
}
