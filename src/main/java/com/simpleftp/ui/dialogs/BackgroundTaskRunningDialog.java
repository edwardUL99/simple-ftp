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
 * A dialog to warn that there are background tasks in progress on quit
 */
public class BackgroundTaskRunningDialog extends Alert {
    /**
     * Constructs a BackgroundTaskRunningDialog
     */
    public BackgroundTaskRunningDialog() {
        super(AlertType.WARNING);
        setTitle("Background Task Dialog");
        setHeaderText("Background Tasks Running");
        setContentText("There are still background tasks running. Quitting now will cancel them. Are you sure?");
        getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        getButtonTypes().clear();
        getButtonTypes().addAll(new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE), new ButtonType("Yes", ButtonBar.ButtonData.FINISH));
    }

    /**
     * Shows the dialog and gets the confirmation
     * @return true if confirmed to quit, false otherwise
     */
    public boolean showAndGetConfirmation() {
        Optional<ButtonType> result = showAndWait();

        return result.map(e -> !e.getText().equals("No")).orElse(false);
    }
}
