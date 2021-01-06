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
import com.simpleftp.filesystem.exceptions.PathResolverException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.filesystem.paths.PathResolverFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.Set;

/**
 * A file representing a local file. While the interfaces specifies that some methods return a FileSystemException, that
 * doesn't occur in LocalFile, more so RemoteFile
 */
public class LocalFile extends File implements CommonFile {

    /**
     * Constructs a LocalFile with the specified pathname
     * @param pathname the pathname of the file
     */
    public LocalFile(String pathname) {
        super(pathname);
    }

    /**
     * Returns absolute path to the file
     * @return absolute path
     */
    @Override
    public String getFilePath() {
        return super.getAbsolutePath();
    }

    @Override
    public boolean isADirectory() {
        return isSymbolicLink() ? super.isDirectory():Files.isDirectory(toPath());
    }

    @Override
    public boolean isNormalFile() {
        return super.isFile();
    }

    /**
     * Returns the hashcode of this file by returning the hashcode of the file path
     * @return hash code for use in hash maps
     */
    @Override
    public int hashCode() {
        return getFilePath().hashCode();
    }

    /**
     * Determines whether two CommonFiles are equal, either by hash code or path
     *
     * @return true if equals, false if not
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LocalFile))
            return false;

        if (this == obj) {
            return true;
        } else {
            LocalFile localFile = (LocalFile)obj;

            return this.getFilePath().equals(localFile.getFilePath());
        }
    }

    /**
     * Returns the size in bytes of the file
     *
     * @return size in bytes of the file
     */
    @Override
    public long getSize() throws FileSystemException {
        if (!isSymbolicLink()) {
            return length();
        } else {
            return new LocalFile(getSymbolicLinkTarget()).getSize();
        }
    }

    /**
     * Checks if POSIX permissions are supported
     * @return the permissions, null if posix permissions are not supported
     */
    private String resolvePosixPermissions() {
        Path path = toPath();

        if (path.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            try {
                Set<PosixFilePermission> permissionsSet = Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS);
                String[] permissionsStringArr = new String[9]; // first 3 indices, owner rwx, next 3, group, last 3 others
                for (int i = 0; i < 9; i++)
                    permissionsStringArr[i] = "-";

                for (PosixFilePermission permission : permissionsSet) {
                    switch (permission) {
                        case OWNER_READ: permissionsStringArr[0] = "r"; break;
                        case OWNER_WRITE: permissionsStringArr[1] = "w"; break;
                        case OWNER_EXECUTE: permissionsStringArr[2] = "x"; break;
                        case GROUP_READ: permissionsStringArr[3] = "r"; break;
                        case GROUP_WRITE: permissionsStringArr[4] = "w"; break;
                        case GROUP_EXECUTE: permissionsStringArr[5] = "x"; break;
                        case OTHERS_READ: permissionsStringArr[6] = "r"; break;
                        case OTHERS_WRITE: permissionsStringArr[7] = "w"; break;
                        case OTHERS_EXECUTE: permissionsStringArr[8] = "x"; break;
                    }
                }

                StringBuilder permissions = new StringBuilder();

                for (String s : permissionsStringArr)
                    permissions.append(s);

                return permissions.toString();
            } catch (IOException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Resolves permissions on systems that aren't posix standard. This can only return permissions for current user
     * @return the permissions string
     */
    private String resolveNonPosixPermissions() {
        String permissions = "";

        if (canRead()) {
            permissions += "r";
        } else {
            permissions += "-";
        }

        if (canWrite()) {
            permissions += "-w";
        } else {
            permissions += "--";
        }

        if (canExecute()) {
            permissions += "-x";
        } else {
            permissions += "--";
        }

        return permissions;
    }

    /**
     * Calculates the permissions for a local file
     * @return the permissions
     */
    private String calculateLocalPermissions() {
        String permissions = resolvePosixPermissions();
        if (permissions == null)
            permissions = resolveNonPosixPermissions();

        if (Files.isSymbolicLink(toPath())) {
            permissions = "l" + permissions;
        } else if (isDirectory()) {
            permissions = "d" + permissions;
        } else {
            permissions = "-" + permissions;
        }

        return permissions;
    }

    /**
     * Gets the permissions as a string in the unix form of ls command. For Non-posix systems, this just displays the permissions for the user running the program
     *
     * @return the permissions as a string
     */
    @Override
    public String getPermissions() {
        return calculateLocalPermissions();
    }

    /**
     * Gets the modification time as a formatted String in the form of Month Day Hour:Minute, e.g Jan 01 12:50
     *
     * @return the formatted modification time String, can be null if can't be determined.
     */
    @Override
    public String getModificationTime() {
        long lastModified = lastModified();

        return new SimpleDateFormat(FileUtils.FILE_DATETIME_FORMAT).format(lastModified);
    }

    /**
     * Refreshes the file if the file implementation caches certain info. E.g a remote file may rather than making multiple calls to the server
     * No-op for a LocalFile
     */
    @Override
    public void refresh() {
        // no-op for LocalFile
    }

    /**
     * Checks if this file is a symbolic link. To determine what type of file the symbolic link points to, call isANormalFile or isADirectory
     *
     * @return true if it is a symbolic link
     */
    @Override
    public boolean isSymbolicLink() {
        return Files.isSymbolicLink(toPath());
    }

    /**
     * Gets the target of the symbolic link.
     *
     * @return the symbolic link target, null if not symbolic link
     * @throws FileSystemException if an error occurs
     */
    @Override
    public String getSymbolicLinkTarget() throws FileSystemException {
        if (isSymbolicLink()) {
            try {
                String path = Files.readSymbolicLink(toPath()).toString();

                return PathResolverFactory.newInstance()
                        .setLocal(FileUtils.getParentPath(getFilePath(), true)) // a symbolic link may be relative to the directory it's inside
                        .build()
                        .resolvePath(path);
            } catch (IOException | PathResolverException ex) {
                throw new FileSystemException("Failed to read symbolic link target");
            }
        } else {
            return null;
        }
    }

    /**
     * This file may be present locally or remotely. This method determines if it is local or remote
     *
     * @return true if local, false if remote
     */
    @Override
    public boolean isLocal() {
        return true;
    }
}
