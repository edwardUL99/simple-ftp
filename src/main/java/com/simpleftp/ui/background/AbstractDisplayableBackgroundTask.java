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

package com.simpleftp.ui.background;

import com.simpleftp.ui.UI;
import com.simpleftp.ui.background.interfaces.DisplayableBackgroundTask;
import javafx.application.Platform;
import javafx.beans.property.*;

/**
 * This class represents a displayable background task that is not suitable to run on its own yet but
 * contains properties like state and String properties.
 *
 * All tasks that want to be represented inside a TaskElement should extend this class. This class provides easy integration with the UI as the properties
 * can be bound with UI text elements. It provides convenience updateState and setDescription methods. These are final
 * as the proper way to change the behaviour for them is through directly implementing BackgroundTask.
 *
 * The intention of this class is to provide consistent access to these properties and consistent state property regardless
 * of the implementing tasks.
 */
public abstract class AbstractDisplayableBackgroundTask implements DisplayableBackgroundTask {
    /**
     * The state for this task. Should only be updated by updateState
     */
    protected State state;
    /**
     * A property representing the state in a displayable String value
     */
    private final StringProperty stateProperty;
    /**
     * The description for this background task.
     */
    private final StringProperty descriptionProperty;
    /**
     * Determines if this can be cancelled
     */
    private final BooleanProperty cancellableProperty;
    /**
     * Determines if this can be removed from UI
     */
    private final BooleanProperty removableProperty;

    /**
     * Constructs an instance of this abstract task
     */
    public AbstractDisplayableBackgroundTask() {
        stateProperty = new SimpleStringProperty();
        descriptionProperty = new SimpleStringProperty();
        cancellableProperty = new SimpleBooleanProperty();
        removableProperty = new SimpleBooleanProperty();
    }

    /**
     * Returns the string property representing the current state. It is a version of each element of the State enum
     * but in a way that is readable on a UI element
     *
     * @return string property representing state
     */
    @Override
    public StringProperty getStateProperty() {
        return stateProperty;
    }

    /**
     * Gets the property representing the description displayed on UI elements
     *
     * @return the property containing the description value
     */
    @Override
    public StringProperty getDescriptionProperty() {
        return descriptionProperty;
    }

    /**
     * This property determines if the task can be cancelled. I.e. a UI element can only cancel if state is scheduled, started or
     * running
     *
     * @return the property representing if cancellable
     */
    @Override
    public ReadOnlyBooleanProperty getCancellableProperty() {
        return cancellableProperty;
    }

    /**
     * This property determines if the task can be removed from the UI, i.e. its state is finished, cancelled or completed
     *
     * @return the property representing if removable
     */
    @Override
    public ReadOnlyBooleanProperty getRemovableProperty() {
        return removableProperty;
    }

    /**
     * Returns true if this task is running
     *
     * @return true if running, false if not
     */
    @Override
    public boolean isRunning() {
        return cancellableProperty.getValue();
    }

    /**
     * Returns a boolean determining if the task is finished.
     * This should be tracked by a variable in the implementing class and not by checking any underlying JavaFX Service state since that has to be done from the JavaFX thread
     *
     * @return true if finished, false if not
     */
    @Override
    public boolean isFinished() {
        return removableProperty.getValue();
    }

    /**
     * Updates the state value and also the state property with the provided state. If called on FX thread, it is done
     * directly, else wrapped with a Platform.runLater call.
     * @param state the state to set this task to
     */
    protected final void updateState(State state) {
        if (Platform.isFxApplicationThread()) {
            this.state = state;
            String stateValue = state.toString();
            stateValue = stateValue.charAt(0) + stateValue.substring(1).toLowerCase();

            boolean cancellable = state != State.FAILED && state != State.CANCELLED && state != State.COMPLETED;

            stateProperty.setValue(stateValue);
            cancellableProperty.setValue(cancellable);
            removableProperty.setValue(!cancellable);
        } else {
            Platform.runLater(() -> updateState(state));
        }
    }

    /**
     * Sets the value of the description property. This should be called from the FX thread.
     * @param description the description to set
     */
    protected final void setDescription(String description) {
        descriptionProperty.setValue(description);
    }

    /**
     * Returns the state of this BackgroundTask
     *
     * @return the state of the background task
     */
    @Override
    public State getState() {
        return state;
    }

    /**
     * This method adds the task to the UI's list of tasks.
     * If on the FX thread, it is added directly, else,
     * it is done in a Platform.runLater call
     */
    protected void displayTask() {
        if (Platform.isFxApplicationThread())
            UI.addBackgroundTask(this);
        else
            Platform.runLater(() -> UI.addBackgroundTask(this));
    }
}
