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

package com.simpleftp.ui;

import com.simpleftp.ftp.exceptions.*;
import com.simpleftp.local.exceptions.LocalPathNotFoundException;
import com.simpleftp.ui.dialogs.ConfirmationDialog;
import com.simpleftp.ui.dialogs.ErrorDialog;
import com.simpleftp.ui.dialogs.ExceptionDialog;
import com.simpleftp.ui.dialogs.InfoDialog;

/**
 * This class provides static util methods and constants for UI
 */
public final class UI {
    /**
     * The size for file and directory icons
     */
    public static final double FILE_ICON_SIZE = 30;

    /**
     * White CSS background colour
     */
    public static final String WHITE_BACKGROUND = "-fx-background-color: white;";

    /**
     * LightGrey background CSS colour
     */
    public static final String GREY_BACKGROUND = "-fx-background-color: lightgrey;";

    /**
     * You need to have a flag to enable/disable double click action on file entries inside in entriesBox
     * Therefore, if the statusPanel is clicked (panel or buttons), doubleClickEnabled is set to false, so double clicks here don't propagate to line entries
     * On click event into entriesBox, this should be reset to true
     *
     * This resolves GitHub issue #42
     */
    private static boolean MOUSE_EVENTS_ENABLED = true;

    /**
     * An enum to determine which type of dialog to show for a given exception
     */
    public enum ExceptionType {
        ERROR, // treat the exception as a normal error
        EXCEPTION // treat the exception as an unexpected error
    }

    /**
     * This method handles the specified exception graphically.
     * If the exception is expected and not undefined, you should pass in ExceptionType.ERROR. If it is a serious problem, ExceptionType.EXCEPTION
     * @param ex the exception to handle
     * @param exceptionType the type of this exception
     * @param printStackTrace true if the stacktrace should be printed to console
     */
    public static void doException(Exception ex, ExceptionType exceptionType, boolean printStackTrace) {
        if (printStackTrace) {
            ex.printStackTrace();
        }

        if (exceptionType == ExceptionType.EXCEPTION) {
            ExceptionDialog exceptionDialog = new ExceptionDialog(ex);
            exceptionDialog.showAndDoAction();
        } else {
            // more exception types can be handled here
            if (ex instanceof FTPRemotePathNotFoundException || ex instanceof LocalPathNotFoundException) {
                String path;
                String headerPrefix = "";
                if (ex instanceof FTPRemotePathNotFoundException) {
                    path = ((FTPRemotePathNotFoundException)ex).getRemotePath();
                    headerPrefix = "Remote ";
                } else {
                    path = ((LocalPathNotFoundException)ex).getLocalPath();
                    headerPrefix = "Local ";
                }
                ErrorDialog errorDialog = new ErrorDialog(headerPrefix + "file does not exist", "The path " + path + " could not be found");
                errorDialog.showAndWait();
            } else if (ex instanceof FTPException) {
                ErrorDialog errorDialog;
                if (ex instanceof FTPConnectionFailedException) {
                    errorDialog = new ErrorDialog("FTP Connection Error", ex.getMessage());
                } else if (ex instanceof FTPCommandFailedException) {
                    errorDialog = new ErrorDialog("FTP Operation Error", ex.getMessage());
                } else if (ex instanceof FTPNotConnectedException) {
                    errorDialog = new ErrorDialog("Not Connected to FTP Server", ex.getMessage());
                } else {
                    // this is a FTPError
                    errorDialog = new ErrorDialog("General FTP Error", ex.getMessage());
                }

                errorDialog.showAndWait();
            }
        }
    }

    /**
     * Shows an error dialog with the header text and message text
     * @param headerText the text for the header to display
     * @param messageText the message text to display
     */
    public static void doError(String headerText, String messageText) {
        ErrorDialog errorDialog = new ErrorDialog(headerText, messageText);
        errorDialog.showAndWait();
    }

    /**
     * Shows an info dialog with the header text and message text
     * @param headerText the text for the header to display
     * @param messageText the message text to display
     */
    public static void doInfo(String headerText, String messageText) {
        InfoDialog infoDialog = new InfoDialog(headerText, messageText);
        infoDialog.showAndWait();
    }

    /**
     * Opens a confirmation dialog
     * @param headerText the text to display in the header
     * @param messageText the message to display
     * @return true if confirmed, false otherwise
     */
    public static boolean doConfirmation(String headerText, String messageText) {
        ConfirmationDialog confirmationDialog = new ConfirmationDialog(headerText, messageText);
        return confirmationDialog.showAndGetChoice();
    }
}
