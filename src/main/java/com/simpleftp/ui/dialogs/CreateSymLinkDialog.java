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

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

import java.util.Optional;

/**
 * This dialog provides a dialog for entering a target path and the name of the symbolic link
 */
public class CreateSymLinkDialog extends Dialog<Pair<String, String>> {
    /**
     * The text field for entering the target path
     */
    private final TextField targetPath;

    /**
     * The text field for entering the symlink name
     */
    private final TextField symLinkName;

    /**
     * Creates the dialog
     */
    public CreateSymLinkDialog() {
        setTitle("Create Symbolic Link Dialog");
        setHeaderText("Symbolic Link Creation");
        setGraphic(new ImageView(new Image("sym_link.png")));
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        targetPath = new TextField();
        targetPath.setPromptText("Target Path");

        symLinkName = new TextField();
        symLinkName.setPromptText("Symbolic link name");

        initialiseFields();
        setResultConverter(e -> {
            if (e == ButtonType.OK) {
                return new Pair<>(targetPath.getText(), symLinkName.getText());
            }

            return null;
        });
    }

    /**
     * Initialises the gridpane with the text fields
     */
    private void initialiseFields() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(30));

        gridPane.add(new Label("Target Path:"), 0, 0);
        gridPane.add(targetPath, 1, 0);
        gridPane.add(new Label("Symbolic Link Name:"), 0, 1);
        gridPane.add(symLinkName, 1, 1);
        getDialogPane().setContent(gridPane);
    }

    /**
     * Shows the dialog and gets the pair of paths
     * @return the pair if ok was pressed, null if cancelled
     */
    public Pair<String, String> showAndGetEntry() {
        Optional<Pair<String, String>> result = showAndWait();

        return result.orElse(null);
    }
}
