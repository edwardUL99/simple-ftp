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
import com.simpleftp.filesystem.paths.interfaces.PathResolver;

import java.util.ArrayList;

/**
 * This is a PathResolver implementation which does a "loose" canonicalization of a path.
 * What is meant by this is that it gets rid of all .. and . characters but doesn't need the path to exist.
 * It also preserves following a path "symbolically", i.e. doesn't go to the target of the link, instead goes to the link as if it was a real directory at that location.
 * On linux if you cd into a symbolic link, it follows it rather than going to the target (that is the -L flag of the cd command (default)).
 * The Local and Remote PathResolvers behave as the -P option of cd command on linux (goes to targets).
 *
 * This resolver expects the path to be already absolute
 */
public class SymbolicPathResolver implements PathResolver {
    /**
     * The path separator to use when building strings
     */
    private final String pathSeparator;

    /**
     * The root to use at the start of the path.
     * This shouldn't be null
     */
    private final String root;

    /**
     * Constructs a symbolic path resolver
     * @param pathSeparator the path separator to use
     * @param root the root at the start of the path
     */
    protected SymbolicPathResolver(String pathSeparator, String root) {
        this.pathSeparator = pathSeparator;
        this.root = root;
    }

    /**
     * Removes root if not null and present and splits the path into it's separate components
     * @param path the path to split
     * @return the array containing the split components
     */
    private String[] splitPath(String path) {
        if (root != null && !root.equals("/") && path.startsWith(root))
            path = path.substring(root.length() - 1);
        else if (path.startsWith(pathSeparator))
            path = path.substring(1);
        String regex = pathSeparator.equals("\\") ? "\\\\":pathSeparator;

        return path.split(regex);
    }

    /**
     * Parses the path into components and returns the list of components
     * @param path the path to parse. Should be absolute
     * @return the list of parsed path components to be built
     */
    private ArrayList<String> parsePath(String path) {
        ArrayList<String> pathComponents = new ArrayList<>();
        String[] components = splitPath(path);

        for (String s : components) {
            if (s.equals("..")) {
                int size = pathComponents.size();
                if (size != 0) {
                    pathComponents.remove(size - 1);// .. means go to parent so remove the last added path component as the one before that component was the parent
                }
            } else if (!s.equals(".")) { // if it is equals to ., don't bother add it as it is the current directory
                pathComponents.add(s);
            }
        }

        return pathComponents;
    }

    /**
     * Builds the path from the given list of path components
     * @param pathComponents the components of the path to construct
     * @return the built path
     */
    private String buildPath(ArrayList<String> pathComponents) {
        StringBuilder path;
        if (root != null) {
            path = new StringBuilder(root);
        } else {
            path = new StringBuilder(pathSeparator); // initialise with path separator
        }

        pathComponents.forEach(e -> path.append(e).append(pathSeparator));
        int pathLength = path.length();

        if (pathLength > 1 && path.charAt(pathLength - 1) == pathSeparator.charAt(0))
            path.deleteCharAt(path.length() - 1); // remove last path separator

        return path.toString();
    }

    /**
     * Resolves the specified path to an absolute "loosely" canonicalized path.
     * As described in the class javadoc header, this is a "loose" canonicalization
     *
     * @param path the path to resolve. For SymbolicPathResolver, the path must be absolute
     * @return the resolved path string
     */
    @Override
    public String resolvePath(String path) {
        if (!path.startsWith("/") || !(new LocalFile(path)).isAbsolute()) {
            throw new IllegalArgumentException("The path " + path + " is not absolute. SymbolicPathResolver expects an absolute path");
        }

        ArrayList<String> pathComponents = parsePath(path);
        return buildPath(pathComponents);
    }
}
