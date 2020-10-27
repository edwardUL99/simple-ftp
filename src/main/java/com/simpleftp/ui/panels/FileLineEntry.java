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

    public FileLineEntry(CommonFile file) throws FTPRemotePathNotFoundException, LocalPathNotFoundException {
        super("file_icon.png", file);
    }

    public void setActionMenu(EventHandler<ActionEvent> actionHandler) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(actionHandler);
        contextMenu.getItems().add(delete);
        setOnContextMenuRequested(e -> {
            contextMenu.show(this, e.getScreenX(), e.getScreenY());
        });
    }

    protected String calculatePermissionsString() throws LocalPathNotFoundException {
        String permissions = "f";

        if (file instanceof LocalFile) {
            LocalFile localFile = (LocalFile)file;

            if (!localFile.exists()) {
                throw new LocalPathNotFoundException("The file no longer exists", localFile.getFilePath());
            }

            if (localFile.canRead()) {
                permissions += "r";
            } else {
                permissions += "-";
            }

            if (localFile.canWrite()) {
                permissions += "-w";
            } else {
                permissions += "--";
            }

            if (localFile.canExecute()) {
                permissions += "-x";
            } else {
                permissions += "--";
            }
        } else {
            RemoteFile remoteFile = (RemoteFile)file;
            try {
                String filePath = remoteFile.getFilePath();
                FTPFile file = FTPSystem.getConnection().getFTPFile(filePath);
                if (file == null) {
                    throw new FTPRemotePathNotFoundException("The file no longer exists", filePath);
                } else {
                    String raw = file.getRawListing();
                    permissions += raw.substring(1, raw.indexOf(" "));
                    /*if (file.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION)) {
                        permissions += "r";
                    } else {
                        permissions += "-";
                    }

                    if (file.hasPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION)) {
                        permissions += "w";
                    } else {
                        permissions += "-";
                    }

                    if (file.hasPermission(FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION)) {
                        permissions += "x";
                    } else {
                        permissions += "-";
                    }

                    permissions += "-";

                    if (file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.READ_PERMISSION)) {
                        permissions += "r";
                    } else {
                        permissions += "-";
                    }

                    if (file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.WRITE_PERMISSION)) {
                        permissions += "w";
                    } else {
                        permissions += "-";
                    }

                    if (file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.EXECUTE_PERMISSION)) {
                        permissions += "x";
                    } else {
                        permissions += "-";
                    }

                    permissions += "-";

                    if (file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.READ_PERMISSION)) {
                        permissions += "r";
                    } else {
                        permissions += "-";
                    }

                    if (file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.WRITE_PERMISSION)) {
                        permissions += "w";
                    }

                    if (file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.EXECUTE_PERMISSION)) {
                        permissions += "x";
                    } else {
                        permissions += "-";
                    }*/
                }
            } catch (FTPException ex) {
                ex.printStackTrace();
            }
        }

        return permissions;//StringUtils.leftPad(permissions, 20);
    }

    public void refresh() throws FTPRemotePathNotFoundException, LocalPathNotFoundException {
        init();
    }
}
