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
/**
 * Provides classes for saving session information. This is information that relates to a current session and allows it to be saved among others.
 * A session includes the following:
 * <ul>
 *     <li>The last logged in user and server</li>
 *     <li>The last path visited both locally and remotely</li>
 *     <li>Other saved connection details</li>
 * </ul>
 *
 * It can contain more saved features down the line, such as hiding files and setting certain properties.
 * The sessions package provides features to abstract the saving of these sessions
 */
package com.simpleftp.sessions;