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

import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Optional;

/**
 * This is a dialog for viewing an exception and it's details
 */
public class ExceptionDialog extends Alert {
    private Exception ex;

    /**
     * Creates an Exception dialog with the specified exception
     * @param ex the exception for this exception dialog
     */
    public ExceptionDialog(Exception ex) {
        super(AlertType.ERROR);
        setTitle("Exception Dialog");
        setHeaderText("An exception has occurred");
        setContentText(ex.getMessage());
        this.ex = ex;
        createExpandableArea();
    }

    private void createExpandableArea() {
        StringWriter sw = new StringWriter();
        PrintWriter printWriter = new PrintWriter(sw);
        ex.printStackTrace(printWriter);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace is: ");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);
        GridPane expandable = new GridPane();
        expandable.setMaxWidth(Double.MAX_VALUE);
        expandable.add(label, 0, 0);
        expandable.add(textArea, 0, 1);
        Tooltip.install(expandable, new Tooltip("Silently ignore the exception, or abort and end the program?"));

        getDialogPane().setExpandableContent(expandable);
        getButtonTypes().clear();
        ButtonType ignore = new ButtonType("Ignore", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType abort = new ButtonType("Abort", ButtonBar.ButtonData.FINISH);
        getButtonTypes().setAll(ignore, abort);

        URL iconUrl = ClassLoader.getSystemResource("exception_icon.png");
        if (iconUrl != null) {
            String icon = iconUrl.toString();
            setGraphic(new ImageView(icon));
        }
        getDialogPane().getStylesheets().add(ClassLoader.getSystemResource("dialogs.css").toExternalForm());
        getDialogPane().getStyleClass().add("exceptionDialog");
    }

    /**
     * Checks the result of the dialog and determines the action
     */
    public void showAndDoAction() {
        Optional<ButtonType> result = showAndWait();
        result.map(buttonType -> {
            if (buttonType.getText().equals("Abort")) {
                System.exit(0);
            }

            return null;
        });
    }
}
