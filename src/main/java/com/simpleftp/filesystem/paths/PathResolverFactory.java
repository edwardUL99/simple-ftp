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

import com.simpleftp.filesystem.exceptions.PathResolverConfigurationException;
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
     * The type of resolver to build
     */
    private ResolverType resolverType;
    /**
     * The current working directory
     */
    private String currWorkingDir;
    /**
     * The path separator to use if using a symbolic path resolver
     */
    private String pathSeparator;
    /**
     * The path root if required when using a symbolic path resolver
     */
    private String root;
    /**
     * An enum used to identify which type of resolver to build
     */
    private enum ResolverType {
        LOCAL,
        REMOTE,
        SYMBOLIC
    }

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
     * @throws PathResolverConfigurationException if the currWorkingDir is null or empty
     */
    public PathResolverFactory setLocal(String currWorkingDir) {
        this.resolverType = ResolverType.LOCAL;
        if (currWorkingDir == null || currWorkingDir.isEmpty())
            throw new PathResolverConfigurationException("The working directory cannot be null");
        this.currWorkingDir = currWorkingDir;
        return this;
    }

    /**
     * Sets this factory to build a remote path resolver with the specified current working directory
     * @param currWorkingDir the current working directory
     * @param connection the connection to use for resolving paths, expected to be connected and logged in and not null
     * @param pathExists true if the path already exists, false if it is to be created
     * @return this instance for chaining
     * @throws PathResolverConfigurationException if currWorkingDir is null or empty or connection is null or not connected and logged in
     */
    public PathResolverFactory setRemote(String currWorkingDir, FTPConnection connection, boolean pathExists) {
        this.resolverType = ResolverType.REMOTE;
        if (currWorkingDir == null || currWorkingDir.isEmpty())
            throw new PathResolverConfigurationException("The working directory cannot be null");

        if (connection == null || (!connection.isConnected() && !connection.isLoggedIn()))
            throw new PathResolverConfigurationException("The provided connection must be connected and logged in, in order to be able to resolve paths");
        this.currWorkingDir = currWorkingDir;
        this.connection = connection;
        this.pathExists = pathExists;
        return this;
    }

    /**
     * Sets this factory to build a SymbolicPathResolverFactory
     * @param pathSeparator the path separator to use
     * @param root the root to use if required, null if not. E.g. may need to add on C:\
     * @return this instance for chaining
     * @throws PathResolverConfigurationException if pathSeparator is null or empty
     */
    public PathResolverFactory setSymbolic(String pathSeparator, String root) {
        this.resolverType = ResolverType.SYMBOLIC;
        if (pathSeparator == null || pathSeparator.isEmpty())
            throw new PathResolverConfigurationException("The provided path separator cannot be null or empty");
        this.pathSeparator = pathSeparator;
        this.root = root;
        return this;
    }

    /**
     * Builds the PathResolver object
     * @return the resolved path object
     * @throws PathResolverConfigurationException if a configuration exception occurs
     */
    public PathResolver build() throws PathResolverConfigurationException {
       switch (resolverType) {
           case LOCAL: return new LocalPathResolver(currWorkingDir);
           case REMOTE: return new RemotePathResolver(connection, currWorkingDir, pathExists);
           case SYMBOLIC: return new SymbolicPathResolver(pathSeparator, root);
           default: throw new PathResolverConfigurationException("Undefined PathResolver type provided");
       }
    }

}
