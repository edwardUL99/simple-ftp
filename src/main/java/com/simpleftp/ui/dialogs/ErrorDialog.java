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

import javafx.scene.layout.Region;

/**
 * Wraps a JavaFX Dialog to abstract some of the components
 */
public class ErrorDialog extends javafx.scene.control.Alert {
    public ErrorDialog(String header, String message) {
        super(AlertType.ERROR);
        setHeaderText(header);
        setTitle("Error Dialog");
        setContentText(message);
        setWidth(getWidth() + 20);
        getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
    }
}
