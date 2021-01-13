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
import com.simpleftp.ui.UI;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.net.ftp.FTPFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;

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
     * The absolute path for this remote file
     */
    private final String absolutePath;
    /**
     * Tracks if this file exists. Calling the exists() method updates it
     */
    private boolean exists;
    /**
     * Indicates this file is a "temporary" file and has been created using a connection that differs to the system connection
     */
    private final boolean temporaryFile;

    /**
     * Constructs a remote file name with the specified file name
     * @param fileName the name of the file. Must be an absolute path (i.e. starts with /)
     */
    public RemoteFile(String fileName) throws FileSystemException {
        this(fileName, null);
    }

    /**
     * Creates a RemoteFile with the provided path and ftp file instance
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
    }

    /**
     * Creates a "temporary" RemoteFile with the specified connection and ftp file. Uses fileName as path to search on Server to initialise the FTPFile returned by getFtpFile
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
     * Retrieves a RemoteFile representing the symbolic file for the specified file path if it is symbolic from the listing, as listing the path itself produces an error.
     * Apache FTPClient has a limitation where if you listFiles on a symbolic link file, it returns no file, so this method is a workaround.
     * See the wiki Symbolic Links for the known issues.
     * @param connection the connection used to list the file
     * @param filePath the file path
     * @return the FTPFile found, null if not found
     * @throws Exception if any exception from the connection is thrown
     */
    public static RemoteFile getSymbolicFile(FTPConnection connection, String filePath) throws Exception {
        RemoteFile remoteFile = new RemoteFile(filePath);
        String parentPath = FileUtils.getParentPath(filePath, false);

        FTPFile[] files = connection.listFiles(parentPath);

        FTPFile file = Arrays.stream(files)
                .filter(file1 -> file1.getName().equals(remoteFile.getName()))
                .findFirst()
                .orElse(null);

        if (file != null) {
            return new RemoteFile(filePath, connection, file);
        }

        return null;
    }

    /**
     * If this is a symbolic path, getFTPFile may return null on it, so instead, find it in the listing of the parent directory.
     * If not a symbolic link, this method returns null
     * @return the found file, null if not found
     */
    private FTPFile getSymbolicFile() throws Exception {
        return Optional.ofNullable(getSymbolicFile(getConnection(), absolutePath))
                .map(file -> file.ftpFile)
                .orElse(null);
    }

    /**
     * Initialises the FTPFile object backing this file.
     * @param ftpFile the file to initialise with. Leave null if you want to force a lookup on the Server using the filename provided
     */
    private void initialiseFTPFile(FTPFile ftpFile) throws FileSystemException {
       try {
           if (ftpFile == null) {
               if (isSymbolicLink())
                   this.ftpFile = getSymbolicFile();
               else
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
            boolean oldExists = exists;
            if (isSymbolicLink())
                exists = getConnection().remotePathExists(getSymbolicLinkTarget());
            else
                exists = getConnection().remotePathExists(absolutePath);

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
            try { // if link was broken here, it would return false
                if (isSymbolicLink())
                    return exists() && getConnection().remotePathExists(getSymbolicLinkTarget(), true);
                else
                    return getConnection().remotePathExists(absolutePath, true);
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
                if (isSymbolicLink()) {// ensure link isn't broken
                    return exists() && getConnection().remotePathExists(getSymbolicLinkTarget(), false);
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
        if (!exists) {
            return -1;
        } else if (ftpFile.isValid() && (!ftpFile.isSymbolicLink() || !FileUtils.FILE_SIZE_FOLLOW_LINK)) {
            return ftpFile.getSize();
        } else {
            try {
                FTPConnection connection = getConnection();
                String path = ftpFile.isSymbolicLink() ? getSymbolicLinkTarget():absolutePath;
                String size = connection.getFileSize(path);
                if (size != null) {
                    return Long.parseLong(size);
                } else {
                    return connection.getFTPFile(path).getSize();
                }
            } catch (FTPException | NumberFormatException ex) {
                throw new FileSystemException("An error occurred retrieving size of file", ex);
            }
        }
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
     * Returns the hashcode of this file's path
     * @return hash code for use in hash data structures
     */
    @Override
    public int hashCode() {
        return getPathWithoutSeparators().hashCode();
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
     * Calculates the permissions for a remote file
     * @return the permissions
     */
    private String calculateRemotePermissions() {
        if (isSymbolicLink() && FileUtils.FILE_PERMS_FOLLOW_LINK) {
            try {
                return new RemoteFile(getSymbolicLinkTarget()).calculateRemotePermissions();
            } catch (FileSystemException ex) {
                if (FTPSystem.isDebugEnabled())
                    ex.printStackTrace();
                // just display the permissions of the link's file
                return calculateRemotePermissions();
            }
        } else {
            String permissions = "";
            FTPFile file = getFtpFile();

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
    }

    /**
     * Gets the permissions as a string in the unix form of ls command. For Non-posix systems, this just displays the permissions for the user running the program
     *
     * @return the permissions as a string
     */
    @Override
    public String getPermissions() {
        if (isSymbolicLink() && FileUtils.FILE_PERMS_FOLLOW_LINK) {
            try {
                return new RemoteFile(getSymbolicLinkTarget()).getPermissions();
            } catch (FileSystemException ex) {
                if (FTPSystem.isDebugEnabled())
                    ex.printStackTrace();
                // just return the permissions of this file even if it is a link
            }
        }
        return calculateRemotePermissions();
    }

    /**
     * Gets the modification time as a formatted String in the form of Month Day Hour:Minute, e.g Jan 01 12:50
     *
     * @return the formatted modification time String
     * @throws FileSystemException if an error occurs
     */
    @Override
    public String getModificationTime() throws FileSystemException {
        try {
            String modificationTime;
            FTPFile ftpFile = getFtpFile();
            String filePath = getFilePath();
            String fileModTime = getConnection().getModificationTime(filePath);
            if (fileModTime != null) {
                LocalDateTime dateTime = LocalDateTime.parse(fileModTime, DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
                modificationTime = dateTime.format(DateTimeFormatter.ofPattern(UI.FILE_DATETIME_FORMAT));
            } else {
                modificationTime = ftpFile.isValid() ? FileUtils.parseCalendarToFormattedDate(ftpFile.getTimestamp()):null;
            }

            return modificationTime;
        } catch (FTPException ex) {
            throw new FileSystemException("An error occurred retrieving file modification time", ex);
        }
    }

    /**
     * Refreshes the file if the file implementation caches certain info. E.g a remote file may rather than making multiple calls to the server
     */
    @Override
    public void refresh() throws FileSystemException {
        if (exists()) {
            initialiseFTPFile(null); // refresh permissions
        }
    }

    /**
     * Checks if this file is a symbolic link. To determine what type of file the symbolic link points to, call isANormalFile or isADirectory
     *
     * @return true if it is a symbolic link
     */
    @Override
    public boolean isSymbolicLink() {
        return ftpFile != null && ftpFile.isSymbolicLink();
    }

    /**
     * Gets the target of the symbolic link
     *
     * @return the symbolic link target, null if not symbolic link
     */
    @Override
    public String getSymbolicLinkTarget() throws FileSystemException {
        if (ftpFile.isSymbolicLink()) {
            String path = ftpFile.getLink();
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

        FTPConnection connection = getConnection();
        RemoteFile parentFile = new RemoteFile(path);
        while (!parentFile.isADirectory()) {
            path = FileUtils.getParentPath(path, false);
            parentFile = new RemoteFile(path);
        }

        if (temporaryFile)
            return new RemoteFile(parentFile.getFilePath(), connection, parentFile.getFtpFile());
        else
            return new RemoteFile(parentFile.getFilePath(), parentFile.getFtpFile());
    }
}
