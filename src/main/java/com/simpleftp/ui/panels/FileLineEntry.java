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

package com.simpleftp.ui.panels;

import com.simpleftp.FTPSystem;
import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ftp.connections.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ftp.exceptions.FTPRemotePathNotFoundException;
import com.simpleftp.local.exceptions.LocalPathNotFoundException;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ContextMenuEvent;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;

/**
 * A line entry implementation for a File.
 */
public class FileLineEntry extends LineEntry {

    /**
     * Constructs a FileLineEntry
     * @param file the file that this entry represents. It is not this class' responsibility to ensure file is a normal file
     * @param owningPanel the panel owning this entry, i.e. the panel it is on
     * @throws FTPRemotePathNotFoundException
     * @throws LocalPathNotFoundException
     */
    public FileLineEntry(CommonFile file, FilePanel owningPanel) throws FTPRemotePathNotFoundException, LocalPathNotFoundException {
        super("file_icon.png", file, owningPanel);
    }
}
