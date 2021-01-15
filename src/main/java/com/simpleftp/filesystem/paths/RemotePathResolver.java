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

package com.simpleftp.filesystem.paths;

import com.simpleftp.filesystem.FileUtils;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.exceptions.PathResolverException;
import com.simpleftp.filesystem.paths.interfaces.PathResolver;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ftp.exceptions.FTPRemotePathNotFoundException;

import java.io.File;
import java.util.List;

/**
 * This class represents a PathResolver for resolving remote paths
 */
class RemotePathResolver implements PathResolver {
    /**
     * The connection to use for resolving paths
     */
    private final FTPConnection connection;
    /**
     * True if the path being resolved should already exist, false if not
     */
    private final boolean pathExists;
    /**
     * The current working directory
     */
    private final String currWorkingDir;

    /**
     * Constructs a RemotePathResolver for resolving remote paths. Needs to be created using factory outside of filesystem package
     * @param connection the connection to be used to resolve paths
     * @param currWorkingDir the current working directory
     * @param pathExists true if the path should already exist, false if it is to be created
     * @throws IllegalArgumentException if the connection is not ready (i.e. not connected and logged in)
     */
    RemotePathResolver(FTPConnection connection, String currWorkingDir, boolean pathExists) throws IllegalArgumentException {
        this.connection = connection;
        this.currWorkingDir = currWorkingDir;
        this.pathExists = pathExists;

        if (!(connection.isConnected() && connection.isLoggedIn())) {
            throw new IllegalArgumentException("The provided connection must be connected and logged in");
        }
    }

    /**
     * Checks if the path is canonical
     * @param path the path to check
     * @return true if canonical
     */
    private boolean isPathCanonical(String path) {
        List<String> splitPath = FileUtils.splitPath(path, false);
        return !splitPath.contains(".") && !splitPath.contains("..");
    }

    /**
     * Checks if the provided path is absolute or not
     * @param path the path to check
     * @return true if absolute, false if not
     */
    private boolean isPathAbsolute(String path) {
        return path.startsWith("/");
    }

    /**
     * Converts the remote path to a canonical version. It is assumed the path is not canonical before hand.
     * @param path the path to convert
     * @return the absolute path version of path
     * @throws FTPException if any FTPConnection methods throw an exception
     */
    private String canonicalizeRemotePath(String path) throws FTPException {
        String fileName = new File(path).getName();

        boolean isFile = true;
        if (pathExists)
            isFile = connection.remotePathExists(path, false);

        String workingDir = isFile ? FileUtils.getParentPath(path, false):path;
        if (!connection.changeWorkingDirectory(workingDir)) { // allow the connection to resolve the . or .. by physically following the links
            throw new FTPRemotePathNotFoundException("The path " + workingDir + " does not exist", workingDir);
        }
        path = connection.getWorkingDirectory();
        connection.changeWorkingDirectory(currWorkingDir); // change back
        boolean appendFileName = false;

        if (!fileName.equals(".") && !fileName.equals("..")) {
            if (pathExists) {
                appendFileName = isFile;
            } else {
                appendFileName = true;
            }
        }

        if (appendFileName) {
            if (path.endsWith("/")) {
                path += fileName;
            } else {
                path += "/" + fileName;
            }
        }

        return path;
    }

    /**
     * Resolves the specified path to an absolute, canonicalized path
     *
     * @param path           the path to resolve
     * @return the resolved path
     * @throws PathResolverException if a FTPException occurs
     */
    @Override
    public String resolvePath(String path) throws PathResolverException {
        boolean absolute = isPathAbsolute(path);
        if (!absolute)
            path = FileUtils.addPwdToPath(currWorkingDir, path, "/");
        boolean canonical = isPathCanonical(path);

        try {
            RemoteFile file = connection == FTPSystem.getConnection() ? new RemoteFile(path):new RemoteFile(path, connection, null);
            if (!canonical || file.isSymbolicLink()) {
                path = canonicalizeRemotePath(path); // if the path doesn't contain . or .. but is a symbolic link, it's not canonical
            }

            return path;
        } catch (FileSystemException ex) {
            throw new PathResolverException((Exception)ex.getCause());
        } catch (FTPException ex) {
            throw new PathResolverException(ex);
        }
    }
}
