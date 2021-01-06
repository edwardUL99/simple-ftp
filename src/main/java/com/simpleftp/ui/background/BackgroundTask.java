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

package com.simpleftp.ui.background;

/**
 * This interface defines a Background task that can be started/stopped
 */
public interface BackgroundTask {
    /**
     * Starts the background task and the underlying service
     */
    void start();

    /**
     * Indicate that the task should be scheduled to run. The difference with this to start() depends on the implementation.
     * Some implementations may just call start() when this method is called, others may actually schedule this task to run
     * only after another one has succeeded.
     *
     * It is preferred to call this method to ensure there are no thread conflicts such as race conditions where 2 threads are
     * working on the same object at the same time, when it would be better to work on them one after another
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
}
