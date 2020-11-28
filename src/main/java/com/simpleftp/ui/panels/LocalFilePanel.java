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

import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.LocalFileSystem;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.files.LineEntry;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This FilePanel is for use displaying local files
 */
final class LocalFilePanel extends FilePanel {
    /**
     * Constructs a LocalFilePanel with the given directory to initialise this panel with
     * @param directory the initial directory to display
     */
    LocalFilePanel(LocalFile directory) throws FileSystemException {
        super(directory);
    }

    /**
     * Initialises the file system for use with this FilePanel
     *
     * @throws FileSystemException if an error occurs initialising it
     */
    @Override
    void initFileSystem() throws FileSystemException {
        fileSystem = new LocalFileSystem();
    }

    /**
     * Controls going up to parent directory
     */
    @Override
    public void up() {
        if (!isAtRootDirectory()) {
            try {
                LocalFile localFile = (LocalFile) directory;
                String parent = localFile.getParent();
                if (parent != null) {
                    LocalFile parentFile = new LocalFile(parent);
                    if (parentFile.exists() && parentFile.canRead()) {
                        setDirectory(parentFile);
                        refresh();
                    }
                }
            } catch (FileSystemException ex) {
                UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
            }
        }
    }

    /**
     * Checks the directory passed in to see if the type matches the dfile type this FilePanel is for.
     * setDirectory calls this
     *
     * @param directory the directory to check
     * @throws IllegalArgumentException if the type of directory is different to the type of the current one
     */
    @Override
    void checkFileType(CommonFile directory) throws IllegalArgumentException {
        if (!(directory instanceof LocalFile))
            throw new IllegalArgumentException("The file type passed into this FilePanel must be an instance of LocalFile");
    }

    /**
     * Gets the target of the directory symbolic link. It is assumed you have already checked if the file is a symbolic link before calling this method
     *
     * @param directory the directory to get target of
     * @return the target of the symbolic link
     * @throws IOException if directory is a local file and the directory provided is not a symbolic link
     */
    @Override
    String getSymLinkTargetPath(CommonFile directory) throws FileSystemException, IOException {
        String path = directory.getSymbolicLinkTarget();
        if (path.startsWith(".") || path.startsWith("..")) {
            String currentPath = this.directory.getFilePath();
            if (currentPath.equals(UI.PATH_SEPARATOR))
                currentPath = "";
            else if (currentPath.endsWith(UI.PATH_SEPARATOR))
                currentPath = currentPath.substring(0, currentPath.length() - 1);
            path = currentPath + UI.PATH_SEPARATOR + path;
        }

        return UI.resolveLocalPath(path, getCurrentWorkingDirectory()).getResolvedPath();
    }

    /**
     * Renames the specified local file
     * @param localFile the file to rename
     */
    private void renameLocalFile(final LocalFile localFile) {
        String filePath = localFile.getFilePath();
        String parentPath = UI.getParentPath(filePath);

        String fileName = localFile.getName();
        String newPath = UI.doRenameDialog(fileName);

        if (newPath != null) {
            newPath = new File(newPath).getName(); // ensure it is just the base name
            if (!newPath.equals(fileName)) {
                newPath = parentPath + System.getProperty("file.separator") + newPath;

                if (localFile.renameTo(new File(newPath))) {
                    UI.doInfo("File Renamed", "File has been renamed successfully");
                    double vPosition = entriesScrollPane.getVvalue();
                    double hPosition = entriesScrollPane.getHvalue();
                    refresh();
                    entriesScrollPane.setVvalue(vPosition);
                    entriesScrollPane.setHvalue(hPosition); // resets the position of the scrollbars to where they were before the refresh
                } else {
                    UI.doError("Rename Failed", "Failed to rename file");
                }
            }
        }
    }

    /**
     * Handles when rename is called on the context menu with the specified line entry
     *
     * @param lineEntry the line entry to rename the file of
     */
    @Override
    void renameLineEntry(LineEntry lineEntry) {
        CommonFile file = lineEntry.getFile();
        String filePath = file.getFilePath();

        if (!UI.isFileOpened(filePath)) {
            renameLocalFile((LocalFile) file);
        } else {
            UI.doError("File Open", "The file " + file.getName() + " is open, it cannot be renamed");
        }
    }

    /**
     * Constructs the list of line entries from the files listed by the local file
     * @param lineEntries the list of line entries to populate
     * @param localFile the file to list
     */
    private void constructListOfLocalFiles(ArrayList<LineEntry> lineEntries, LocalFile localFile) {
        try {
            CommonFile[] files = fileSystem.listFiles(localFile.getFilePath());
            if (files == null || files.length == 0) {
                lineEntries.clear();
            } else {
                for (CommonFile file : files) {
                    LocalFile file1 = (LocalFile) file;
                    boolean showFile = showFile(file, e -> showHiddenFiles || !file1.isHidden());

                    if (showFile) {
                        LineEntry constructed = createLineEntry(file1);

                        if (constructed != null)
                            lineEntries.add(constructed);
                    }
                }
            }
        } catch (FileSystemException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
            lineEntries.clear();
        }
    }

    /**
     * Constructs the list of line entries to display
     *
     * @return the list of constructed line entries
     */
    @Override
    ArrayList<LineEntry> constructListOfFiles() {
        ArrayList<LineEntry> lineEntries = new ArrayList<>();
        constructListOfLocalFiles(lineEntries, (LocalFile)directory);

        if (lineEntries.size() > 0) {
            Collections.sort(lineEntries);
        }

        return lineEntries;
    }


}
