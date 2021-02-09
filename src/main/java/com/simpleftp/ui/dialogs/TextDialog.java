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
 * A general TextDialog that's configurable to have any text for header, or content
 */
public class TextDialog extends TextInputDialog {
    /**
     * Creates a TextDialog
     * @param headerText the text for the header
     * @param contentText the text for the content
     * @param currentText current text to show, can also be null
     * @param subText optional text to show beneath the text field. Can be null. If not null, it will be displayed as an italic
     */
    public TextDialog(String headerText, String contentText, String currentText, String subText) {
        setTitle("Text Dialog");
        setHeaderText(headerText);
        setContentText(contentText);
        getEditor().setText(currentText);
        getEditor().selectAll();
        if (subText != null) {
            Tooltip.install(this.getDialogPane(), new Tooltip(subText));
        }

        getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
    }

    /**
     * Shows the dialog
     * @return the entered string. Null if blank or not entered
     */
    public String showAndGet() {
        Optional<String> result = showAndWait();

        return result.filter(e -> !e.equals("")).orElse(null);
    }
}
