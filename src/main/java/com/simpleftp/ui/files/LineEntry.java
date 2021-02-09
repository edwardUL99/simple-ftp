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

package com.simpleftp.ui.files;

import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.directories.DirectoryPane;
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

import java.util.HashMap;

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
    protected DirectoryPane owningPane;
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
     * A boolean flag to determine if a drag has been started
     */
    protected boolean dragStarted;
    /**
     * A boolean variable to determine if mouse entry is allowed on any LineEntry
     */
    protected static boolean enableMouseEntry;
    /**
     * A boolean variable to detect if this is selected
     */
    @Getter
    protected boolean selected;
    /**
     * This flag is true if this line entry is the source of a ddrag event
     */
    protected boolean dragEventSource;
    /**
     * The last selected line entry for that directory pane
     */
    private static final HashMap<DirectoryPane, LineEntry> lastSelected = new HashMap<>();

    /**
     * Creates a base LineEntry with the specified image URL (which is assumed to be in the jar), file and panel
     * @param imageURL the URL to the image (assumed to be in the JAR)
     * @param file the file this LineEntry represents
     * @param owningPane the DirectoryPane this LineEntry is a part of
     */
    protected LineEntry(String imageURL, CommonFile file, DirectoryPane owningPane) throws FileSystemException {
        setSpacing(30);
        setStyle(UI.WHITE_BACKGROUND);
        image = new ImageView();
        image.setFitWidth(UI.FILE_ICON_SIZE);
        image.setFitHeight(UI.FILE_ICON_SIZE);
        image.setImage(new Image(imageURL));
        setHeight(UI.FILE_ICON_SIZE);
        this.file = file;
        this.owningPane = owningPane;
        enableMouseEntry = true;

        init();
    }

    /**
     * Sets this LineEntry to be the one that is selected on it's parent FilePanel if not null
     * @param selected true if selected, false if not
     */
    public void setSelected(boolean selected) {
        if (owningPane != null) {
            if (selected) {
                this.selected = true;
                LineEntry lastSelected = LineEntry.lastSelected.get(owningPane);
                if (lastSelected != null)
                    lastSelected.setSelected(false);

                LineEntry.lastSelected.put(owningPane, this);
                setStyle(UI.GREY_BACKGROUND_TRANSPARENT);
            } else {
                this.selected = false;
                setStyle(UI.WHITE_BACKGROUND);
            }

            FilePanel filePanel = owningPane.getFilePanel();
            if (filePanel != null) {
                if (selected)
                    filePanel.setComboBoxSelection(this);
                else
                    filePanel.setComboBoxSelection(null);
            }
        }
    }

    /**
     * If any line entry was selected for that directory pane
     * @param directoryPane the directory pane to unselect for
     */
    public static void unselectLastEntry(DirectoryPane directoryPane) {
        LineEntry lineEntry = lastSelected.remove(directoryPane);
        if (lineEntry != null)
            lineEntry.setSelected(false);
    }

    /**
     * This method sets the height of the line entry but is final as the behaviour cannot be changed by sub-classes
     * @param height the height to set this line entry to
     */
    @Override
    protected final void setHeight(double height) {
        super.setHeight(height);
    }

    /**
     * Initialises the line entry
     */
    private void init() throws FileSystemException {
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

        initMouseEvents();
    }

    /**
     * This method initialises mouse events so that we can interact with line entries with the mouse
     */
    private void initMouseEvents() {
        setOnMouseEntered(e -> {
            if (enableMouseEntry)
                setStyle(UI.GREY_BACKGROUND_TRANSPARENT);
        });
        setOnMouseExited(e -> {
            if (!selected)
                setStyle(UI.WHITE_BACKGROUND);
            if (dragStarted) {
                DirectoryPane.forEachInstance(pane -> pane.getUpButton().displayDragDropTarget(true));
                enableMouseEntry = false;
                dragEventSource = true;
                setStyle(UI.GREY_BACKGROUND_TRANSPARENT);

                UI.Events.setDragCursorImage(this);
                UI.Events.setDragInProgress(true);
            }
        });
        setOnMouseClicked(e -> {
            if (!e.isConsumed()) {
                e.setDragDetect(false);
                if (e.getClickCount() == 1) {
                    setSelected(!selected);
                } else if (e.getClickCount() == 2) {
                    owningPane.openLineEntry(this);
                }
            }
            owningPane.requestFocus();
        });
        setOnMousePressed(e -> e.setDragDetect(true));
        setOnMouseReleased(e -> {
            DirectoryPane.forEachInstance(pane -> pane.getUpButton().displayDragDropTarget(false));
            if (dragStarted) {
                dragEventSource = false;
                if (!selected)
                    setStyle(UI.WHITE_BACKGROUND);
            }

            UI.Events.resetMouseCursor();
            UI.Events.setDragInProgress(false);
            dragStarted = false;
            enableMouseEntry = true;
        });
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
     * Gets the size of the file behind this LineEntry
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
            symLinkTarget = file.getSymbolicLinkTarget();
        }

        return symLinkTarget;
    }

    /**
     * Can be used to determine the type of this LineEntry
     * @return true if the file this LineEntry represents is a file (could be a symlink too)
     */
    public abstract boolean isFile();

    /**
     * Can be used to determine the type of this LineEntry
     * @return true if the file this LineEntry represents is a file (could be a symlink too)
     */
    public abstract boolean isDirectory();

    @Override
    public int compareTo(LineEntry other) {
        if (isLocal() && System.getProperty("os.name").toLowerCase().contains("win"))
            return file.getName().compareToIgnoreCase(other.file.getName()); // windows sorts case-insensitively
        else
            return file.getName().compareTo(other.file.getName()); // unix and remote is to be sorted by case
    }

    /**
     * Creates a new instance of LineEntry based on the file provided
     * @param file the file for this line entry
     * @param owningPanel the panel displaying this LineEntry
     * @return the appropriate instance of LineEntry or null if file doesn't exist or an error occurs
     */
    public static LineEntry newInstance(CommonFile file, DirectoryPane owningPanel) {
        try {
            if (owningPanel == null)
                throw new NullPointerException("The provided DirectoryPane is null. A LineEntry cannot exist without an owning DirectoryPane");

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

    /**
     * Use this method to determine if the LineEntry is local, or false if remote
     * @return true if this LineEntry represents a local file, false if remote
     */
    public boolean isLocal() {
        return owningPane.isLocal();
    }

    /**
     * Refreshes this LineEntry. Should be called from the JavaFX thread
     * @throws FileSystemException if fails to refresh file or line entry
     */
    public void refresh() throws FileSystemException {
        fileSize = null;
        modificationTime = null;
        if (file.exists())
            init();
    }

    /**
     * This method defines how the drag event starts for a line entry
     */
    protected void dragStarted() {
        startFullDrag();
        dragStarted = true;
    }

    /**
     * Returns the backing file's hash code
     * @return hash code for this line entry
     */
    @Override
    public abstract int hashCode();

    /**
     * Determine if this LineEntry is equals to another.
     * Equality is done by calling the file's equals() method
     * @param obj the object to check.
     * @return true if equal, false if not
     */
    @Override
    public abstract boolean equals(Object obj);
}
