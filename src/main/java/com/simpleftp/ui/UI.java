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

import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.paths.PathResolverFactory;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.paths.ResolvedPath;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.exceptions.PathResolverException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.filesystem.paths.interfaces.PathResolver;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.*;
import com.simpleftp.local.exceptions.LocalPathNotFoundException;
import com.simpleftp.ui.dialogs.*;
import com.simpleftp.ui.editor.FileEditorWindow;
import com.simpleftp.ui.panels.FilePanel;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.geometry.Insets;
import org.apache.tika.Tika;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

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
     * A slightly darker CSS white background
     */
    public static final String SMOKE_WHITE_BACKGROUND = "-fx-background-color: #f5f5f5;";

    /**
     * LightGrey background CSS colour
     */
    public static final String GREY_BACKGROUND = "-fx-background-color: lightgrey;";

    /**
     * A more transparent version of GREY_BACKGROUND
     */
    public static final String GREY_BACKGROUND_TRANSPARENT = "-fx-background-color: rgb(211, 211, 211, .4);";

    /**
     * Name for the hide hidden files button
     */
    public static final String HIDE_FILES = "_Hide hidden files";

    /**
     * Name for the show hidden files button
     */
    public static final String SHOW_FILES = "_Show hidden files";

    /**
     * Height of the File panel including FilePanelContainer
     */
    public static final int FILE_PANEL_HEIGHT = 500;

    /**
     * Width of the File panel including FilePanelContainer
     */
    public static final int FILE_PANEL_WIDTH = 570;

    /**
     * The background colour for the file panel container toolbar
     */
    public static final String PANEL_CONTAINER_COLOUR = "-fx-background-color: #cff8fa;";

    /**
     * The height for file editors
     */
    public static final int FILE_EDITOR_HEIGHT = 700;

    /**
     * The width for file editors
     */
    public static final int FILE_EDITOR_WIDTH = 700;

    /**
     * The width for the FilePanelContainer ComboBox
     */
    public static final int PANEL_CONTAINER_COMBO_WIDTH = 170;

    /**
     * The width for the properties window
     */
    public static final int PROPERTIES_WINDOW_WIDTH = 450;

    /**
     * The height for the properties window
     */
    public static final int PROPERTIES_WINDOW_HEIGHT = 400;

    /**
     * The most common insets used in the FilePropertyWindow
     */
    public static final Insets PROPERTIES_WINDOW_INSETS = new Insets(25, 15, 25, 15);

    /**
     * The padding value for the empty directory logo to display in the file panel
     */
    public static final int EMPTY_FOLDER_PANEL_PADDING = 70;

    /**
     * Exit code for if Abort is pressed on an exception dialog
     */
    public static final int EXCEPTION_DIALOG_ABORTED_EXIT_CODE = 1;

    /**
     * The general padding value to use throughout the UI
     */
    public static final int UNIVERSAL_PADDING = 2;

    /**
     * The format for the date time string throughout the UI for files
     */
    public static final String FILE_DATETIME_FORMAT = "MMM dd HH:mm";

    /**
     * An enum to determine which type of dialog to show for a given exception
     */
    public enum ExceptionType {
        ERROR, // treat the exception as a normal error
        EXCEPTION // treat the exception as an unexpected error
    }

    /**
     * The temp directory
     */
    public static final String TEMP_DIRECTORY = System.getProperty("java.io.tmpdir");

    /**
     *
     * The separator for file paths
     */
    public static final String PATH_SEPARATOR = System.getProperty("file.separator");

    /**
     * An enum to determine which path dialog to open
     */
    public enum PathAction {
        GOTO, // the dialog is for going to the directory/file
        CREATE // the dialog is for creating the directory/file
    }

    /**
     * List of background UI tasks running
     */
    private static final ArrayList<Service<?>> backgroundTasks = new ArrayList<>();

    /**
     * List of paths of opened files
     */
    private static final Set<String> openedFiles = new HashSet<>();

    /**
     * Prevent instantiation
     */
    private UI() {}

    /**
     * This method handles the specified exception graphically, showing the ignore button
     * If the exception is expected and not undefined, you should pass in ExceptionType.ERROR. If it is a serious problem, ExceptionType.EXCEPTION
     * @param ex the exception to handle
     * @param exceptionType the type of this exception
     * @param printStackTrace true if the stacktrace should be printed to console
     */
    public static void doException(Exception ex, ExceptionType exceptionType, boolean printStackTrace) {
        doException(ex, exceptionType, printStackTrace, true);
    }

    /**
     * This method handles the specified exception graphically, giving the option to show/hide the ignore button. If you want to force the application to terminate after exception, hide ignore
     * If the exception is expected and not undefined, you should pass in ExceptionType.ERROR. If it is a serious problem, ExceptionType.EXCEPTION
     * @param ex the exception to handle
     * @param exceptionType the type of this exception
     * @param printStackTrace true if the stacktrace should be printed to console
     * @param showIgnoreButton true to show ignore button, false to hide it and closing the dialog terminates the application
     */
    public static void doException(Exception ex, ExceptionType exceptionType, boolean printStackTrace, boolean showIgnoreButton) {
        if (printStackTrace) {
            ex.printStackTrace();
        }

        if (exceptionType == ExceptionType.EXCEPTION) {
            ExceptionDialog exceptionDialog = new ExceptionDialog(ex, showIgnoreButton);
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
                String error = ((FTPException) ex).getReplyString();
                if (error != null && error.equals(""))
                    error = "N/A";
                if (ex instanceof FTPConnectionFailedException) {
                    errorDialog = new ErrorDialog("FTP Connection Error", ex.getMessage() + "\nFTP Error: " + (error == null ? "FTP Connection failed":error));
                } else if (ex instanceof FTPCommandFailedException) {
                    errorDialog = new ErrorDialog("FTP Operation Error", ex.getMessage() + "\nFTP Error: " + error);
                } else if (ex instanceof FTPNotConnectedException) {
                    errorDialog = new ErrorDialog("Not Connected to FTP Server", ex.getMessage() + "\nFTP Error: " + error);
                } else {
                    // this is a FTPError
                    errorDialog = new ErrorDialog("General FTP Error", ex.getMessage() + "\nFTP Error: " + error);
                }

                errorDialog.showAndWait();
            } else if (ex instanceof FileSystemException) {
                Throwable cause = ex.getCause();
                if (cause instanceof FTPException)
                    doException((Exception)cause, ExceptionType.ERROR, false);
            } else if (ex instanceof IOException) {
                doError("I/O Exception occurred", ex.getMessage());
            }
        }
    }

    /**
     * Shows an error dialog with the header text and message text
     * @param headerText the text for the header to display
     * @param messageText the message text to display
     */
    public static void doError(String headerText, String messageText) {
        doError(headerText, messageText, false);
    }

    /**
     * Shows an error dialog with the header text and message text
     * @param headerText the text for the header to display
     * @param messageText the message text to display
     * @param toFront true to show this dialog in front of others
     */
    public static void doError(String headerText, String messageText, boolean toFront) {
        ErrorDialog errorDialog = new ErrorDialog(headerText, messageText);
        if (toFront)
            errorDialog.showAndWait();
        else
            errorDialog.show();
    }

    /**
     * Shows an info dialog with the header text and message text
     * @param headerText the text for the header to display
     * @param messageText the message text to display
     */
    public static void doInfo(String headerText, String messageText) {
        InfoDialog infoDialog = new InfoDialog(headerText, messageText);
        infoDialog.show();
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

    /**
     * Opens a path dialog and returns the path
     * @param action the action to open path dialog as
     * @param directory true to create a directory, false to create a file. If action == GOTO, this boolean can be any value
     * @return the path entered, can be null
     */
    public static String doPathDialog(PathAction action, boolean directory) {
        ChangePathDialog pathDialog;
        if (action == PathAction.GOTO) {
            pathDialog = new ChangePathDialog();
        } else {
            pathDialog = new CreatePathDialog(directory);
        }

        return pathDialog.showAndGetPath();
    }

    /**
     * Shows a rename dialog
     * @param currentFileName the name of the file being renamed
     * @return the entered string
     */
    public static String doRenameDialog(String currentFileName) {
        RenameFileDialog renameFileDialog = new RenameFileDialog(currentFileName);

        return renameFileDialog.showAndGetName();
    }

    /**
     * Shows a text input dialog
     * @param headerText the header text to show
     * @param contentText the text for the content
     * @param currentText the value to initially show, can be null
     * @param subText an optional string that will be shown as a tooltip
     * @return the entered string or null
     */
    public static String doInputDialog(String headerText, String contentText, String currentText, String subText) {
        TextDialog textDialog = new TextDialog(headerText, contentText, currentText, subText);

        return textDialog.showAndGet();
    }

    /**
     * Opens the confirm quit dialog and if true, quits
     */
    public static void doQuit() {
        QuitDialog quitDialog = new QuitDialog();

        if (quitDialog.showAndGetConfirmation()) {
            boolean quit = true;
            if (UI.backgroundTaskInProgress()) {
                quit = new BackgroundTaskRunningDialog().showAndGetConfirmation();
            }

            if (quit) {
                Platform.exit();
                System.exit(0);
            }
        }
    }

    /**
     * Opens the unsaved changes dialog and returns true if wants to save
     */
    public static boolean doUnsavedChanges() {
        UnsavedChangesDialog unsavedChangesDialog = new UnsavedChangesDialog();
        return unsavedChangesDialog.showAndGetConfirmation();
    }

    /**
     * Shows a file editor
     * @param panel the panel opening the editor
     * @param file the file to edit
     * @param fileContents the contents of the file
     */
    public static void showFileEditor(FilePanel panel, CommonFile file, String fileContents) {
        FileEditorWindow editorWindow = new FileEditorWindow(panel, fileContents,file);
        editorWindow.show();
    }

    /**
     * The service to add to the system
     * @param service background task to keep track of
     */
    public static void addBackgroundTask(Service<?> service) {
        backgroundTasks.add(service);
    }

    /**
     * Removes the specified task from the system
     * @param service the service to remove
     */
    public static void removeBackgroundTask(Service<?> service) {
        backgroundTasks.remove(service);
    }

    /**
     * Returns whether there is at least one background task running
     * @return true if a background task is running
     */
    public static boolean backgroundTaskInProgress() {
        for (Service<?> service : backgroundTasks) {
            if (service.isRunning()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Shows a dialog saying the specified file is over 100MB and may cause you to run out of memory if opened in editor
     * @param path the path of the file
     * @return true if editor should be opened, false if not
     */
    public static boolean doFileSizeWarningDialog(String path) {
        FileSizeConfirmationDialog confirmationDialog = new FileSizeConfirmationDialog(path);
        return confirmationDialog.showAndGetConfirmation();
    }

    /**
     * Takes a calendar object, presumably from a FTPFile and uses it to parse it to a time in the format for the UI to display
     * @param calendar the calendar object to convert
     * @return the formatted String
     */
    public static String parseCalendarToUITime(Calendar calendar) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int year = calendar.get(Calendar.YEAR);

        LocalDateTime localDateTime = LocalDateTime.of(year, month, day, hour, minute);
        return localDateTime.format(DateTimeFormatter.ofPattern(FILE_DATETIME_FORMAT));
    }

    /**
     * Retrieves the parent of the provided path
     * @param filePath the path to get the parent of
     * @return the parent path
     */
    public static String getParentPath(String filePath) {
        String parentPath = new File(filePath).getParent();
        String windowsParent;
        parentPath = parentPath == null ? ((windowsParent = System.getProperty("SystemDrive")) != null ? windowsParent:"/"):parentPath; // if windows, find the root

        return parentPath;
    }

    /**
     * The UI supports symbolic links. It regularly needs to check if a CommonFile is a symbolic link.
     * This method provides one place to check this
     * @param file the file to check
     * @return true if symbolic link, false if not
     */
    public static boolean isFileSymbolicLink(CommonFile file) {
        boolean symbolic;

        if (file instanceof LocalFile) {
            symbolic = Files.isSymbolicLink(((LocalFile)file).toPath());
        } else {
            symbolic = ((RemoteFile)file).getFtpFile().isSymbolicLink();
        }

        return symbolic;
    }

    /**
     * Attempts to get the type of the specified file
     * @param file the file to query
     * @return the file type
     */
    private static String getFileType(LocalFile file) throws IOException {
        Tika tika = new Tika();
        return tika.detect(file);
    }

    /**
     * Attempts to determine if the specified file can be opened in an editor
     * @param file the file to check
     * @return true if the file can be opened (txt or xml), false if not or can't be determined
     */
    public static boolean canOpenFile(LocalFile file) {
        try {
            String type = getFileType(file);

            return type.contains("xml") || type.contains("text") || (type.contains("application") && !type.contains("octet-stream") && !type.contains("executable") && !type.contains("java-vm")) || file.length() == 0;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Resolves a local path
     * @param localPath the path to resolve
     * @param currWorkingDir the current working directory
     * @return the resolved path
     * @throws IOException if it fails to be resolved
     */
    public static ResolvedPath resolveLocalPath(String localPath, String currWorkingDir) throws IOException {
        PathResolverFactory resolverFactory = PathResolverFactory.newInstance();
        PathResolver pathResolver = resolverFactory.setLocal(currWorkingDir).build();
        try {
            return pathResolver.resolvePath(localPath);
        } catch (PathResolverException ex) {
            throw (IOException)ex.getWrappedException();
        }
    }

    /**
     * Resolves a remote path
     * @param remotePath the path to resolve
     * @param currWorkingDir the current working directory
     * @param pathExists true if the path already exists (i.e. resolving a path to travel to), false if it is to be created
     * @param connection the connection to resolve the path with. Expected to be non-null and connected and logged in
     * @return the resolved path
     * @throws FTPException if it fails to be resolved
     */
    public static ResolvedPath resolveRemotePath(String remotePath, String currWorkingDir, boolean pathExists, FTPConnection connection) throws FTPException {
        PathResolverFactory resolverFactory = PathResolverFactory.newInstance();
        PathResolver pathResolver = resolverFactory.setRemote(currWorkingDir, connection, pathExists).build();
        try {
            return pathResolver.resolvePath(remotePath);
        } catch (PathResolverException ex) {
            throw (FTPException)ex.getWrappedException();
        }
    }

    /**
     * "Opens" this file by adding its path to the list of opened files. A LineEntry represents a file with a path, so more efficient to just pass in it's path
     * This method doesn't check if the file exists on the file system
     * @param filePath the path of the file that has been opened in the UI. When opening a LineEntry, just call lineEntry.getFilePath to retrieve the path
     */
    public static void openFile(final String filePath) {
        openedFiles.add(filePath);
    }

    /**
     * "Closes" this file by removing its path from the list of opened files. A LineEntry represents a file with a path, so more efficient to just pass in it's path
     * This method does not check if the path exists on the file system
     * @param filePath the path of the file that has been opened in the UI. When opening a LineEntry, just call lineEntry.getFilePath to retrieve the path
     */
    public static void closeFile(final String filePath) {
        openedFiles.remove(filePath);
    }

    /**
     * Checks if the specified file is opened in the UI. A LineEntry represents a file with a path, so more efficient to just pass in it's path.
     * Note that this method does not check if the path exists on the file system
     * @param filePath the path of the file to check. When opening a LineEntry, just call lineEntry.getFilePath to retrieve the path
     * @return true if the file has been opened in the UI, false otherwise
     */
    public static boolean isFileOpened(final String filePath) {
        return openedFiles.contains(filePath);
    }
}
