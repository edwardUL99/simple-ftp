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

package com.simpleftp.ui.files;

import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ui.directories.DirectoryPane;

/**
 * A line entry implementation for a File.
 */
public final class FileLineEntry extends LineEntry {
    /**
     * Constructs a FileLineEntry
     * @param file the file that this entry represents. It is not this class' responsibility to ensure file is a normal file
     * @param owningPanel the panel owning this entry, i.e. the panel it is on
     */
    FileLineEntry(CommonFile file, DirectoryPane owningPanel) throws FileSystemException {
        super(file.isSymbolicLink() ? "file_icon_symlink.png":"file_icon.png", file, owningPanel);
        initDragAndDrop();
    }

    /**
     * Can be used to determine the type of this LineEntry
     *
     * @return true if the file this LineEntry represents is a file (could be a symlink too)
     */
    @Override
    public boolean isFile() {
        return true;
    }

    /**
     * Can be used to determine the type of this LineEntry
     *
     * @return true if the file this LineEntry represents is a directory (could be a symlink too)
     */
    @Override
    public boolean isDirectory() {
        return false;
    }

    /**
     * This method initialises drag and drop
     */
    private void initDragAndDrop() {
        setOnMouseDragged(e -> e.setDragDetect(false));
        setOnDragDetected(e -> dragStarted());
    }

    /**
     * Returns the hash code of the file behind this instance
     * @return hash code for this directory line entry
     */
    @Override
    public int hashCode() {
        return file.hashCode();
    }

    /**
     * Determines if this DirectoryLineEntry is equal to another.
     * Does equality based on the backing file
     * @param obj the object to check
     * @return true if equal, false if not.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FileLineEntry)) {
            return false;
        } else if (obj == this) {
            return true;
        } else {
            FileLineEntry fileLineEntry = (FileLineEntry)obj;

            return file.equals(fileLineEntry.file);
        }
    }
}
