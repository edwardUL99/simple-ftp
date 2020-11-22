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

/**
 * This class represents a path that has been resolved by a PathResolver.
 * It consists of a path and a boolean value. The path is a canonical path and the boolean value represents if the path passed in was already absolute (can be canonicalized but doesn't need to be)
 */
public class ResolvedPath {
    /**
     * The resolved path
     */
    private String resolvedPath;
    /**
     * Flag indicating if the path passed in was already absolute
     */
    private boolean pathAlreadyAbsolute;

    /**
     * Creates a resolved path object
     * @param resolvedPath the path that was resolved
     * @param pathAlreadyAbsolute the flag indicating that the path passed into PathResolver.resolvePath was already absolute
     */
    public ResolvedPath(String resolvedPath, boolean pathAlreadyAbsolute) {
        this.resolvedPath = resolvedPath;
        this.pathAlreadyAbsolute = pathAlreadyAbsolute;
    }

    /**
     * Gets the resolved path
     * @return resolved path
     */
    public String getResolvedPath() {
        return resolvedPath;
    }

    /**
     * Returns whether the path passed into resolvePath was already absolute
     * @return true if the path was already absolute, false if not
     */
    public boolean isPathAlreadyAbsolute() {
        return pathAlreadyAbsolute;
    }

}
