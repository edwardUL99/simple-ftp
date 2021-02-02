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
 * The sessions package provides features to abstract the saving of these sessions.
 *
 * This is done through a SessionFile representation in memory. This SessionFile contains all the sessions currently related to the system.
 * On disk, it is stored in XML representation, the structure of which as defined by sessions.xsd.
 *
 * This package aims to provide the interface to this Sessions file on disk by providing abstract access to each component of the file.
 * This main access is done through the Sessions class which resolves around the fact that a single login instance can represent only one session at a time.
 * This Sessions class abstracts all the steps required to setup this current session and the saving of the session.
 *
 * Sessions cannot be constructed outside this package so this interface, with the Sessions class, is the only way for client classes not in this package, to both create
 * and interact with the sessions.
 *
 * The entry point to using the Sessions interface is the call to Sessions#initialise. This makes the system ready for all client classes.
 *
 * The Sessions class is only designed to be accessed from a single thread. Therefore you should not have multiple threads accessing any class in the package at the same time.
 */
package com.simpleftp.sessions;