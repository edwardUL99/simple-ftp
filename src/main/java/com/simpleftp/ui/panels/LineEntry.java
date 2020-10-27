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
import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.*;

import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import lombok.Getter;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This is an abstract class representing a line entry on the panel.
 * It can represent a Directory or File Line entry
 */
public abstract class LineEntry extends HBox {
    private ImageView image;
    @Getter
    protected CommonFile file;

    private static int FILE_NAME_LENGTH = 20;

    private HBox imageNamePanel = new HBox();

    protected LineEntry(String imageURL, CommonFile file) throws FTPRemotePathNotFoundException, LocalPathNotFoundException{
        setSpacing(30);
        //setBorder(new Border(new BorderStroke(Paint.valueOf("BLACK"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        //setMaxHeight(5);

        imageNamePanel.setSpacing(30);

        //HBox.setHgrow(this, Priority.ALWAYS);

        image = new ImageView();
        image.setImage(new Image(ClassLoader.getSystemResourceAsStream(imageURL)));
        this.file = file;
        imageNamePanel.getChildren().add(image);

        init();
    }

    /*private StackPane getModificationPane() {
        StackPane modificationPane = new StackPane();
        modificationPane.setPadding(new Insets(0,5,0,5));
        Text text = new Text(getModificationTime());
        modificationPane.getChildren().add(text);
        return modificationPane;
    }

    private StackPane getPermissionsPane() {
        StackPane permissionsPane = new StackPane();
        permissionsPane.setPadding(new Insets(0,5,0,5));
        Text permissions = new Text(calculatePermissionsString());
        permissions.setFont(Font.font("Monospaced"));
        permissionsPane.getChildren().add(permissions);
        permissionsPane.setMinWidth(100);
        return permissionsPane;
    }

    private void setFileNamePanel() {
        String fileNameString = getFileNameString();
        Text text = new Text();
        text.setText(fileNameString);
        text.setFont(Font.font("Monospaced"));
        if (fileNameString.contains("...")) {
            Tooltip tooltip = new Tooltip(file.getName());
            Tooltip.install(text, tooltip);
        }
        imageNamePanel.setAlignment(Pos.CENTER_LEFT);
        imageNamePanel.getChildren().add(text);
        setLeft(imageNamePanel);
    }

    private void setInfoPanel() {
        HBox rightPanel = new HBox();
        rightPanel.getChildren().addAll(getModificationPane(), getPermissionsPane());

        setRight(rightPanel);
    }*/

    protected void init() throws FTPRemotePathNotFoundException, LocalPathNotFoundException{
        if (getChildren().size() > 1) {
            //setRight(null);
            getChildren().clear();
        }

        //setFileNamePanel();
        //setInfoPanel();
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
    }

    private String getModificationTime() throws FTPRemotePathNotFoundException, LocalPathNotFoundException {
        String modificationTime = "";
        if (file instanceof LocalFile) {
            LocalFile localFile = (LocalFile)file;
            if (localFile.exists()) {
                modificationTime = new SimpleDateFormat("MMM dd HH:mm").format(localFile.lastModified());
            } else {
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
                        modificationTime = dateTime.format(DateTimeFormatter.ofPattern("MMM dd HH:mm"));
                    } else {
                        throw new FTPRemotePathNotFoundException("The file no longer exists", filePath);
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

    private String getFileNameString() throws FTPRemotePathNotFoundException, LocalPathNotFoundException {
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

        paddedName += "\t " + getModificationTime() + " " + calculatePermissionsString();

        return paddedName.trim();
    }

    protected abstract String calculatePermissionsString() throws FTPRemotePathNotFoundException, LocalPathNotFoundException;

    /**
     * Refreshes the line entry
     * @throws FTPRemotePathNotFoundException if it is refreshing a remote path and it is not found
     * @throws LocalPathNotFoundException if it is refreshing a local path and it is not found
     */
    public abstract void refresh() throws FTPRemotePathNotFoundException, LocalPathNotFoundException;

    protected String getFilePath() {
        String filePath = file.getFilePath();

        if (file instanceof RemoteFile) {
            filePath = "ftp://" + filePath;
        }

        return filePath;
    }
}
