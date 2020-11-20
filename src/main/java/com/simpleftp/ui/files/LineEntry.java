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

import com.simpleftp.FTPSystem;
import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ftp.exceptions.FTPRemotePathNotFoundException;
import com.simpleftp.local.exceptions.LocalPathNotFoundException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.panels.FilePanel;
import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.*;

import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.net.ftp.FTPFile;

import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This is an abstract class representing a line entry on the panel.
 * It can represent a Directory or File Line entry
 */
@Log4j2
public abstract class LineEntry extends HBox implements Comparable<LineEntry> {
    /**
     * The image icon for the line entry
     */
    private final ImageView image;
    /**
     * The file this line entry represents
     */
    @Getter
    protected CommonFile file;
    /**
     * The max width for a file name before it is shortened and ... is added to the end of it
     */
    private static final int FILE_NAME_LENGTH = 20;
    /**
     * The filePanel this LineEntry is a part of
     */
    @Getter
    @Setter
    private FilePanel owningPanel;

    /**
     * Creates a base LineEntry with the specified image URL (which is assumed to be in the jar), file and panel
     * @param imageURL the URL to the image (assumed to be in the JAR)
     * @param file the file this LineEntry represents
     * @param owningPanel the FilePanel this LineEntry is a part of
     * @throws FTPRemotePathNotFoundException if it is a remote file and cant be found
     * @throws LocalPathNotFoundException if it is a local file and cant be found
     */
    protected LineEntry(String imageURL, CommonFile file, FilePanel owningPanel) throws FTPRemotePathNotFoundException, LocalPathNotFoundException, FileSystemException {
        setSpacing(30);
        setStyle(UI.WHITE_BACKGROUND);
        /**
         * The pane containing the image and name
         */
        HBox imageNamePanel = new HBox();
        imageNamePanel.setSpacing(30);
        image = new ImageView();
        image.setFitWidth(UI.FILE_ICON_SIZE);
        image.setFitHeight(UI.FILE_ICON_SIZE);
        image.setImage(new Image(imageURL));
        this.file = file;
        imageNamePanel.getChildren().add(image);

        this.owningPanel = owningPanel;

        init();
    }

    /**
     * Initialises the line entry
     * @throws FTPRemotePathNotFoundException if the file is remote and cant be found
     * @throws LocalPathNotFoundException if the file is local and cant be found
     */
    protected void init() throws FTPRemotePathNotFoundException, LocalPathNotFoundException, FileSystemException {
        if (getChildren().size() > 1) {
            getChildren().clear();
        }

        setAlignment(Pos.CENTER_LEFT);
        String fileNameString = getFileNameString();
        Text text = new Text(fileNameString);
        if (fileNameString.contains("...")) {
            Tooltip t = new Tooltip(file.getName());
            Tooltip.install(text, t);
        }
        text.setFont(Font.font("Monospaced"));
        getChildren().add(image);
        getChildren().add(text);

        setOnMouseEntered(e -> setStyle(UI.GREY_BACKGROUND_TRANSPARENT));
        setOnMouseExited(e -> setStyle(UI.WHITE_BACKGROUND));
        setOnMouseClicked(e -> {
            if (!e.isConsumed()) {
                if (e.getClickCount() == 1) {
                    owningPanel.click(this);
                } else {
                    owningPanel.openLineEntry(this);
                }
            }
            owningPanel.requestFocus();
        });
        setPickOnBounds(true);
    }

