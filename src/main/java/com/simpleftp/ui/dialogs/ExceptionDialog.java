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

import com.simpleftp.ui.UI;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is a dialog for viewing an exception and it's details
 */
public class ExceptionDialog extends Alert {
    private final Exception ex;

    /**
     * Creates an Exception dialog with the specified exception
     * @param ex the exception for this exception dialog
     * @param showIgnoreButton true to show ignore button, false to not show it. If false, clicking the x button also aborts execution
     */
    public ExceptionDialog(Exception ex, boolean showIgnoreButton) {
        super(AlertType.ERROR);
        setTitle("Exception Dialog");
        setHeaderText("An exception has occurred");
        setContentText(ex.getMessage());
        this.ex = ex;
        createExpandableArea();
        getDialogPane().getStylesheets().add(ClassLoader.getSystemResource("dialogs.css").toExternalForm());
        getDialogPane().getStyleClass().add("exceptionDialog");
        getButtonTypes().clear();
        ButtonType ignore = new ButtonType("Ignore", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType abort = new ButtonType("Abort", ButtonBar.ButtonData.FINISH);

        if (showIgnoreButton) {
            getButtonTypes().setAll(ignore, abort);
        } else {
            getButtonTypes().setAll(abort);
            setOnCloseRequest(e -> {
                Platform.exit();
                System.exit(UI.EXCEPTION_DIALOG_ABORTED_EXIT_CODE);
            });
        }

        URL iconUrl = ClassLoader.getSystemResource("exception_icon.png");
        if (iconUrl != null) {
            String icon = iconUrl.toString();
            setGraphic(new ImageView(icon));
        }
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
    }

    /**
     * Checks the result of the dialog and determines the action
     */
    public void showAndDoAction() {
        Optional<ButtonType> result = showAndWait();
        AtomicBoolean exit = new AtomicBoolean(false);
        result.map(buttonType -> {
            if (buttonType.getText().equals("Abort")) {
                exit.set(true);
            }

            return null;
        });

        if (exit.get()) {
            Platform.exit();
            System.exit(UI.EXCEPTION_DIALOG_ABORTED_EXIT_CODE);
        }
    }
}
