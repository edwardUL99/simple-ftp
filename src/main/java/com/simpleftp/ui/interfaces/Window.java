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

package com.simpleftp.ui.interfaces;

/**
 * An interface to describe a window that the application can open.
 *
 * It defines 2 basic operations that a Window can do, show and close.
 * The implementing windows can define for each type of window if they want to be added to the UI#trackedWindows list
 */
public interface Window {
    /**
     * Defines the basic operation of showing a window.
     * This usually entails defining a scene to place a pane in and then using a Stage to show it
     */
    void show();

    /**
     * Defines the basic operation of closing a window.
     * This usually entails doing some clean up and then calling the stage that was opened to close
     */
    void close();
}
