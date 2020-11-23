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

import com.simpleftp.filesystem.interfaces.CommonFile;

import java.io.File;

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
        return super.isDirectory();
    }

    @Override
    public boolean isNormalFile() {
        return super.isFile();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
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
        } else if (this.hashCode() == obj.hashCode()) {
            return true;
        } else {
            LocalFile localFile = (LocalFile)obj;

            return localFile.getFilePath().equals(localFile.getFilePath());
        }
    }

    /**
     * Returns the size in bytes of the file
     *
     * @return size in bytes of the file
     */
    @Override
    public long getSize() {
        return length();
    }
}
