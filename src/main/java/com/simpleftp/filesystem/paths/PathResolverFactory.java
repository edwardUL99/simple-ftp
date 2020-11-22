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

import com.simpleftp.filesystem.paths.interfaces.PathResolver;
import com.simpleftp.ftp.connection.FTPConnection;

/**
 * A factory for creating a PathResolver
 */
public class PathResolverFactory {
    /**
     * The connection to use if remote
     */
    private FTPConnection connection;
    /**
     * If remote, this is required
     */
    private boolean pathExists;
    /**
     * True if building a local path resolver, false if remote
     */
    private boolean local;
    /**
     * The current working directory
     */
    private String currWorkingDir;

    /**
     * Don't allow instantiation using constructor
     */
    private PathResolverFactory() {}

    /**
     * Constructs a new factory
     * @return returns the created factory
     */
    public static PathResolverFactory newInstance() {
        return new PathResolverFactory();
    }

    /**
     * Sets this factory to build a local path resolver with the specified current working directory
     * @param currWorkingDir the current working directory
     * @return the instance of the factory for chaining
     */
    public PathResolverFactory setLocal(String currWorkingDir) {
        this.local = true;
        this.currWorkingDir = currWorkingDir;
        return this;
    }

    /**
     * Sets this factory to build a remote path resolver with the specified current working directory
     * @param currWorkingDir the current working directory
     * @param connection the connection to use for resolving paths, expected to be connected and logged in and not null
     * @param pathExists true if the path already exists, false if it is to be created
     * @return this instance for chaining
     */
    public PathResolverFactory setRemote(String currWorkingDir, FTPConnection connection, boolean pathExists) {
        this.local = false;
        this.currWorkingDir = currWorkingDir;
        this.connection = connection;
        this.pathExists = pathExists;
        return this;
    }

    /**
     * Builds the PathResolver object
     * @return the resolved path object
     */
    public PathResolver build() {
        if (local) {
            return new LocalPathResolver(currWorkingDir);
        } else {
            return new RemotePathResolver(connection, currWorkingDir, pathExists);
        }
    }

}
