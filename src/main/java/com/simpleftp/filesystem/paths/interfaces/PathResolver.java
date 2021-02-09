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

package com.simpleftp.filesystem.paths.interfaces;

import com.simpleftp.filesystem.exceptions.PathResolverException;

/**
 * This interface provides a interface for resolving paths.
 *
 * It is up to the implementing classes to have methods to set any flags/fields that the resolvePath method requires.
 * An example, remote path resolving requires an ready FTPConnection and a flag pathExists which determines if the path to be resolved doesn't exist yet or true if it does exist (for go to)
 *
 * These should be set using setters. If any flag isn't set, either leave as default false or throw an exception (i.e. have object Boolean and if null, throw). Fields like connection, if null, throw exception or revert to FTPSystem.getConnection
 *
 * This PathResolverException wraps any FTPException or IOException that can occur.
 *
 * A LocalPathResolver is a much simpler implementation than a RemotePathResolver.
 */
public interface PathResolver {
    /**
     * Resolves the specified path to what the implementation defines a "resolved" path
     * @param path the path to resolve
     * @return the resolved path string
     * @throws PathResolverException if an IOException or FTPException occurs
     */
    String resolvePath(String path) throws PathResolverException;
}
