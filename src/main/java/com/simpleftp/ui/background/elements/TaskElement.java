/*
 *  Copyright (C) 2020-2021 Edward Lynch-Milner
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

package com.simpleftp.ui.background.elements;

import com.simpleftp.properties.Properties;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.background.interfaces.BackgroundTask;
import com.simpleftp.ui.background.interfaces.DisplayableBackgroundTask;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;

import java.util.Timer;
import java.util.TimerTask;

// TODO the whole background task viewing needs rigorous testing, document new properties on confluence

/**
 * This class represents a UI element to display a DisplayableBackgroundTask graphically
 */
public final class TaskElement extends HBox {
    /**
     * The background task for this TaskElement
     */
    private final DisplayableBackgroundTask backgroundTask;
    /**
     * The button to cancel a task
     */
    private final Button cancelButton;
    /**
     * The button to delete a task entry
     */
    private final Button deleteButton;
    /**
     * A box to manage separate spacing between state and buttons
     */
    private final HBox stateButtonBox;
    /**
     * The max width for the description
     */
    private final static int MAX_DESCRIPTION_WIDTH = 60;
    /**
     * The width of the HBox used for laying out the description
     */
    private final static double DESCRIPTION_LAYOUT_WIDTH = 420;

    /**
     * Constructs a background task UI element
     * @param backgroundTask the task this element represents
     */
    public TaskElement(DisplayableBackgroundTask backgroundTask) {
        this.backgroundTask = backgroundTask;
        this.cancelButton = new Button();
        this.deleteButton = new Button();
        this.stateButtonBox = new HBox();

        init();
    }

    /**
     * Initialises this TaskElement element
     */
    private void init() {
        setPadding(new Insets(5));
        setSpacing(10);
        setStyle(UI.WHITE_BACKGROUND);
        setAlignment(Pos.CENTER_LEFT);
        stateButtonBox.setSpacing(20);
        stateButtonBox.setAlignment(Pos.CENTER);
        stateButtonBox.setStyle(UI.WHITE_BACKGROUND);

        initGraphic();
        initDescription();

        getChildren().add(stateButtonBox); // other elements have been added to the main box, not add the state button box

        initState();
        initButtons();
    }

    /**
     * This method initialises the
     */
    private void initGraphic() {
        ImageView imageView = new ImageView("task_icon.png");
        imageView.setFitHeight(25);
        imageView.setFitWidth(25);

        getChildren().add(imageView);
    }

    /**
     * Takes the provided description and determines if it needs to be ellipsized and then sets it as the label's text
     * @param description the description to ellipsize if required
     * @param label the label to set the text and tooltip
     */
    private void setLabelDescription(String description, Label label) {
        String labelValue;
        int newValueLength = description.length();
        if (newValueLength > MAX_DESCRIPTION_WIDTH) {
            labelValue = description.substring(0, MAX_DESCRIPTION_WIDTH - 3) + "...";
            label.setTooltip(new Tooltip(description));
        } else {
            labelValue = description;
            label.setTooltip(null);
        }

        label.setText(labelValue);
    }

    /**
     * Initialises the description
     */
    private void initDescription() {
        Label description = new Label();
        description.setFont(Font.font("Monospaced"));

        ReadOnlyStringProperty descriptionProperty = backgroundTask.getDescriptionProperty();
        setLabelDescription(descriptionProperty.getValue(), description);

        description.setOnMouseEntered(e -> description.setUnderline(true));
        description.setOnMouseExited(e -> description.setUnderline(false));
        description.setOnMouseClicked(e -> UI.doInfo("Background Task Description", descriptionProperty.getValue()));

        descriptionProperty.addListener((observable, oldValue, newValue) -> setLabelDescription(newValue, description));

        HBox descriptionBox = new HBox();
        descriptionBox.setMinWidth(DESCRIPTION_LAYOUT_WIDTH);
        descriptionBox.setMaxWidth(DESCRIPTION_LAYOUT_WIDTH);
        descriptionBox.setAlignment(Pos.CENTER_LEFT);
        descriptionBox.getChildren().add(description);
        descriptionBox.setStyle(UI.WHITE_BACKGROUND);

        getChildren().add(descriptionBox);
    }

    /**
     * Initialises the state of the task element
     */
    private void initState() {
        ReadOnlyStringProperty stateProperty = backgroundTask.getStateProperty();
        if (Properties.DELETE_TASK_ON_COMPLETION.getValue()) {
            stateProperty.addListener((observable, oldValue, newValue) -> {
                BackgroundTask.State state = backgroundTask.getState();
                if (state == BackgroundTask.State.COMPLETED) // don't remove if failed/cancelled so that user doesn't get confused that it was removed as if it was completed, when in
                    new DeletionTimer().start();            // fact, it was cancelled
            });
        }

        Label state = new Label(stateProperty.getValue());
        state.setFont(Font.font("Monospaced"));

        state.textProperty().bind(stateProperty);
        HBox stateBox = new HBox();
        stateBox.setAlignment(Pos.CENTER_RIGHT);
        stateBox.setMaxWidth(80);
        stateBox.setMinWidth(80);
        stateBox.getChildren().add(state);

        stateButtonBox.getChildren().add(stateBox);
    }

    /**
     * Initialises the buttons for this task
     */
    private void initButtons() {
        ImageView cancelGraphic = new ImageView("cancel_task.png");
        cancelGraphic.setFitWidth(25);
        cancelGraphic.setFitHeight(25);
        cancelButton.setGraphic(cancelGraphic);
        cancelButton.setTooltip(new Tooltip("Cancel this task's execution"));

        ImageView deleteGraphic = new ImageView("delete_task.png");
        deleteGraphic.setFitHeight(25);
        deleteGraphic.setFitWidth(25);
        deleteButton.setGraphic(deleteGraphic);
        deleteButton.setTooltip(new Tooltip("Remove this task from the task panel"));

        BooleanProperty cancelVisible = cancelButton.visibleProperty();
        cancelVisible.bind(backgroundTask.getCancellableProperty());
        cancelButton.managedProperty().bind(cancelVisible);

        BooleanProperty deleteVisible = deleteButton.visibleProperty();
        deleteVisible.bind(backgroundTask.getRemovableProperty());
        deleteButton.managedProperty().bind(deleteVisible);

        cancelButton.setOnAction(e -> backgroundTask.cancel());
        deleteButton.setOnAction(e -> UI.removeBackgroundTask(backgroundTask));

        stateButtonBox.getChildren().addAll(cancelButton, deleteButton);
    }

    /**
     * This class times the delay until deletion if deletion of completed tasks are enabled
     */
    private class DeletionTimer {
        /**
         * The backing timer element
         */
        private final Timer timer;

        /**
         * Constructs a deletion timer element
         */
        private DeletionTimer() {
            this.timer = new Timer(true);
        }

        /**
         * Starts the timer
         */
        private void start() {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> UI.removeBackgroundTask(TaskElement.this.backgroundTask));
                    timer.cancel();
                }
            }, Properties.TASK_DELETION_DELAY.getValue() * 1000);
        }
    }
}
