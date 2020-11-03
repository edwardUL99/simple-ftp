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

package com.simpleftp.ui.dialogs;

import javafx.scene.control.TextInputDialog;

import java.util.Optional;

/**
 * A dialog for renaming a file/directory
 */
public class RenameFileDialog extends TextInputDialog {
    /**
     * Constructs a dialog.
     * @param currentName the current name of the file being renamed
     */
    public RenameFileDialog(String currentName) {
        setTitle("Rename Dialog");
        setHeaderText("Rename file/directory");
        setContentText("Enter the new name for the file/directory:");
        getEditor().setTooltip(null);
        getEditor().setText(currentName);
        getEditor().selectAll();
    }

    /**
     * Shows the dialofg and returns the name
     * @return the entered name, can be null
     */
    public String showAndGetName() {
        Optional<String> result = showAndWait();

        return result.orElse(null);
    }
}
