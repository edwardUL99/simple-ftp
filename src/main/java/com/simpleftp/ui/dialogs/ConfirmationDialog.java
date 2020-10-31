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
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

import java.util.Optional;

/**
 * Represents a confirmation dialog
 */
public class ConfirmationDialog extends Alert {
    /**
     * Creates a confirmation dialog with the specified header and message
     * @param header text to display on dialog header
     * @param message text to display on dialog content
     */
    public ConfirmationDialog(String header, String message) {
        super(AlertType.CONFIRMATION);
        setTitle("Confirmation Dialog");
        setHeaderText(header);
        setContentText(message);
        getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
    }

    /**
     * Gets the chosen option and if ok, return true
     * @return true if ok is chosen, false otherwise
     */
    public boolean showAndGetChoice() {
        Optional<ButtonType> result = showAndWait();

        return result.get() == ButtonType.OK;
    }
}
