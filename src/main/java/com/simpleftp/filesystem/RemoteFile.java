/*
 *  Copyright (C) 2020-2021 Edward Lynch-Milner
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

import com.simpleftp.filesystem.exceptions.PathResolverException;
import com.simpleftp.filesystem.paths.PathResolverFactory;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.*;
import com.simpleftp.properties.Properties;
import com.simpleftp.ui.UI;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.net.ftp.FTPFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * Represents a remote file associated with the provided FTPConnection instance
 */
@AllArgsConstructor
public class RemoteFile implements CommonFile {
    /**
     * A connection to use if the remote file is not to use the FTPSystem connection
     */
    private FTPConnection connection;
    /**
     * The FTPFile backing this remote file
     */
    @Getter
    private FTPFile ftpFile;
    /**
     * The ftp file representing the target if this is a symbolic link
     */
    private FTPFile targetFile;
    /**
     * The absolute path for this remote file
     */
    private final String absolutePath;
    /**
     * Tracks if this file exists. Calling the exists() method updates it
     */
    private boolean exists;
    /**
     * A variable to cache the size of the file
     */
    private Long size;
    /**
     * This caches the permissions for this file
     */
    private String permissions;
    /**
     * This caches the modification time for this file
     */
    private String modificationTime;
    /**
     * Indicates this file is a "temporary" file and has been created using a connection that differs to the system connection
     */
    private final boolean temporaryFile;

    /**
     * Constructs a remote file name with the specified file name, using the system's connection (i.e. not temporary)
     * @param fileName the name of the file. Must be an absolute path (i.e. starts with /)
     */
    public RemoteFile(String fileName) throws FileSystemException {
        this(fileName, null);
    }

    /**
     * Creates a RemoteFile with the provided path and ftp file instance which is not "temporary", i.e. uses the system's connection
     * @param fileName the name of the file (absolute path)
     * @param ftpFile the FTPFile instance to back this RemoteFile
     * @throws FileSystemException if an error occurs
     */
    public RemoteFile(String fileName, FTPFile ftpFile) throws FileSystemException {
        validateFilePath(fileName);

        FTPConnection connection = FTPSystem.getConnection();
        if (connection == null) {
            throw new FileSystemException("There's no FTP Connection setup");
        }

        temporaryFile = false;

        validateConnection(connection);

        absolutePath = fileName;
        initialiseFTPFile(ftpFile);
        initSymlinkProperties();
    }

    /**
     * Creates a "temporary" RemoteFile with the specified connection and ftp file. If the provided connection != FTPSystem.getConnection(),
     * use this constructor.
     * Uses fileName as path to search on Server to initialise the FTPFile returned by getFtpFile
     * @param fileName the name of the file (absolute path required)
     * @param ftpConnection the connection to use
     * @param ftpFile the FTPFile to initialise this RemoteFile with. Leave null to force a lookup on FTP Server with fileName as path. This file may not exist. Assumed it does exist if you have the file
     * @throws FileSystemException if an error occurs
     */
    public RemoteFile(String fileName, FTPConnection ftpConnection, FTPFile ftpFile) throws FileSystemException {
        validateFilePath(fileName);
        absolutePath = fileName;
        this.connection = ftpConnection;
        temporaryFile = true;

        validateConnection(this.connection);
        initialiseFTPFile(ftpFile);
        initSymlinkProperties();
    }

