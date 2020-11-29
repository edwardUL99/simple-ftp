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

package com.simpleftp.filesystem.paths;

import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.exceptions.PathResolverException;
import com.simpleftp.filesystem.paths.interfaces.PathResolver;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ftp.exceptions.FTPRemotePathNotFoundException;
import com.simpleftp.ui.UI;

import java.io.File;

/**
 * This class represents a PathResolver for resolving remote paths
 */
public class RemotePathResolver implements PathResolver {
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
    protected RemotePathResolver(FTPConnection connection, String currWorkingDir, boolean pathExists) throws IllegalArgumentException {
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
        String fileName = new LocalFile(path).getName();
        return !path.contains("/..") && !path.contains("../")
                && !path.contains("/.") && !path.contains("./")
                && !path.equals("..") && !path.equals(".")
                && !fileName.equals(".") && !fileName.equals("..");
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
     * Adds the current working directory to the path.
     * @param path the path to be prepended
     * @return the full path
     */
    private String addPwdToPath(String path) {
        if (currWorkingDir.endsWith("/")) {
            path = currWorkingDir + path;
        } else {
            path = currWorkingDir + "/" + path;
        }

        return path;
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

        String workingDir = isFile ? UI.getParentPath(path):path;
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
        if (path.startsWith("./"))
            path = path.substring(2); // if it starts with ./, remove it and make the method look at this as a file starting in pwd
        boolean absolute = isPathAbsolute(path);
        if (!absolute)
            path = addPwdToPath(path);
        boolean canonical = isPathCanonical(path);

        try {
            if (!canonical) {
                path = canonicalizeRemotePath(path);
            }

            return path;
        } catch (FTPException ex) {
            throw new PathResolverException(ex);
        }
    }
}
