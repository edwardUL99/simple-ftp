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

package com.simpleftp.ui.background.interfaces;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;

/**
 * This interface represents a background task that can be displayed on the U.I graphically.
 * It is the same as BackgroundTask but with extra methods related to JavaFX properties.
 *
 * If you want to display a background task in the TasksWindow, it will have to implement this interface. The best way
 * to implement this interface is to extend com.simpleftp.ui.background.AbstractDisplayableBackgroundTask, see next paragraph
 * <p>
 * For best integration with the UI, all tasks that want to be represented by a TaskElement should extend AbstractDisplayableBackgroundTask as that provides
 * convenience methods for updating state and description properties through AbstractDisplayableBackgroundTask.updateState(State state)
 * and AbstractDisplayableBackgroundTask.setDescription(String description).
 * <p>
 * These properties can be bound to UI text elements so its best to update them in one place. If the implementation of updateState
 * or setDescription needs to be changed, implement this interface directly.
 * <p>
 * <b>NOTE:</b> You need to call UI.addBackgroundTask(this) either in the schedule or start method so that the task will be displayed. Simply
 * implementing this interface does not automatically display the Task, just provides a means to tell the UI class that it
 * is a BackgroundTask that we want to display. The extra methods here then are used in the constructed TaskElement.
 */
public interface DisplayableBackgroundTask extends BackgroundTask {
    /**
     * This property determines if the task can be cancelled. I.e. a UI element can only cancel if state is scheduled, started or
     * running
     * @return the property representing if cancellable
     */
    ReadOnlyBooleanProperty getCancellableProperty();

    /**
     * This property determines if the task can be removed from the UI, i.e. its state is finished, cancelled or completed
     * @return the property representing if removable
     */
    ReadOnlyBooleanProperty getRemovableProperty();

    /**
     * Returns the string property representing the current state. It is a version of each element of the State enum
     * but in a way that is readable on a UI element
     * @return string property representing state
     */
    ReadOnlyStringProperty getStateProperty();

    /**
     * Gets the property representing the description displayed on UI elements
     * @return the property containing the description value
     */
    ReadOnlyStringProperty getDescriptionProperty();
}
