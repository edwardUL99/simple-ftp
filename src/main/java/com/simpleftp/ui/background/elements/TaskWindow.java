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

import com.simpleftp.ui.UI;
import com.simpleftp.ui.interfaces.Window;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * This class represents a Window that can display a TaskPane
 */
public class TaskWindow implements Window {
    /**
     * The stage displaying the TasksWindow
     */
    private final Stage stage;
    /**
     * The task pane backing this window
     */
    private final TaskPane taskPane;

    /**
     * Creates a TaskWindow instance
     */
    public TaskWindow() {
        stage = new Stage();
        taskPane = new TaskPane(this);

        init();
    }

    /**
     * Initialises this task window
     */
    private void init() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(taskPane);
        scrollPane.setMinViewportHeight(UI.TASKS_WINDOW_HEIGHT);
        scrollPane.setMinViewportWidth(UI.TASKS_WINDOW_WIDTH);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));

        Scene scene = new Scene(scrollPane, UI.TASKS_WINDOW_WIDTH, UI.TASKS_WINDOW_HEIGHT);
        stage.setTitle("SimpleFTP Background Tasks");
        stage.setResizable(false);
        stage.setScene(scene);
    }

    /**
     * Returns the TaskPane displaying the tasks. Call getTaskPane().addTask or removeTask to add/remove tasks to this window
     * @return the TaskPane behind this window
     */
    public TaskPane getTaskPane() {
        return taskPane;
    }

    /**
     * Defines the basic operation of showing a window.
     * This usually entails defining a scene to place a pane in and then using a Stage to show it
     */
    @Override
    public void show() {
        if (!stage.isShowing())
            stage.show();
        else
            stage.toFront();
    }

    /**
     * Defines the basic operation of closing a window.
     * This usually entails doing some clean up and then calling the stage that was opened to close
     */
    @Override
    public void close() {
        if (stage.isShowing())
            stage.close();
    }
}
