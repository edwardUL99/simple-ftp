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

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

import java.util.Optional;

/**
 * This dialog is used to notify the user that they have entered a path that denotes they have entered a symbolic link.
 * It gives the choice between going to the path specified, or the target of the link
 */
public class SymbolicPathDialog extends Alert {
    /**
     * Constructs a symbolic path dialog
     * @param path the path that this dialog is for
     */
    public SymbolicPathDialog(String path) {
        super(AlertType.CONFIRMATION);
        setTitle("Symbolic Link");
        setHeaderText("Symbolic Link Path");
        setContentText("The path " + path + " represents a symbolic link. Do you want to go to the path or the target of the link?");
        getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        getButtonTypes().clear();
        getButtonTypes().addAll(new ButtonType("Target Path", ButtonBar.ButtonData.CANCEL_CLOSE), new ButtonType("Specified Path", ButtonBar.ButtonData.OK_DONE));
    }

    /**
     * SHows the dialog and returns the choice
     * @return true indicates that they want to go to the specified path (default behaviour), false indicates to go to target path
     */
    public boolean showAndGetChoice() {
        Optional<ButtonType> result = showAndWait();

        if (result.isPresent()) {
            ButtonType button = result.get();
            return !button.getText().equals("Target Path"); // if it equals it, it will return false, else true
        }

        return true;
    }
}
