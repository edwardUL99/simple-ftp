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

import com.simpleftp.properties.Properties;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

import java.util.Optional;

/**
 * Gives the user a choice to still open a file even if it is over 100MB
 */
public class FileSizeConfirmationDialog extends Alert {
    /**
     * Constructs the FileSizeConfirmationDialog
     * @param path the file being opened
     */
    public FileSizeConfirmationDialog(String path) {
        super(AlertType.WARNING);
        setHeaderText("File Size Warning");
        setContentText("File " + path + " is over " + Properties.FILE_EDITOR_SIZE_WARN_LIMIT.getValue() / 1000000 + "MB. Do you confirm that you still want to open it? (You may run out of memory)");
        setTitle("Editing Large File Dialog");
        getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        getButtonTypes().clear();
        getButtonTypes().addAll(new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE), new ButtonType("Yes", ButtonBar.ButtonData.FINISH));
    }

    /**
     * Shows the dialog and gets the confirmation
     * @return true if confirmed to open, false otherwise
     */
    public boolean showAndGetConfirmation() {
        Optional<ButtonType> result = showAndWait();

        return result.map(e -> !e.getText().equals("No")).orElse(false);
    }
}
