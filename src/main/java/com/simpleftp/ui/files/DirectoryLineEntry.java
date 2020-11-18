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
import com.simpleftp.ftp.exceptions.FTPRemotePathNotFoundException;
import com.simpleftp.local.exceptions.LocalPathNotFoundException;
import com.simpleftp.ui.panels.FilePanel;

/**
 * A line entry implementation for a Directory.
 */
public class DirectoryLineEntry extends LineEntry {

    /**
     * Constructs Directory LineEntry
     * @param file the file that this entry represents. It is not this class' responsibility to ensure file is a directory
     * @param owningPanel the panel owning this entry, i.e. the panel it is on
     * @throws FTPRemotePathNotFoundException
     * @throws LocalPathNotFoundException
     */
    public DirectoryLineEntry(CommonFile file, FilePanel owningPanel) throws FTPRemotePathNotFoundException, LocalPathNotFoundException, FileSystemException {
        super("dir_icon.png", file, owningPanel);
    }
}
