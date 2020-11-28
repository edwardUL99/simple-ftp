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

import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;

/**
 * Dialog for specifying a path for a directory to create
 */
public class CreatePathDialog extends ChangePathDialog {
    /**
     * Constructs the dialog
     * @param directory true if to create a directory, false for a file
     */
    public CreatePathDialog(boolean directory) {
        String fileTypeUpper = directory ? "Directory":"File";
        String fileTypeLower = directory ? "directory":"file";
        setTitle("Create " + fileTypeUpper);
        setHeaderText("Create new " + fileTypeLower);
        setContentText("Enter the path of the new " + fileTypeLower + ": ");
    }

    /**
     * Looks up the button by type and returns it
     * @param buttonType the type of the button
     * @return the button if found
     */
    public Button lookupButton(ButtonType buttonType) {
        return (Button)getDialogPane().lookupButton(buttonType);
    }
}
