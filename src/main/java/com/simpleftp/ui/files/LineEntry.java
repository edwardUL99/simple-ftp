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
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.directories.DirectoryPane;
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

/**
 * This is an abstract class representing a line entry on the panel.
 * DirectoryPane essentially displays a listing of the files in the current directory.
 * Each file on the panel can be represented as an entry of the file listing.
 * Since all the info for the file is laid out as a line by line basis on the panel, this class will be called a LineEntry
 * It can represent a Directory or File Line entry
 *
 * To create a LineEntry for a File, pass in a CommonFile that is a normal file and for a Directory, a CommonFile that is a directory to LineEntry.newInstance
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
     * The directoryPane this LineEntry is a part of
     */
    @Getter
    @Setter
    private DirectoryPane owningPanel;
    /**
     * Stores the retrieved file size so you are not making constant calls to the FTPConnection for opening property window etc
     */
    private Long fileSize;
    /**
     * Stores the retrieved modification time so you are not making constant calls to the FTPConnection
     */
    private String modificationTime;
    /**
     * If this file is a symbolic link, store the target on initial resolving, to cache it so we don't have to keep resolving the path
     */
    private String symLinkTarget;

    /**
     * Creates a base LineEntry with the specified image URL (which is assumed to be in the jar), file and panel
     * @param imageURL the URL to the image (assumed to be in the JAR)
     * @param file the file this LineEntry represents
     * @param owningPanel the DirectoryPane this LineEntry is a part of
     */
    protected LineEntry(String imageURL, CommonFile file, DirectoryPane owningPanel) throws FileSystemException {
        setSpacing(30);
        setStyle(UI.WHITE_BACKGROUND);
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
     */
    protected void init() throws FileSystemException {
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
        getChildren().addAll(image, text);

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
     * Retrieves the modification time string of the file. Note that if ftpConnection.getModificationTime fails, the file.getTimestamp is used even if it is not very accurate
     * @return modification time or Cannot be determined
     */
    public String getModificationTime() throws FileSystemException {
        if (modificationTime == null) {
            modificationTime = file.getModificationTime();
            if (modificationTime == null)
                modificationTime = "Cannot be determined";
        }

        return modificationTime;
    }

    /**
     * Gets the size of the file behing this LineEntry
     * @return the file size in bytes
     * @throws FileSystemException if an error occurs and size cannot be retrieved
     */
    public long getSize() throws FileSystemException {
        if (fileSize == null) {
            fileSize = file.getSize();
        }

        return fileSize;
    }

    /**
     * Gets the file name with modification time size and permissions
     * @return String of file name, size, modification time, permissions
     */
    private String getFileNameString() throws FileSystemException {
        String fileName = file.getName();
        StringBuilder paddedName; // the name that will be displayed on the UI.

        if (fileName.length() > FILE_NAME_LENGTH) {
            paddedName = new StringBuilder(fileName.substring(0, FILE_NAME_LENGTH - 3) + "...");
        } else {
            paddedName = new StringBuilder(fileName);
            paddedName.append(" ".repeat(FILE_NAME_LENGTH - fileName.length()));
        }

        String permissions = file.getPermissions();
        permissions = permissions == null ? "N/A":permissions;
        paddedName.append("\t\t")
                .append(getSize())
                .append(" ")
                .append(getModificationTime())
                .append(" ")
                .append(permissions);

        return paddedName.toString().trim();
    }

    /**
     * Returns the file path this LineEntry is visually representing
     * @return the file path this LineEntry represents
     */
    public String getFilePath() {
        return file.getFilePath();
    }

    /**
     * If this LineEntry represents a file that is a symbolic link, this method gets and caches the target path for use by other classes
     * @return the target path
     * @throws Exception if an error occurs resolving it
     */
    public String getSymbolicLinkTarget() throws Exception {
        if (symLinkTarget == null && file.isSymbolicLink()) {
            symLinkTarget = file.getSymbolicLinkTarget();//resolvePath(file.getSymbolicLinkTarget());
        }

        return symLinkTarget;
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

    /**
     * Creates a new instance of LineEntry based on the file provided
     * @param file the file for this line entry
     * @param owningPanel the panel displaying this LineEntry
     * @return the appropriate instance of LineEntry or null if file doesn't exist or an error occurs
     */
    public static LineEntry newInstance(CommonFile file, DirectoryPane owningPanel) {
        try {
            if (file.isADirectory())
                return new DirectoryLineEntry(file, owningPanel);
            else if (file.isNormalFile())
                return new FileLineEntry(file, owningPanel);
            else
                return null;
        } catch (FileSystemException ex) {
            if (FTPSystem.isDebugEnabled())
                ex.printStackTrace();

            return null;
        }
    }
}
