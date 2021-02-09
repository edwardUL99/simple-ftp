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

/**
 * This interface defines a "task" that is running in the background and wants to be tracked by the UI for example, copy/move of files.
 * This interface is intended for use only with tasks that occur in the background that is of interest to the user.
 * Background tasks that are just utilities, like the TaskScheduler do not need to be graphically known to the user. Therefore,
 * that is not a "BackgroundTask", instead it is more so a background service.
 *
 * This interface is just for the purpose of defining a BackgroundTask and tracking in the UI class. For a BackgroundTask that can be displayed
 * efficiently by the UI, see DisplayableBackgroundTask.
 */
public interface BackgroundTask {
    /**
     * Starts the background task and the underlying service.
     *
     * This can be started on any thread (i.e. can be started by schedulers etc.) but if there is any UI logic in the start() logic,
     * that logic should be called with a Platform.runLater method call. If that task is not a scheduled task (i.e. schedule just calls start()),
     * you do not need this method call.
     */
    void start();

    /**
     * Indicate that the task should be scheduled to run. The difference with this to start() depends on the implementation.
     * Some implementations may just call start() when this method is called, others may actually schedule this task to run
     * only after another one has succeeded.
     *
     * It is preferred to call this method to ensure there are no thread conflicts such as race conditions where 2 threads are
     * working on the same object at the same time, when it would be better to work on them one after another.
     *
     * This class should be called on the FX thread
     */
    void schedule();
    
    /**
     * Cancels the background task and the underlying service
     */
    void cancel();

    /**
     * Returns true if this task is running
     * @return true if running, false if not
     */
    boolean isRunning();

    /**
     * Use this call to determine if a task is ready
     * @return true if ready, false if not
     */
    boolean isReady();

    /**
     * Returns a boolean determining if the task is finished.
     * This should be tracked by a variable in the implementing class and not by checking any underlying JavaFX Service state since that has to be done from the JavaFX thread
     * @return true if finished, false if not
     */
    boolean isFinished();

    /**
     * Returns the state of this BackgroundTask
     * @return the state of the background task
     */
    State getState();

    /**
     * This enum provides enum values for the state of the background task
     */
    enum State {
        /**
         * The task has been created using the schedule() method and has not been started yet by a scheduler
         */
        SCHEDULED,
        /**
         * The task has been started using the start() method
         */
        STARTED,
        /**
         * The BackgroundTask's task has begun
         */
        RUNNING,
        /**
         * The task has been cancelled using cancel() method
         */
        CANCELLED,
        /**
         * The task has failed for some reason
         */
        FAILED,
        /**
         * The task has completed successfully
         */
        COMPLETED
    }
}
