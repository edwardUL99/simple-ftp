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
     * Cancels the background task and the underlying service
     */
    void cancel();

    /**
     * Returns true if this task is running
     * @return true if running, false if not
     */
    boolean isRunning();
}
