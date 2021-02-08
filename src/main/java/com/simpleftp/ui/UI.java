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

package com.simpleftp.ui;

import com.simpleftp.filesystem.FileUtils;
import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.paths.PathResolverFactory;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.exceptions.PathResolverException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.filesystem.paths.interfaces.PathResolver;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.connection.Server;
import com.simpleftp.ftp.exceptions.*;
import com.simpleftp.local.exceptions.LocalPathNotFoundException;
import com.simpleftp.properties.Properties;
import com.simpleftp.security.exceptions.PasswordEncryptionException;
import com.simpleftp.sessions.Sessions;
import com.simpleftp.sessions.exceptions.SessionLoadException;
import com.simpleftp.ui.background.elements.TaskElement;
import com.simpleftp.ui.background.interfaces.BackgroundTask;
import com.simpleftp.ui.background.FileService;
import com.simpleftp.ui.background.interfaces.DisplayableBackgroundTask;
import com.simpleftp.ui.dialogs.*;
import com.simpleftp.ui.exceptions.UIException;
import com.simpleftp.ui.files.DirectoryLineEntry;
import com.simpleftp.ui.files.FileLineEntry;
import com.simpleftp.ui.files.LineEntry;
import com.simpleftp.ui.interfaces.ActionHandler;
import com.simpleftp.ui.directories.DirectoryPane;
import com.simpleftp.ui.editor.FileEditorWindow;
import com.simpleftp.ui.login.LoginWindow;
import com.simpleftp.ui.views.MainView;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventTarget;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import org.apache.tika.Tika;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class provides static util methods and constants for UI as well as background task management and tracking
 * if a file is opened or not
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
    public static final String HIDE_FILES = "Hide hidden files";

    /**
     * Name for the show hidden files button
     */
    public static final String SHOW_FILES = "Show hidden files";

    /**
     * Height of the File panel
     */
    public static final int FILE_PANEL_HEIGHT = 600;

    /**
     * Width of the File panel
     */
    public static final int FILE_PANEL_WIDTH = 570;

    /**
     * Width for the panel view
     */
    public static final int PANEL_VIEW_WIDTH = (FILE_PANEL_WIDTH * 2);

    /**
     * Height for the panel view
     */
    public static final int PANEL_VIEW_HEIGHT = FILE_PANEL_HEIGHT + 15;

    /**
     * Height for main view
     */
    public static final int MAIN_VIEW_HEIGHT = PANEL_VIEW_HEIGHT + 100;

    /**
     * Width for main view
     */
    public static final int MAIN_VIEW_WIDTH = PANEL_VIEW_WIDTH;

    /**
     * The background colour for the file panel toolbar
     */
    public static final String PANEL_COLOUR = "-fx-background-color: #cff8fa;";

    /**
     * The width for the DirectoryPane ComboBox
     */
    public static final int PANEL_COMBO_WIDTH = 195;

    /**
     * The width for the properties window
     */
    public static final int PROPERTIES_WINDOW_WIDTH = 450;

    /**
     * The name of the operating system
     */
    public static final String OS_NAME = System.getProperty("os.name");

    /**
     * The height for the properties window without permissions
     */
    public static final int PROPERTIES_WINDOW_HEIGHT = 400;

    /**
     * The height for the properties window with permissions
     */
    public static final int PROPERTIES_WINDOW_HEIGHT_PERMISSIONS = PROPERTIES_WINDOW_HEIGHT + 120;

    /**
     * The most common insets used in the FilePropertyWindow
     */
    public static final Insets PROPERTIES_WINDOW_INSETS = new Insets(25, 15, 25, 15);

    /**
     * The padding value for the empty directory logo to display in the file panel
     */
    public static final int EMPTY_FOLDER_PANEL_PADDING = 70;

    /**
     * The height for the login window scene
     */
    public  static final int LOGIN_WINDOW_HEIGHT = 370;

    /**
     * The width for the login window scene
     */
    public static final int LOGIN_WINDOW_WIDTH = 400;

    /**
     * The width of the tasks window
     */
    public static final int TASKS_WINDOW_WIDTH = 640;

    /**
     * The height of the tasks window
     */
    public static final int TASKS_WINDOW_HEIGHT = 300;

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
     * List of background UI tasks running
     */
    private static final ArrayList<BackgroundTask> backgroundTasks = new ArrayList<>();

    /**
     * This property represents that there are background tasks tracked by the UI
     */
    @Getter
    private static final BooleanProperty backgroundTasksProperty = new SimpleBooleanProperty(false);

    /**
     * A hashmap to map a background task with its TaskElement representation for easy lookup
     */
    private static final HashMap<DisplayableBackgroundTask, TaskElement> displayedBackgroundTasks = new HashMap<>();

    /**
     * List of paths of opened remote files
     */
    private static final Set<String> openedRemoteFiles = new HashSet<>();

    /**
     * Lost of paths of opened local files
     */
    private static final Set<String> openedLocalFiles = new HashSet<>();

    /**
     * The Scene this application is being displayed in.
     * Should be set in the start() method of the main class
     */
    @Getter
    private static Scene applicationScene;

    /**
     * The MainView that is the basis of the UI for this application.
     * This is the window that the user interacts with. It contains all the separate components in one place.
     *
     * The value of this shouldn't be changed outside of the startApplication method
     */
    @Getter
    private static MainView mainView;
    /**
     * A boolean flag to keep track of if application has been started
     */
    private static boolean applicationStarted;

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
                handlePathNotFoundException(ex);
            } else if (ex instanceof FTPException) {
                handleFTPException(ex);
            } else if (ex instanceof FileSystemException) {
                handleFileSystemException(ex);
            } else if (ex instanceof IOException) {
                doError("I/O Exception occurred", ex.getMessage());
            } else if (ex instanceof UIException) {
                handleUIException(ex);
            } else {
                doException(ex, ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled()); // we have an unknown exception, show an exception dialog
            }
        }
    }

    /**
     * Handles displaying an exception related to a path not being found
     * @param ex the exception to display
     */
    private static void handlePathNotFoundException(Exception ex) {
        String path;
        String headerPrefix;
        if (ex instanceof FTPRemotePathNotFoundException) {
            path = ((FTPRemotePathNotFoundException)ex).getRemotePath();
            headerPrefix = "Remote ";
        } else {
            path = ((LocalPathNotFoundException)ex).getLocalPath();
            headerPrefix = "Local ";
        }
        ErrorDialog errorDialog = new ErrorDialog(headerPrefix + "file does not exist", "The path " + path + " could not be found");
        errorDialog.showAndWait();
    }

    /**
     * Handles displaying of a FTPException
     * @param ex the exception to display
     */
    private static void handleFTPException(Exception ex) {
        ErrorDialog errorDialog;
        String error = ((FTPException) ex).getReplyString();
        String ftpErrorString = error != null ? "\nFTP Error: " + error:"";

        if (ex instanceof FTPConnectionFailedException) {
            errorDialog = new ErrorDialog("FTP Connection Error", ex.getMessage() + (ftpErrorString.equals("") ? "\nFTP Error: FTP Connection Failed":ftpErrorString));
        } else if (ex instanceof FTPCommandFailedException) {
            errorDialog = new ErrorDialog("FTP Operation Error", ex.getMessage() + ftpErrorString);
        } else if (ex instanceof FTPNotConnectedException) {
            errorDialog = new ErrorDialog("Not Connected to FTP Server", ex.getMessage() + ftpErrorString);
        } else {
            // this is a FTPError
            errorDialog = new ErrorDialog("General FTP Error", ex.getMessage() + ftpErrorString);
        }

        errorDialog.showAndWait();
    }

    /**
     * Handles displaying a FileSystemException
     * @param ex the exception to display
     */
    private static void handleFileSystemException(Exception ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof FTPException)
            doException((Exception)cause, ExceptionType.ERROR, false);
    }

    /**
     * Handles an UIException by displaying the underlying exception
     * @param ex the exception to display
     */
    private static void handleUIException(Exception ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof Exception)
            doException((Exception)cause, ExceptionType.ERROR, FTPSystem.isDebugEnabled());
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
        return doConfirmation(headerText, messageText, false);
    }

    /**
     * Opens a confirmation dialog but gives the option to open it as a warning or normal confirmation
     * @param headerText the text to display in the header
     * @param messageText the message to display
     * @param warning true to display as a warning, false to display as a normal confirmation dialog
     * @return true if confirmed, false otherwise
     */
    public static boolean doConfirmation(String headerText, String messageText, boolean warning) {
        ConfirmationDialog confirmationDialog = warning ? new WarnConfirmationDialog(headerText, messageText):new ConfirmationDialog(headerText, messageText);
        return confirmationDialog.showAndGetChoice();
    }

    /**
     * Opens a path dialog and returns the path
     * @return the path entered, can be null
     */
    public static String doPathDialog() {
        ChangePathDialog pathDialog = new ChangePathDialog();

        return pathDialog.showAndGetPath();
    }

    /**
     * Opens a create file dialog.
     * @param directory true if creating a directory, false if not
     * @param altEnterHandler the handler to perform when Alt + Enter is pressed, can be null
     * @return the created path
     */
    public static String doCreateDialog(boolean directory, ActionHandler altEnterHandler) {
        CreatePathDialog createPathDialog = new CreatePathDialog(directory);
        if (altEnterHandler != null) {
            AtomicBoolean altPressed = new AtomicBoolean(false);
            Button okButton = createPathDialog.lookupButton(ButtonType.OK);
            okButton.setTooltip(new Tooltip("Press Alt + Enter to open created " + (directory ? "directory":"file")));
            createPathDialog.getDialogPane().setOnKeyPressed(e -> {
                altPressed.set(e.isAltDown());
                if (e.getCode() == KeyCode.ENTER && altPressed.get()) {
                    okButton.fire();
                    altEnterHandler.doAction();
                }
            });
        }

        return createPathDialog.showAndGetPath();
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
     * Call this method to perform the specified action if quit is specified
     * @param actionHandler the action to perform, null if no action requested
     */
    public static void doQuit(ActionHandler actionHandler) {
        QuitDialog quitDialog = new QuitDialog();

        if (quitDialog.showAndGetConfirmation()) {
            boolean quit = true;
            if (UI.backgroundTaskInProgress()) {
                quit = new BackgroundTaskRunningDialog().showAndGetConfirmation();

                if (!quit)
                    mainView.getTaskWindow().show();
            }

            if (quit) {
                if (actionHandler != null)
                    actionHandler.doAction();

                Platform.exit();
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
     * @param lineEntry the line entry to edit
     * @param fileContents the contents of the file
     */
    public static void showFileEditor(DirectoryPane panel, LineEntry lineEntry, String fileContents) {
        FileEditorWindow editorWindow = FileEditorWindow.newInstance(panel, fileContents, lineEntry);
        editorWindow.show();
    }

    /**
     * Adds a task to the UI to be tracked.
     * If this is called inside BackgroundTask#start(), it should be called on the FX thread. Else, if called in BackgroundTask#schedule,
     * it is ok to call without a Platform.runLater.
     *
     * If the background task is an instance of DisplayableBackgroundTask, an U.I element is created for it and displayed in the
     * MainView's instance of TaskWindow
     * @param backgroundTask background task to keep track of
     */
    public synchronized static void addBackgroundTask(BackgroundTask backgroundTask) {
        if (!applicationStarted)
            throw new IllegalStateException("The Application has not been started yet, so cannot add a BackgroundTask to the system. " +
                    "Call UI.startApplication(Stage primaryStage) first");

        backgroundTasks.add(backgroundTask);

        if (backgroundTask instanceof DisplayableBackgroundTask) {
            DisplayableBackgroundTask displayableTask = (DisplayableBackgroundTask)backgroundTask;
            TaskElement taskElement = new TaskElement(displayableTask);
            displayedBackgroundTasks.put(displayableTask, taskElement);
            mainView.getTaskWindow().getTaskPane().addTask(taskElement);
            backgroundTasksProperty.setValue(true);
        }
    }

    /**
     * Removes the specified task from the system.
     *
     * If backgroundTask is an instance of DisplayableBackgroundTask, it will be removed from MainView's
     * TaskWindow instance
     * @param backgroundTask the background task to remove
     */
    public synchronized static void removeBackgroundTask(BackgroundTask backgroundTask) {
        if (!applicationStarted)
            throw new IllegalStateException("The Application has not been started yet, so cannot remove a BackgroundTask from the system. " +
                    "Call UI.startApplication(Stage primaryStage) first");

        backgroundTasks.remove(backgroundTask);

        if (backgroundTask instanceof DisplayableBackgroundTask) {
            DisplayableBackgroundTask displayableTask = (DisplayableBackgroundTask)backgroundTask;
            TaskElement taskElement = displayedBackgroundTasks.get(displayableTask);

            if (taskElement != null) {
                mainView.getTaskWindow().getTaskPane().removeTask(taskElement);
                displayedBackgroundTasks.remove(displayableTask);
            }
        }

        backgroundTasksProperty.setValue(!displayedBackgroundTasks.isEmpty());
    }

    /**
     * Returns whether there is at least one background task running
     * @return true if a background task is running
     */
    public static boolean backgroundTaskInProgress() {
        return backgroundTasks.stream()
                .anyMatch(BackgroundTask::isRunning);
    }

    /**
     * Returns an unmodifiable view of the background tasks this UI class is holding
     * @return unmodifiable view of the background tasks
     */
    public static List<BackgroundTask> getBackgroundTasks() {
        return Collections.unmodifiableList(backgroundTasks);
    }

    /**
     * Checks if file matches file1 by the isLocal method and file's filePath starts with file1's filePath
     * @param file the first file to compare
     * @param file1 the file that the first file is matched against
     * @return true if they match, false if not
     */
    private static boolean taskFileEquals(CommonFile file, CommonFile file1) {
        boolean fileLocal = file1.isLocal();
        return fileLocal == file1.isLocal() && FileUtils.startsWith(file.getFilePath(), file1.getFilePath(), fileLocal); // if the file's filePath starts with the file1's path, we have a sub-file of file and file is a directory
    }

    /**
     * Determines whether the specified file is "locked" by a FileService task.
     * It is determined locked, if there exists a FileService background task with a source file matching the file. The source file could be a parent of the file
     * @param file the file to check if it is locked
     * @return true if locked, false if not
     */
    public static boolean isFileLockedByFileService(CommonFile file) {
        List<BackgroundTask> tasks = getBackgroundTasks();
        if (tasks.size() == 0)
            return false;
        else
            return tasks.stream()
                    .filter(task -> task instanceof FileService)
                    .map(task -> (FileService) task)
                    .anyMatch(task -> taskFileEquals(file, task.getSource()));
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
     * Shows the dialog that the specified path is a symbolic link
     * @param path the path to the symbolic link
     * @return true if the user wants to go to the path, false if they want to go to the target
     */
    public static boolean doSymbolicPathDialog(String path) {
        SymbolicPathDialog symbolicPathDialog = new SymbolicPathDialog(path);
        return symbolicPathDialog.showAndGetChoice();
    }

    /**
     * Shows the dialog to create a symbolic link and returns the pair of paths. First of the pair being the target, second being the link name
     * @return the pair of paths, first is target path, second is name of link
     */
    public static Pair<String, String> doCreateSymLinkDialog() {
        CreateSymLinkDialog symLinkDialog = new CreateSymLinkDialog();
        return symLinkDialog.showAndGetEntry();
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

            return type.contains("xml") || type.contains("text")
                    || (type.contains("application") && !type.contains("octet-stream")
                    && !type.contains("executable") && !type.contains("java-vm")
                    && !type.contains("sharedlib")) && !type.contains("lib")
                    && !type.contains("zip") && !type.contains("archive") || file.length() == 0;
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
    public static String resolveLocalPath(String localPath, String currWorkingDir) throws IOException {
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
    public static String resolveRemotePath(String remotePath, String currWorkingDir, boolean pathExists, FTPConnection connection) throws FTPException {
        PathResolverFactory resolverFactory = PathResolverFactory.newInstance();
        PathResolver pathResolver = resolverFactory.setRemote(currWorkingDir, connection, pathExists).build();
        try {
            return pathResolver.resolvePath(remotePath);
        } catch (PathResolverException ex) {
            throw (FTPException)ex.getWrappedException();
        }
    }

    /**
     * Resolves a symbolic path
     * @param path the path to resolve. Expected to be absolute
     * @param pathSeparator the path separator to use
     * @param root the root of the path if it is not path separator, instead leave null
     * @return the resolved path, null if an unexpected exception occurs
     */
    public static String resolveSymbolicPath(String path, String pathSeparator, String root) {
        PathResolverFactory resolverFactory = PathResolverFactory.newInstance();
        PathResolver pathResolver = resolverFactory.setSymbolic(pathSeparator, root).build();
        try {
            return pathResolver.resolvePath(path);
        } catch (PathResolverException ex) {
            UI.doException(ex, ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled()); // show exception dialog as this shouldn't happen for symbolic resolving
        }

        return null;
    }

    /**
     * "Opens" this file by adding its path to the list of opened files. A LineEntry represents a file with a path, so more efficient to just pass in it's path
     * This method doesn't check if the file exists on the file system. It also doesn't open the window/do an action of opening on the file.
     * This method simply is to just tell the UI that you have opened this file elsewhere so that other methods calling isFileOpened will know about it
     * and so can block the action if it is an action that isn't allowed while a file is open, e.g. delete/rename.
     * It is a way of tracking that the file has been opened. This call should be made everytime you open a file (like clicking into a directory or opening an editor). Example, opening a property window is not opening the file, because opening means either opening a directory or a file to view the contents
     * @param filePath the path of the file that has been opened in the UI. When opening a LineEntry, just call lineEntry.getFilePath to retrieve the path
     * @param local true if this is a local path, false if remote
     */
    public static void openFile(final String filePath, boolean local) {
        Set<String> openedFiles = local ? openedLocalFiles:openedRemoteFiles;
        openedFiles.add(filePath);
    }

    /**
     * "Closes" this file by removing its path from the list of opened files. A LineEntry represents a file with a path, so more efficient to just pass in it's path
     * This method does not check if the path exists on the file system.
     * It also doesn't close any window/do an action of closing on the file.
     * This method simply is to just tell the UI that you have now closed this file elsewhere to tell other methods calling isFileOpened that they can now perform an action on the file
     * that would otherwise not be allowed if isFileOpened returned true, e.g. if you want to delete a file, and a call to openFile for that path was made, you need to call closeFile before you can delete it.
     * It is a way of tracking that the window is now closed. Should be made when leaving a file after it was opened (directory opened or file opened to view the contents, not opening a properties window for example)
     * @param filePath the path of the file that has been opened in the UI. When opening a LineEntry, just call lineEntry.getFilePath to retrieve the path
     * @param local true if this is a local path, false if remote
     */
    public static void closeFile(final String filePath, boolean local) {
        Set<String> openedFiles = local ? openedLocalFiles:openedRemoteFiles;
        openedFiles.remove(filePath);
    }

    /**
     * Removes all files from the specified opened files path
     * @param local true if the path is local, false if not
     */
    public static void closeAllFiles(boolean local) {
        Set<String> openedFiles = local ? openedLocalFiles:openedRemoteFiles;
        openedFiles.clear();
    }

    /**
     * Checks if the specified file is opened in the UI. A LineEntry represents a file with a path, so more efficient to just pass in it's path.
     * Note that this method does not check if the path exists on the file system
     * @param filePath the path of the file to check. When opening a LineEntry, just call lineEntry.getFilePath to retrieve the path
     * @param local true if this is a local path, false if remote
     * @return true if the file has been opened in the UI, false otherwise
     */
    public static boolean isFileOpened(final String filePath, boolean local) {
        Set<String> openedFiles = local ? openedLocalFiles:openedRemoteFiles;
        return openedFiles.contains(filePath);
    }

    /**
     * Display an error message if connection of the system connection fails
     */
    public static void doConnectionError() {
        FTPConnection connection = FTPSystem.getConnection();
        if (connection != null) {
            Server server = connection.getServer();
            String error = connection.getReplyString();
            error = error == null || error.isEmpty() ? "Unknown Error. The server may not exist or is not responding":error;
            UI.doError("Connection Error", "Failed to connect to server " + server.getServer() + " with user " + server.getUser() + " and port " + server.getPort()
                    + ". FTP Error: " + error);
        }
    }

    /**
     * Initialises the Sessions functionality. This wraps the steps and error-handling initialisation may require.
     * This should be called to initialise the Sessions package so that sessions can be retrieved using Sessions.getSession etc.
     * This method, thus, does not set the current session
     */
    public static boolean initialiseSessions() {
        boolean disabled = Sessions.isDisabled();

        if (!disabled) {
            return doSessionInitialisation();
        }

        return false;
    }

    /**
     * Displays error messages related to initialisation
     * @param errorMessage an error message to display, leave null to generate a message based on a Sessions.getExistingLoadException()
     * @return true if initialised false if not
     */
    private static boolean doInitialisationError(String errorMessage) {
        boolean initialised = false;
        SessionLoadException exception = Sessions.getExistingLoadException();

        if (errorMessage == null)
            errorMessage = exception != null ? "Failed because of: " + exception.getMessage() + ". " : "";
        else
            errorMessage = "Failed because of: " + errorMessage;

        if (UI.doConfirmation("Sessions Initialisation Failure", "Failed to initialise Sessions functionality correctly."
                + " It may be due to a corrupted sessions file on the disk. " + errorMessage + "Removing the existing sessions file and re-initialising "
                + "can fix the issue. Do you wish to do this? It cannot be undone", true)) {
            Sessions.initialise(true);
            initialised = Sessions.isInitialised();

            if (!initialised)
                UI.doError("Sessions Initialisation Failure", "Failed to initialise Sessions again. Proceeding without Session functionality");
        } else {
            UI.doInfo("Sessions Initialisation Cancelled", "Cancelling Sessions initialisation as user rejected starting Sessions with a new sessions file");
        }

        return initialised;
    }

    /**
     * A private method to carry out the initialisation logic. This does not check if sessions are disabled or not
     * @return true if initialised successfully, false if not
     */
    private static boolean doSessionInitialisation() {
        boolean initialised = Sessions.isInitialised();
        if (!initialised) {
            try {
                Sessions.initialise(false);

                initialised = Sessions.isInitialised();

                if (!initialised) {
                    initialised = doInitialisationError(null);
                }
            } catch (PasswordEncryptionException ex) {
                if (FTPSystem.isDebugEnabled())
                    ex.printStackTrace();

                return doInitialisationError("Password decryption failure. Invalid key may have been used (i.e. existing sessions file was generated " +
                        "from a different installation). ");
            }
        }

        return initialised;
    }

    /**
     * This method starts the login workflow by displaying this class' MainView instance's LoginWindow.
     * If you want sessions enabled before displaying the loginWindow, call enableSessions on the main view instance before displaying the login window
     */
    public static void doLogin() {
        if (!applicationStarted)
            throw new IllegalStateException("The Application has not been started yet, so cannot display a login window. Call UI.startApplication(Stage primaryStage) first");

        LoginWindow loginWindow = mainView.getLoginWindow();
        if (loginWindow == null)
            throw new IllegalStateException("A LoginWindow has not been set on the UI's MainView instance");

        loginWindow.show();
    }

    /**
     * Starts the application with the primary stage passed into the Application.start() method.
     * This should only be called once.
     * It initialises the Application Scene also which can be retrieved using UI.getApplicationScene()
     * @param primaryStage the primary stage passed into the start() method.
     * @throws UIException if the UI fails to be initialised
     */
    public static void startApplication(Stage primaryStage) throws UIException {
        if (applicationStarted)
            throw new IllegalStateException("The application has already been started, you cannot start the application twice in one JVM instance");
        applicationStarted = true;

        MainView.deferSessionInitialisation(); // in case there's issues initialising sessions, defer MainView's enableSessions() method as construction of LoginWindow may call this
        mainView = MainView.getInstance();

        Scene scene = new Scene(mainView, UI.MAIN_VIEW_WIDTH, UI.MAIN_VIEW_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.setTitle("SimpleFTP Client");
        primaryStage.setResizable(false);
        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> {
            mainView.quit();
            e.consume(); // consume if we don't want to quit
        });

        applicationScene = scene;
        doLogin();
    }

    /**
     * This class is used to provide static methods in the context of the UI for the help of processing any UI events
     * such as mouse events etc.
     */
    public static final class Events {
        /**
         * The colour for when drag enters any other object
         */
        public static final String DRAG_ENTERED_BACKGROUND = "-fx-background-color: rgb(135,206,250, 0.4);";

        /**
         * The colour for when drag enters an object where there's already a drag entered colour
         */
        public static final String DRAG_ENTERED_BACKGROUND_CLEAR = "-fx-background-color: rgb(135,206,250, 0.0);";

        /**
         * This set contains the classes that are supported drag and drop targets
         */
        private static final Set<Class<?>> DRAG_DROP_TARGETS = new HashSet<>();

        static {
            DRAG_DROP_TARGETS.add(DirectoryPane.UpButton.class);
            DRAG_DROP_TARGETS.add(DirectoryPane.EntriesBox.class);
            DRAG_DROP_TARGETS.add(DirectoryLineEntry.class);
        }

        /**
         * If a drag event is in progress, this determines the cursor hand to use in a move
         */
        @Getter
        @Setter
        private static boolean dragInProgress = false;

        /**
         * This method attempts to select the LineEntry that may have been picked by the MouseEvent target.
         * If the target is a LineEntry, it is returned. If it is not, the parent of it is checked to be a LineEntry.
         * <p>
         * If it cannot be determined, or the mouse event does not match a line entry, this returns null.
         * <p>
         * This was required as when clicking a LineEntry, most of the time the target would be the text (or image view) inside it.
         * We need a method to determine the LineEntry that those clicked objects are a part of. This method provides the logic to do so.
         * Events do not give enough control in picking the specific element so we need logic to "select" the LineEntry based on the actual object
         * that was clicked inside in the LineEntry (since a LineEntry extends HBox, it can be a parent). For example,
         * if the permissions string (a text object) is selected, the parent of that Text object will be the LineEntry. This
         * method selects the LineEntry from the mouse event.
         * <p>
         * Whenever working with mouse events and Line entries, you should always call this method on the mouse event to get the LineEntry clicked.
         *
         * @param mouseEvent the mouse event to process
         * @return the LineEntry if found, null if not
         */
        public static LineEntry selectLineEntry(MouseEvent mouseEvent) {
            return selectLineEntry(mouseEvent.getTarget());
        }

        /**
         * Determines if this event target represents a LineEntry and returns it
         * @param target the event target to extract the LineEntry from
         * @return the line entry if found, null if now
         */
        public static LineEntry selectLineEntry(EventTarget target) {
            if (target instanceof LineEntry) {
                return (LineEntry)target;
            } else {
                Parent parent = ((Node)target).getParent();

                if (parent instanceof LineEntry) {
                    return (LineEntry)parent;
                } else {
                    return null;
                }
            }
        }

        /**
         * Selects a line entry from the provided object if it is an instance of EventTarget
         * @param object the object to select line entry from
         * @return the selected LineEntry if found, null if not EventTarget or not found
         */
        public static LineEntry selectLineEntry(Object object) {
            if (object instanceof EventTarget)
                return selectLineEntry(((EventTarget)object));
            else
                return null;
        }

        /**
         * This method attempts to identify if the target is a valid target, i.e. directory line entry or EntriesBox
         * @param target the target of the event
         * @return true if a valid target, false if not
         */
        public static boolean validDragAndDropTarget(EventTarget target) {
            if (!DRAG_DROP_TARGETS.contains(target.getClass())) {
                if (target instanceof FileLineEntry)
                    return validDragAndDropTarget(((LineEntry)target).getParent());
                else
                    return false;
            } else {
                return true;
            }
        }

        /**
         * Gets the directory pane from the given object
         * @param eventObject the object to find directory pane from
         * @return the found directory pane, null if not found
         */
        public static DirectoryPane getDirectoryPane(Object eventObject) {
            if (eventObject instanceof Node) {
                Node node = (Node)eventObject;
                if (node instanceof DirectoryPane) {
                    return (DirectoryPane)node;
                } else if (node instanceof DirectoryPane.EntriesBox) {
                    return ((DirectoryPane.EntriesBox)eventObject).getDirectoryPane();
                } else {
                    LineEntry lineEntry = selectLineEntry(eventObject);

                    if (lineEntry != null)
                        return lineEntry.getOwningPane();
                    else
                        return null;
                }
            } else {
                return null;
            }
        }

        /**
         * Sets the cursor image for the drag event based on the source line entry if the Application Scene is not null
         * @param source the source line entry. This is ignored if DRAG_DROP_CURSOR_FILE_ICON property is false
         */
        public static void setDragCursorImage(LineEntry source) {
           if (applicationScene != null && dragInProgress) {
               if (!Properties.DRAG_DROP_CURSOR_FILE_ICON.getValue()) {
                   applicationScene.setCursor(Cursor.MOVE);
               } else {
                   boolean symlink = source.getFile().isSymbolicLink();
                   Image image = source.isDirectory() ? new Image(symlink ? "dir_icon_symlink.png" : "dir_icon.png") : new Image(symlink ? "file_icon_symlink.png" : "file_icon.png");
                   applicationScene.setCursor(new ImageCursor(image));
               }
            }
        }

        /**
         * Sets the cursor image for the drag entry event based on the source line entry if the Application Scene is not null
         * if DRAG_DROP_CURSOR_FILE_ICON property is false
         */
        public static void setDragCursorEnteredImage() {
            if (applicationScene != null && dragInProgress) {
                if (!Properties.DRAG_DROP_CURSOR_FILE_ICON.getValue()) {
                    applicationScene.setCursor(Cursor.OPEN_HAND);
                }
            }
        }

        /**
         * If Application scene is not null, the mouse cursor is resetConnection to default
         */
        public static void resetMouseCursor() {
            if (applicationScene != null && dragInProgress)
                applicationScene.setCursor(Cursor.DEFAULT);
        }
    }
}
