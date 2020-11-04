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
 * A dialog used for confirming quit
 */
public class QuitDialog extends Alert {
    /**
     * Constructs a QuitDialog object
     */
    public QuitDialog() {
        super(AlertType.WARNING);
        setTitle("Quit Dialog");
        setHeaderText("Quit");
        setContentText("Are you sure you want to quit?");
        getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        getButtonTypes().clear();
        getButtonTypes().addAll(new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE), new ButtonType("Yes", ButtonBar.ButtonData.FINISH));
    }

    /**
     * Shows the dialog and returns true to quit, false if not
     * @return the result
     */
    public boolean showAndGetConfirmation() {
        Optional<ButtonType> result = showAndWait();

        return result.map(e -> {
            if (e.getText().equals("No")) {
                return false;
            } else {
                return true;
            }
        }).orElse(false);
    }
}
