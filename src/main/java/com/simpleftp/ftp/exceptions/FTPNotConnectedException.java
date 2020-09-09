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
        LOGIN,
        LOGOUT,
        DISCONNECT,
        UPLOAD,
        DOWNLOAD,
        STATUS_CHECK
    }

    private static HashMap<ActionType, String> actionTypeMappings = new HashMap<>();

    static {
        ActionType[] actionTypes = ActionType.values();
        String[] names = {"Login", "Logout", "Disconnect", "Upload", "Download", "Status Check"};

        for (int i = 0; i < actionTypes.length; i++)
            actionTypeMappings.put(actionTypes[i], names[i]);
    }

    /**
     * The ActionType enum value which caused this exception
     */
    private ActionType actionType;

    /**
     * Constructs an object of this exception with the specified message and ActionType enum value
     * @param message the message for this exception to display
     * @param actionType the ActionType representing the action that was done that caused this exception
     */
    public FTPNotConnectedException(String message, ActionType actionType) {
        super(message);
        this.actionType = actionType;
    }

    /**
     * Constructs an object of this exception with the specified message, an exception that causes this one and the ActionType enum value
     * @param message the message for this exception to display
     * @param e the exception causing this exception
     * @param actionType the ActionType representing the action that was done that caused this exception
     */
    public FTPNotConnectedException(String message, Exception e, ActionType actionType) {
        super(message, e);
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