    /**
     * Retrieves the modification time and size of the file. Note that if ftpConnection.getModificationTime fails, the file.getTimestamp is used even if it is not very accurate
     * @return modification time or Cannot be determined
     * @throws FTPRemotePathNotFoundException if file is remote and cant be found
     * @throws LocalPathNotFoundException if file is local and cant be found
     */
    public String getModificationTimeAndSize() throws FTPRemotePathNotFoundException, LocalPathNotFoundException {
        String modificationTime = "";
        if (file instanceof LocalFile) {
            LocalFile localFile = (LocalFile)file;
            if (localFile.exists()) {
                modificationTime = localFile.length() + " " + new SimpleDateFormat(UI.FILE_DATETIME_FORMAT).format(localFile.lastModified());
            } else if (!Files.isSymbolicLink(localFile.toPath())){
                throw new LocalPathNotFoundException("The file no longer exists", file.getFilePath());
            }
        } else {
            FTPConnection connection = FTPSystem.getConnection();
            if (connection != null) {
                try {
                    String filePath = file.getFilePath();
                    String fileModTime = connection.getModificationTime(filePath);
                    if (fileModTime != null) {
                        LocalDateTime dateTime = LocalDateTime.parse(fileModTime, DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
                        modificationTime = dateTime.format(DateTimeFormatter.ofPattern(UI.FILE_DATETIME_FORMAT));
                        modificationTime = connection.getFileSize(filePath) + " " + modificationTime;
                    } else {
                        RemoteFile remoteFile = (RemoteFile)file;
                        FTPFile ftpFile = remoteFile.getFtpFile();
                        if (ftpFile.isValid()) {
                            return ftpFile.getSize() + " " + UI.parseCalendarToUITime(ftpFile.getTimestamp());
                        }

                        log.warn("Could not determine modification time for file {}", filePath);
                        return "Cannot be determined";
                    }
                } catch (FTPException ex) {
                    ex.printStackTrace();
                    if (ex instanceof FTPRemotePathNotFoundException) {
                        throw (FTPRemotePathNotFoundException)ex;
                    }
                }
            }
        }

        return modificationTime;
    }

    /**
     * Gets the file name with modification time size and permissions
     * @return String of file name, size, modification time, permissions
     * @throws FTPRemotePathNotFoundException if a remote file and not found
     * @throws LocalPathNotFoundException if a local file and not found
     */
    private String getFileNameString() throws FTPRemotePathNotFoundException, LocalPathNotFoundException, FileSystemException {
        String fileName = file.getName();
        String paddedName = ""; // the name that will be displayed on the UI.

        if (fileName.length() > FILE_NAME_LENGTH) {
            paddedName = fileName.substring(0, FILE_NAME_LENGTH - 3) + "...";
        } else {
            paddedName = fileName;
            for (int i = fileName.length(); i < FILE_NAME_LENGTH; i++) {
                paddedName += " ";
            }
        }

        paddedName += "\t\t" + getModificationTimeAndSize() + " " + calculatePermissionsString();

        return paddedName.trim();
    }

    /**
     * Calculates the permissions for the file
     * @return permissions string for the file
     * @throws FTPRemotePathNotFoundException if a remote file and not found
     * @throws LocalPathNotFoundException if a local file and not found
     */
    protected String calculatePermissionsString() throws FTPRemotePathNotFoundException, LocalPathNotFoundException, FileSystemException {
        String permissions = "";

        if (file instanceof LocalFile) {
            LocalFile localFile = (LocalFile)file;

            if (!localFile.exists()) {
                throw new LocalPathNotFoundException("The file no longer exists", localFile.getFilePath());
            }

            if (Files.isSymbolicLink(localFile.toPath())) {
                permissions += "l";
            } else if (localFile.isADirectory()) {
                permissions += "d";
            } else {
                permissions += "-";
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
                if (!remoteFile.exists()) {
                    throw new FTPRemotePathNotFoundException("The file no longer exists", remoteFile.getFilePath());
                } else {
                    FTPFile file = remoteFile.getFtpFile();

                    if (file.isSymbolicLink()) {
                        permissions += "l";
                    } else if (file.isDirectory()) {
                        permissions += "d";
                    } else {
                        permissions += "-";
                    }

                    if (file.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION)) {
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

                    if (file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.READ_PERMISSION)) {
                        permissions += "r";
                    } else {
                        permissions += "-";
                    }

                    if (file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.WRITE_PERMISSION)) {
                        permissions += "w";
                    } else {
                        permissions += "-";
                    }

                    if (file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.EXECUTE_PERMISSION)) {
                        permissions += "x";
                    } else {
                        permissions += "-";
                    }
                }
            } catch (FTPException ex) {
                ex.printStackTrace();
                if (ex instanceof FTPRemotePathNotFoundException) {
                    throw (FTPRemotePathNotFoundException)ex;
                }
            }
        }

        return permissions;
    }

    @Override
    public int compareTo(LineEntry other) {
        String n1 = this.file.getName();
        String n2 = other.file.getName();

        int n1Ind = n1.lastIndexOf(".");
        if (n1Ind != -1) {
            n1 = n1.substring(0, n1Ind);
        }

        int n2Ind = n2.lastIndexOf(".");
        if (n2Ind != -1) {
            n2 = n2.substring(0, n2Ind);
        }
        // we want to compare without extensions
        return n1.compareTo(n2);
    }
}
