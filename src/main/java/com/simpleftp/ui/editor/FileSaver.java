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

package com.simpleftp.ui.editor;

import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.RemoteFileSystem;
import com.simpleftp.filesystem.interfaces.FileSystem;

import java.io.*;

/**
 * This class will provide functionality for saving a file that has been opened.
 * If the file is remote, it is expected to be saved locally in a temp folder. after saving the file can be deleted
 */
public class FileSaver {
    /**
     * The file system the file is being saved to
     */
    private final FileSystem fileSystem;

    /**
     * Constructs a file saver object
     * @param fileSystem the file system files will be saved to
     */
    public FileSaver(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    /**
     * Writes to the specified file the file contents
     * @param filePath the path to the file
     * @param fileContents the contents of the file
     * @throws IOException if an error occurs
     */
    private void writeToFile(String filePath, String fileContents) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filePath));
        bufferedWriter.write(fileContents);
        bufferedWriter.close();
    }

    /**
     * Saves the specified file contents in a file specified by filePath.
     * @param filePath the path to save the file to. If this is a remote file system, the changes will be saved in a temporary location and then uploaded to the specified path
     * @param savedFileContents
     * @throwa Exception if any error occurs
     */
    public void saveFile(String filePath, String savedFileContents) throws Exception {
        File file = new File(filePath);
        String fileName = file.getName();
        String saveFilePath;
        boolean remoteFileSystem = fileSystem instanceof RemoteFileSystem;

        if (remoteFileSystem) {
            saveFilePath = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + fileName;
            file = new File(saveFilePath);
        } else {
            saveFilePath = filePath;
        }

        writeToFile(saveFilePath, savedFileContents);

        if (remoteFileSystem) {
            fileSystem.removeFile(filePath); // remove as we are overwriting
            String parent = new File(filePath).getParent();
            parent = parent == null ? "/":parent;
            fileSystem.addFile(new LocalFile(file.getAbsolutePath()), parent);
            file.delete();
        }

        // don't need to bother add file to local file system, because write to file would have already added it
    }
}
