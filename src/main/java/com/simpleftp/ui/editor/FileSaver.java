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

import com.simpleftp.ui.UI;
import com.simpleftp.ui.editor.tasks.FileUploader;

/**
 * This class will provide functionality for saving a file that has been opened.
 * If the file is remote, it is expected to be saved locally in a temp folder. after saving the file can be deleted
 */
public class FileSaver {
    /**
     * The editor window saving the file
     */
    private FileEditorWindow editorWindow;
    /**
     * Constructs a file saver object
     * @param editorWindow the editor window saving the file
     */
    public FileSaver(FileEditorWindow editorWindow) {
        this.editorWindow = editorWindow;
    }

    /**
     * Saves the specified file contents in a file specified by filePath.
     * @param filePath the path to save the file to. If this is a remote file system, the changes will be saved in a temporary location and then uploaded to the specified path
     * @param savedFileContents the contents of the file to save
     */
    public void saveFile(String filePath, String savedFileContents) {
        FileUploader uploader = FileUploader.newInstance(editorWindow, filePath, savedFileContents);
        UI.addBackgroundTask(uploader);
        uploader.start();
    }
}