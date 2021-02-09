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
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * This class represents a Pane that lists multiple TaskElements for display
 */
public class TaskPane extends VBox {
    /**
     * The TaskWindow containing this TaskPane
     */
    private final TaskWindow taskWindow;
    /**
     * Creates a TaskPane element
     * @param taskWindow the TaskWindow containing this pane. It can't be null
     */
    public TaskPane(TaskWindow taskWindow) {
        if (taskWindow == null)
            throw new NullPointerException("Null TaskWindow provided to TaskPane");

        this.taskWindow = taskWindow;
        setStyle(UI.WHITE_BACKGROUND);
        setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
        setMinHeight(UI.TASKS_WINDOW_HEIGHT);
        setMinWidth(UI.TASKS_WINDOW_WIDTH - 20);
    }

    /**
     * This method adds a TaskElement to this TaskPane's children
     * @param task the task to add
     */
    public void addTask(TaskElement task) {
        getChildren().add(task);
    }

    /**
     * Removes the task from this pane
     * @param task the task to remove
     */
    public void removeTask(TaskElement task) {
        ObservableList<Node> children = getChildren();
        children.remove(task);

        if (children.isEmpty())
            taskWindow.close();
    }
}