    /**
     * Validates that the file path is absolute and throws an IllegalArgumentException if not
      * @param filePath the file path to validate
     */
    private void validateFilePath(String filePath) {
        if (!filePath.startsWith("/"))
            throw new IllegalArgumentException("The path passed into a RemoteFile must be absolute");
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
     * Returns the connection to use for the remote file. FTPSystem if this file is not a temporary file, the provided connection if temporary file
     * @return connection to use for this remote file
     */
    private FTPConnection getConnection() {
        return temporaryFile ? connection:FTPSystem.getConnection();
    }

    /**
     * Initialises the FTPFile object backing this file.
     * @param ftpFile the file to initialise with. Leave null if you want to force a lookup on the Server using the filename provided
     */
    private void initialiseFTPFile(FTPFile ftpFile) throws FileSystemException {
       try {
           if (ftpFile == null) {
               this.ftpFile = getConnection().getFTPFile(absolutePath);
               exists = this.ftpFile != null;
           } else {
               this.ftpFile = ftpFile;
               exists = true;
           }

           if (!absolutePath.equals(".") && !absolutePath.equals("..") && exists)
               this.ftpFile = checkFileForCurrOrParentDir(this.ftpFile);
       } catch (Exception ex) {
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
            String parentPath = FileUtils.getParentPath(absolutePath, false);
            if (parentPath.equals("/")) {
                // we're at root
                return file;
            }

            FTPFile[] files = getConnection().listFiles(parentPath);

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

    /**
     * Pulls the name of the absolute remote path given and returns it
     * @param absolutePath the absolute path to retrieve name from
     * @return the name of the file the absolute path refers to
     */
    public static String getName(String absolutePath) {
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
    public String getName() {
        return getName(absolutePath);
    }

    @Override
    public String getFilePath() {
        return absolutePath;
    }

    /**
     * This method initialises the symbolic link properties if this file is a symlink
     * @throws FileSystemException if an error occurs
     */
    private void initSymlinkProperties() throws FileSystemException {
        try {
            if (isSymbolicLink())
                refreshSymbolicLinkProperties();
        } catch (Exception ex) {
            throw new FileSystemException("An error occurred initialising the file", ex);
        }
    }

    /**
     * Gets an absolute version of the link path of the ftpFile
     * @return the absolute link path
     */
    private String getLinkPath() {
        String linkPath = ftpFile.getLink();
        if (!linkPath.startsWith("/"))
            return FileUtils.appendPath(FileUtils.getParentPath(absolutePath, false), linkPath, false);
        else
            return linkPath;
    }

    /**
     * Follows the link of this file if it is a symbolic link until it eventually gets to the destination
     * @return the FTPFile representing the destination of the link, null if not a symbolic link
     * @throws Exception if any exception occurs
     */
    private FTPFile followLink() throws Exception {
        if (isSymbolicLink()) {
            String linkPath = getLinkPath();
            FTPConnection connection = getConnection();
            FTPFile file = connection.getFTPFile(linkPath);

            while (file != null && file.isSymbolicLink() && !FileUtils.pathEquals(absolutePath, linkPath, false)) {
                String linkPath1 = FileUtils.getParentPath(linkPath, false);
                String nextLink = file.getLink();
                linkPath = !nextLink.startsWith("/") ? FileUtils.appendPath(linkPath1, nextLink, false):nextLink;

                file = connection.getFTPFile(linkPath);
            }

            if (file != null)
                file.setName(linkPath);

            return file;
        }

        return null;
    }

    /**
     * Refreshes the properties for a symbolic link
     */
    private void refreshSymbolicLinkProperties() throws Exception {
        FTPFile symbolicFile = followLink();
        exists = symbolicFile != null;
        if (exists) {
            targetFile = symbolicFile;
        }
    }

    /**
     * Checks if this file exists as either a directory or a normal file on the provided FTPConnection.
     * This syncs up any cached information with the latest info of the file
     * @return true if exists, false if not
     * @throws FileSystemException if an error occurs
     */
    @Override
    public boolean exists() throws FileSystemException {
        refresh();
        return exists;
    }

    /**
     * Checks if this file is a directory.
     * May return true if the file no longer exists as this RemoteFile may not be an up to date version. Calling exists/refresh updates this information
     * @return true if a directory, false if not
     * @throws FileSystemException if an error occurs
     */
    @Override
    public boolean isADirectory() throws FileSystemException {
        if (!exists) {
            return false;
        } else if (ftpFile.isValid() && !ftpFile.isSymbolicLink()) {
            return ftpFile.isDirectory();
        } else if (isSymbolicLink() && targetFile.isValid()) {
            return targetFile.isDirectory();
        } else {
            try { // if link was broken here, it would return false
                if (isSymbolicLink())
                    return getConnection().remotePathExists(getLinkPath(), true);
                else
                    return getConnection().remotePathExists(absolutePath, true);
            } catch (FTPException ex) {
                throw new FileSystemException("A FTP Exception occurred, it could not be determined if this file is a directory", ex);
            }
        }
    }

    /**
     * Checks if this file is a normal file.
     * May return true, even if the file doesn't exist anymore, e.g. if the file is deleted after this RemoteFile is created. Calling exists()/refresh() should be called if an update on the file is required or just general existence and if you don't need to know if it is a file or directory, calling exists updates this information
     * @return true if a directory, false if not
     * @throws FileSystemException if an error occurs
     */
    @Override
    public boolean isNormalFile() throws FileSystemException {
        if (!exists) {
            return false;
        } else if (ftpFile.isValid() && !ftpFile.isSymbolicLink()) {
            return ftpFile.isFile();
        } else if (isSymbolicLink() && (targetFile != null && targetFile.isValid())) {
            return targetFile.isFile();
        } else {
            try {
                if (isSymbolicLink()) {// ensure link isn't broken
                    return getConnection().remotePathExists(getLinkPath(), false);
                } else {
                    return getConnection().remotePathExists(absolutePath, false);
                }
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
        if (size == null) {
            boolean getTargetSize = ftpFile.isSymbolicLink() && Properties.FILE_SIZE_FOLLOW_LINK.getValue();
            if (!exists) {
                size = -1L;
            } else if (ftpFile.isValid() && !getTargetSize) {
                size = ftpFile.getSize();
            } else if (getTargetSize && targetFile.isValid()) {
                size = targetFile.getSize();
            } else {
                try {
                    FTPConnection connection = getConnection();
                    String path = getTargetSize ? getLinkPath():absolutePath;
                    String size = connection.getFileSize(path);
                    if (size != null) {
                        this.size = Long.parseLong(size);
                    } else {
                        this.size = getTargetSize ? targetFile.getSize():ftpFile.getSize();
                    }
                } catch (FTPException | NumberFormatException ex) {
                    throw new FileSystemException("An error occurred retrieving size of file", ex);
                }
            }
        }

        return size;
    }

    /**
     * Returns the path without separators (if a path is equal to another path without separators, that means that it would be equal with the separator.
     * This is used for hashcode also
     * @return path without separators
     */
    private String getPathWithoutSeparators() {
        return absolutePath.replaceAll("/", "");
    }

    /**
     * Returns the hashcode of this file's path + 1 for remote
     * @return hash code for use in hash data structures
     */
    @Override
    public int hashCode() {
        return getPathWithoutSeparators().hashCode() + 1;
    }

    /**
     * Determines if this file is equal to the object provided based on file path
     * @param obj the object to compare to
     * @return true if equal, false if not
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RemoteFile)) {
            return false;
        } else if (this == obj) {
            return true;
        } else {
            RemoteFile remoteFile = (RemoteFile)obj;

            return this.getPathWithoutSeparators().equals(remoteFile.getPathWithoutSeparators());
        }
    }

    /**
     * Calculates the permissions for the provided FTPFile
     * @param file the file to calculate permissions for
     * @return the permissions for the file
     */
    private String getPermissions(FTPFile file) {
        if (file != null) {
            String permissions = "";
            if (file.isSymbolicLink()) {
                permissions += "l";
            } else if (file.isDirectory()) {
                permissions += "d";
            } else {
                permissions += "-";
            }

            if (file.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION)) {
                permissions += "r";
            } else {
                permissions += "-";
            }

            if (file.hasPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION)) {
                permissions += "w";
            } else {
                permissions += "-";
            }

            if (file.hasPermission(FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION)) {
                permissions += "x";
            } else {
                permissions += "-";
            }

            if (file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.READ_PERMISSION)) {
                permissions += "r";
            } else {
                permissions += "-";
            }

            if (file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.WRITE_PERMISSION)) {
                permissions += "w";
            } else {
                permissions += "-";
            }

            if (file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.EXECUTE_PERMISSION)) {
                permissions += "x";
            } else {
                permissions += "-";
            }

            if (file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.READ_PERMISSION)) {
                permissions += "r";
            } else {
                permissions += "-";
            }

            if (file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.WRITE_PERMISSION)) {
                permissions += "w";
            } else {
                permissions += "-";
            }

            if (file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.EXECUTE_PERMISSION)) {
                permissions += "x";
            } else {
                permissions += "-";
            }

