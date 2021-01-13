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

package com.simpleftp.ui.directories;

import com.simpleftp.filesystem.FileUtils;
import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.LocalFileSystem;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.files.LineEntry;

import java.io.File;
import java.util.ArrayList;

/**
 * This DirectoryPane is for use displaying local files
 */
final class LocalDirectoryPane extends DirectoryPane {
    /**
     * Constructs a LocalDirectoryPane with the given directory to initialise this panel with
     * @param directory the initial directory to display
     */
    LocalDirectoryPane(LocalFile directory) throws FileSystemException {
        super();
        fileSystem = new LocalFileSystem();
        initDirectory(directory);
    }

    /**
     * This method determines if a DirectoryPane is a local one. All Remote panes, regardless of the underlying connection is still remote, so they should return false
     *
     * @return true
     */
    @Override
    public boolean isLocal() {
        return true;
    }

    /**
     * Checks the directory passed in to see if the type matches the dfile type this DirectoryPane is for.
     * setDirectory calls this
     *
     * @param directory the directory to check
     * @throws IllegalArgumentException if the type of directory is different to the type of the current one
     */
    @Override
    void checkFileType(CommonFile directory) throws IllegalArgumentException {
        if (!(directory instanceof LocalFile))
            throw new IllegalArgumentException("The file type passed into this DirectoryPane must be an instance of LocalFile");
    }

    /**
     * Lazy Initialises the root directory for this directory pane and returns it
     *
     * @return the root directory
     */
    @Override
    CommonFile getRootDirectory() {
        if (rootDirectory == null) {
            rootDirectory = new LocalFile(FileUtils.getRootPath(true));
        }

        return rootDirectory;
    }

    /**
     * Does the remove action for the file. Removal of a file may differ between local and remote file panels, hence why abstract
     *
     * @param commonFile the file to remove
     * @return the result
     */
    @Override
    boolean doRemove(CommonFile commonFile) throws Exception {
        return fileSystem.removeFile(commonFile);
    }

    /**
     * Renames the specified local file
     * @param localFile the file to rename
     */
    private void renameLocalFile(final LocalFile localFile) {
        String fileName = localFile.getName();
        String newPath = UI.doRenameDialog(fileName);

        if (newPath != null) {
            try {
                newPath = FileUtils.addPwdToPath(getCurrentWorkingDirectory(), newPath, "/");
                newPath = UI.resolveSymbolicPath(newPath, "/", FileUtils.getRootPath(true)); // we will use symbolic path resolving as we may want to rename a file to a symbolic path
                if (overwriteExistingFile(new LocalFile(newPath))) {
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
            } catch (FileSystemException ex) {
                UI.doException(ex, UI.ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled());
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

        if (!UI.isFileOpened(filePath, true)) {
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

        return lineEntries;
    }


}
