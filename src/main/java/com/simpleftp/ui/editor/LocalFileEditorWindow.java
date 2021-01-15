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

package com.simpleftp.ui.editor;

import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.directories.DirectoryPane;
import com.simpleftp.ui.files.LineEntry;

/**
 * An editor window for local files
 */
public class LocalFileEditorWindow extends FileEditorWindow {

    /**
     * Constructs a FileEditorWindow with the specified panel and file
     *
     * @param creatingPane the DirectoryPane opening this window
     * @param fileContents the contents of the file as this class does not download the contents
     * @param lineEntry the line entry to edit
     */
    LocalFileEditorWindow(DirectoryPane creatingPane, String fileContents, LineEntry lineEntry) {
        super(creatingPane, fileContents, lineEntry);
    }

    /**
     * Gets the path of where to save the file to. If this file is a symbolic link, it is the target path, as uploading to the parent path of the link will break the link
     *
     * @return the path where to save the file
     */
    @Override
    String getSaveFilePath() throws Exception {
        CommonFile file = lineEntry.getFile();
        // the default option if it is an unknown file type
        if (file.isSymbolicLink()) {
            String targetPath = file.getSymbolicLinkTarget();

            /* we made DirectoryPane and DirectoryPane abstract to avoid this situation as if a new type of file is implemented, you only have to extend and create a type for that file, implementing abstract methods.
             * However here, you can only logically define symbolic links in a local file system or remote on the server. In a different file type for representing a different file that is not on the local or remote machine, symbolic links don't make sense
             * Since, this is such a small method, we can just change this method if absolutely necessary. WOuld be a bit extreme to make several different types of FileEditorWindow for one method. If you end up having more methods like this, do extract into sub-classes like DirectoryPane and DirectoryPane
             */
            return UI.resolveLocalPath(targetPath, creatingPane.getCurrentWorkingDirectory());
        }

        return file.getFilePath();
    }

    /**
     * Gets the file path to display as a title on the opened file window.
     * If this is remote, it should append what form of remote path it is, e.g. ftp
     *
     * @return the file path
     */
    @Override
    String getFilePath() {
        return lineEntry.getFilePath();
    }

    /**
     * Returns true if this editor window is remote, false if local
     *
     * @return true if remote, false if local
     */
    @Override
    public boolean isRemote() {
        return false;
    }
}
