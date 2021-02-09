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

import javafx.scene.control.ButtonType;

/**
 * This is a ConfirmationDialog but for asking to confirm about something that warrants more a warning than a simple confirmation
 */
public class WarnConfirmationDialog extends ConfirmationDialog {

    /**
     * Creates a confirmation dialog with the specified header and message
     *
     * @param header  text to display on dialog header
     * @param message text to display on dialog content
     */
    public WarnConfirmationDialog(String header, String message) {
        super(header, message);
        setAlertType(AlertType.WARNING);
        setTitle("Warning Dialog");
        getButtonTypes().add(0, ButtonType.CANCEL);
    }
}
