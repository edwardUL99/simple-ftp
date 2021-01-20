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
import com.simpleftp.ui.UI;
import com.simpleftp.ui.directories.DirectoryPane;
import javafx.collections.ObservableList;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.input.*;

/**
 * A line entry implementation for a Directory.
 */
public final class DirectoryLineEntry extends LineEntry {
    /**
     * Constructs Directory LineEntry
     * @param file the file that this entry represents. It is not this class' responsibility to ensure file is a directory
     * @param owningPanel the panel owning this entry, i.e. the panel it is on
     */
    DirectoryLineEntry(CommonFile file, DirectoryPane owningPanel) throws FileSystemException {
        super(file.isSymbolicLink() ? "dir_icon_symlink.png":"dir_icon.png", file, owningPanel);
        initDragAndDrop();
    }

    /**
     * Can be used to determine the type of this LineEntry
     *
     * @return true if the file this LineEntry represents is a file (could be a symlink too)
     */
    @Override
    public boolean isFile() {
        return false;
    }

    /**
     * Can be used to determine the type of this LineEntry
     *
     * @return true if the file this LineEntry represents is a directory(could be a symlink too)
     */
    @Override
    public boolean isDirectory() {
        return true;
    }

    /**
     * This method defines the behaviour for when a drag enters a line entry
     * @param dragEvent the drag event representing the drag entry
     */
    private void dragEntered(MouseDragEvent dragEvent) {
        EventTarget target = dragEvent.getTarget();
        Object gestureSource = dragEvent.getGestureSource();

        if (gestureSource != this && UI.MouseEvents.validDragAndDropTarget(target)) {
            LineEntry sourceEntry = UI.MouseEvents.selectLineEntry(gestureSource);
            if (sourceEntry == null || sourceEntry.owningPane == owningPane) {
                setStyle(UI.MouseEvents.DRAG_ENTERED_BACKGROUND);
            } else {
                ObservableList<Node> children = owningPane.getEntriesBox().getChildren();
                if (this == children.get(children.size() - 1))
                    setStyle(UI.MouseEvents.DRAG_ENTERED_BACKGROUND); // bit darker to distinguish from drag entry of entries box
                else
                    setStyle(UI.MouseEvents.DRAG_ENTERED_BACKGROUND_CLEAR);
            }

            UI.MouseEvents.setDragCursorEnteredImage();
        }
    }

    /**
     * This method defines the behaviour for when a drag exits a line entry
     * @param dragEvent the drag event representing the drag exit
     */
    private void dragExited(MouseDragEvent dragEvent) {
        if (!dragEventSource && !selected)
            setStyle(UI.WHITE_BACKGROUND);

        UI.MouseEvents.setDragCursorImage(this);
    }

    /**
     * This method defines the behaviour for when a drag is dropped on a line entry
     * @param dragEvent the drag event representing the drag drop
     */
    private void dragDropped(MouseDragEvent dragEvent) {
        Object source = dragEvent.getGestureSource();
        if (source != this) {
            DirectoryPane directoryPane = UI.MouseEvents.getDirectoryPane(source);

            if (directoryPane != null) {
                if (directoryPane == owningPane) {
                    directoryPane.handleDragAndDropOnSamePane(dragEvent, this);
                } else {
                    owningPane.handleDragAndDropToDifferentPane(dragEvent, directoryPane, this);
                }
            }
        }

        UI.MouseEvents.resetMouseCursor();

        dragEvent.consume();
    }

    /**
     * This method initialises the drag and drop for the line entry
     */
    void initDragAndDrop() {
        setOnDragDetected(e -> dragStarted());
        setOnMouseDragged(e -> e.setDragDetect(false));
        setOnMouseDragEntered(this::dragEntered);
        setOnMouseDragExited(this::dragExited);
        setOnMouseDragReleased(this::dragDropped);
    }
}