            return permissions;
        }

        return null;
    }

    /**
     * Gets the permissions as a string in the unix form of ls command. For Non-posix systems, this just displays the permissions for the user running the program
     *
     * @return the permissions as a string
     */
    @Override
    public String getPermissions() {
        if (permissions == null) {
            if (isSymbolicLink() && Properties.FILE_PERMS_FOLLOW_LINK.getValue()) {
                permissions = getPermissions(targetFile);
            } else {
                permissions = getPermissions(ftpFile);
            }
        }

        return permissions;
    }

    /**
     * Gets the modification time as a formatted String in the form of Month Day Hour:Minute, e.g Jan 01 12:50
     *
     * @return the formatted modification time String
     * @throws FileSystemException if an error occurs
     */
    @Override
    public String getModificationTime() throws FileSystemException {
        if (this.modificationTime == null) {
            try {
                FTPFile ftpFile = getFtpFile();
                String filePath = getFilePath();
                String fileModTime = Properties.SERVER_REMOTE_MODIFICATION_TIME.getValue() ? getConnection().getModificationTime(filePath):null;
                if (fileModTime != null) {
                    LocalDateTime dateTime = LocalDateTime.parse(fileModTime, DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
                    modificationTime = dateTime.format(DateTimeFormatter.ofPattern(UI.FILE_DATETIME_FORMAT));
                } else {
                    ftpFile = isSymbolicLink() ? targetFile:ftpFile;
                    modificationTime = ftpFile.isValid() ? FileUtils.parseCalendarToFormattedDate(ftpFile.getTimestamp()) : null;
                }
            } catch (FTPException ex) {
                throw new FileSystemException("An error occurred retrieving file modification time", ex);
            }
        }

        return modificationTime;
    }

    /**
     * This method resets any cached variables since the last call to exists()
     */
    private void resetCachedVariables() {
        targetFile = null;
        size = null;
        modificationTime = null;
        permissions = null;
    }

    /**
     * Refreshes the file if the file implementation caches certain info. E.g a remote file may rather than making multiple calls to the server
     */
    @Override
    public void refresh() throws FileSystemException {
        resetCachedVariables();
        initialiseFTPFile(null);
        if (isSymbolicLink()) {
            try {
                refreshSymbolicLinkProperties();
            } catch (Exception ex) {
                throw new FileSystemException("An error occurred refreshing the file");
            }
        }
    }

    /**
     * Checks if this file is a symbolic link. To determine what type of file the symbolic link points to, call isANormalFile or isADirectory
     *
     * @return true if it is a symbolic link
     */
    @Override
    public final boolean isSymbolicLink() {
        return ftpFile != null && ftpFile.isSymbolicLink();
    }

    /**
     * Gets the target of the symbolic link, canonicalised
     *
     * @return the symbolic link target, null if not symbolic link
     */
    @Override
    public String getSymbolicLinkTarget() throws FileSystemException {
        if (isSymbolicLink()) {
            String path = targetFile.getName();//ftpFile.getLink();
            String parent = FileUtils.getParentPath(absolutePath, false);

            try {
                return PathResolverFactory.newInstance()
                        .setRemote(parent, getConnection(), true)
                        .build()
                        .resolvePath(path);
            } catch (PathResolverException ex) {
                throw new FileSystemException("An error occurred resolving symbolic link target", ex);
            }
        }

        return null;
    }

    /**
     * This file may be present locally or remotely. This method determines if it is local or remote
     *
     * @return true if local, false if remote
     */
    @Override
    public boolean isLocal() {
        return false;
    }

    /**
     * This method retrieves the <b>next</b> available parent of this file. What this means is that the parent retrieved
     * may not be the immediate parent of this file, it is just the next parent that exists. It is guaranteed to not be null
     * as the one parent that will always exist is the root file.
     *
     * @return the next available parent of this file
     * @throws FileSystemException if an error occurs retrieving the parent
     */
    @Override
    public RemoteFile getExistingParent() throws FileSystemException {
        String path = FileUtils.getParentPath(absolutePath, false);

        try {
            FTPConnection connection = getConnection();
            FTPFile parentFile = connection.getFTPFile(path);
            while (parentFile == null) {
                path = FileUtils.getParentPath(path, false);
                parentFile = connection.getFTPFile(path);
            }

            if (temporaryFile)
                return new RemoteFile(path, connection, parentFile);
            else
                return new RemoteFile(path, parentFile);
        } catch (FTPException ex) {
            throw new FileSystemException("An FTP error occurred retrieving existing parent file", ex);
        }
    }
}
