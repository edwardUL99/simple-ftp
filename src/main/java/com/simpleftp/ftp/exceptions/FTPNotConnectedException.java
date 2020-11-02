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

package com.simpleftp.ftp.exceptions;

import lombok.Getter;

import java.util.HashMap;

/**
 * A simple Exception class to wrap all exceptions related to connection status of the FTPConnection
 */
@Getter
public class FTPNotConnectedException extends FTPException {
    /**
     * An enum to represent the action that took place which caused this NotConnectedException
     *
     * This is a "growing" enum, i.e. it will change as development progresses
     */
    public enum ActionType {
        /**
         * Represents an action related to logging into/out of the server
         */
        LOGIN,
        /**
         * Represents an action relating to logging out from the server
         */
        LOGOUT,
        /**
         * Represents the action of disconnecting from a connected FTPConnection
         */
        DISCONNECT,
        /**
         * Represents an action of uploading a file to the server
         */
        UPLOAD,
        /**
         * Represents any action in relation to downloading, either downloading physically to the local disk, or downloading file information in an intermediate step,
         * such as into a FTPFile
         */
        DOWNLOAD,
        /**
         * Represents any action in relation to modifying an existing file e.g. editing it or deleting it
         */
        MODIFICATION,
        /**
         * Represents any action related to checking to the status of the server/files. Differs from DOWNLOAD, as the information received is limited to status
         * and not contents being downloaded
         */
        STATUS_CHECK,
        /**
         * Represents any action relating to server navigation, such as changing working directory
         */
        NAVIGATE
    }

    private static final HashMap<ActionType, String> actionTypeMappings = new HashMap<>();

    static {
        ActionType[] actionTypes = ActionType.values();
        String[] names = {"Login", "Logout", "Disconnect", "Upload", "Download", "Modification", "Status Check", "Navigate"};

        for (int i = 0; i < actionTypes.length; i++)
            actionTypeMappings.put(actionTypes[i], names[i]);
    }

    /**
     * The ActionType enum value which caused this exception
     */
    private final ActionType actionType;

    /**
     * Constructs an object of this exception with the specified message and ActionType enum value
     * @param message the message for this exception to display
     * @param actionType the ActionType representing the action that was done that caused this exception
     */
    public FTPNotConnectedException(String message, ActionType actionType) {
        super(message, "");
        this.actionType = actionType;
    }

    /**
     * Constructs an object of this exception with the specified message, an exception that causes this one and the ActionType enum value
     * @param message the message for this exception to display
     * @param e the exception causing this exception
     * @param actionType the ActionType representing the action that was done that caused this exception
     */
    public FTPNotConnectedException(String message, Exception e, ActionType actionType) {
        super(message, "", e);
        this.actionType = actionType;
    }

    /**
     * Overrides super's getMessage() call to append to the message the action type in a string form
     * @return the message this exception displays
     */
    @Override
    public String getMessage() {
        return super.getMessage() + ", Caused by a " + actionTypeMappings.get(actionType) + " action";
    }
}
