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
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;

import java.util.Optional;

/**
 * A dialog pane for entering a path to change directory to or file to open
 */
public class ChangePathDialog extends TextInputDialog {
    /**
     * Constructs a dialog
     */
    public ChangePathDialog() {
        setTitle("Go to path");
        setHeaderText("Go to directory/file");
        setContentText("Enter path to the directory/file: ");
        getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        getEditor().setTooltip(new Tooltip("Path can be absolute or relative"));
    }

    /**
     * Shows the dialog and returns the entered path
     * @return the entered path, can be null
     */
    public String showAndGetPath() {
        Optional<String> result = showAndWait();

        return result.orElse(null);
    }
}
