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
import com.simpleftp.ui.UI;

import java.io.IOException;

/*
 * Represents a resolver for resolving local paths
 */
public class LocalPathResolver implements PathResolver {
    /**
     * The current working directory;
     */
    private final String currWorkingDir;

    /**
     * Constructs the LocalPathResolver. Outside the filesystem package, it is only instantiable from the PathResolverFactory
     * @param currWorkingDir the current working directory
     */
    protected LocalPathResolver(String currWorkingDir) {
        this.currWorkingDir = currWorkingDir;
    }

    /**
     * Adds the current working directory to the path.
     * @param path the path to be prepended
     * @return the full path
     */
    private String addPwdToPath(String path) {
        if (currWorkingDir.endsWith(UI.PATH_SEPARATOR)) {
            path = currWorkingDir + path;
        } else {
            path = currWorkingDir + UI.PATH_SEPARATOR + path; // the current working directory should be an absolute path
        }

        return path;
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
     * Resolves the specified path to an absolute, canonicalized path
     * @param path the path to resolve
     * @return the ResolvedPath object
     * @throws PathResolverException if an IOException occurs
     */
    @Override
    public ResolvedPath resolvePath(String path) throws PathResolverException {
        LocalFile file = new LocalFile(path);
        boolean absolute = file.isAbsolute();
        if (!absolute)
            path = addPwdToPath(path);

        try {
            if (!isPathCanonical(path)) {
                file = new LocalFile(path);
                path = file.getCanonicalPath();
            }

            return new ResolvedPath(path, absolute);
        } catch (IOException ex) {
            throw new PathResolverException(ex);
        }
    }
}
