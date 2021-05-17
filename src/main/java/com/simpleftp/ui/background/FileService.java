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

package com.simpleftp.ui.background;

import com.simpleftp.filesystem.FileOperationError;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.properties.Properties;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.background.scheduling.TaskScheduler;
import com.simpleftp.ui.interfaces.ActionHandler;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class provides a service for doing copy/move operations on files in the background.
 * It is designed to have a local or remote file service for the type of operation required.
 * It uses a copy FTPSystem's connection to do all its operations.
 */
public abstract class FileService extends AbstractDisplayableBackgroundTask {
    /**
     * The source file for the file operation this FileService is providing
     */
    @Getter
    private final CommonFile source;
    /**
     * The destination file for the file operation this FileService is providing
     */
    @Getter
    private final CommonFile destination;
    /**
     * The backing service for this FileService
     */
    private Service<Void> service;
    /**
     * The operation that this FileService will carry out
     */
    protected final Operation operation;
    /**
     * A boolean flag for the service to set if the operation succeeded
     */
    private boolean operationSucceeded;
    /**
     * A FileSystemException if occurred from any operation
     */
    @Getter
    private FileSystemException operationException;
    /**
     * The FileSystem backing the operations this FileService is doing
     */
    protected FileSystem fileSystem;
    /**
     * The action handler to use for when operation succeeds
     */
    private ActionHandler onOperationSucceeded;
    /**
     * The action handler to use for when operation fails
     */
    private ActionHandler onOperationFailed;
    /**
     * The monitor for watching FileOperationErrors
     */
    @Getter
    private final ErrorMonitor errorMonitor;
    /**
     * The bundle this service is a part of if any
     */
    BundledServices bundle;
    /**
     * The scheduler that will schedule this service
     */
    private static final TaskScheduler<CommonFile, FileService> scheduler = new TaskScheduler<>();
    /**
     * Keep track of all scheduled FileServices
     */
    private static final ArrayList<FileService> scheduledServices = new ArrayList<>();
    /**
     * A hash map mapping an operation to a boolean to check if a destination file is required and if not, null can be provided
     */
    private static final HashMap<Operation, Boolean> destinationRequired = new HashMap<>();

    static {
        destinationRequired.put(Operation.COPY, true);
        destinationRequired.put(Operation.MOVE, true);
        destinationRequired.put(Operation.REMOVE, false);
    }

    /**
     * This enum represents the operation that this FileService is to carry out
     */
    public enum Operation {
        /**
         * This enum option represents a copy operation
         */
        COPY,
        /**
         * This enum option represents a move operation
         */
        MOVE,
        /**
         * This enum option represents a remove operation
         */
        REMOVE
    }

    /**
     * Constructs a FileService object with the provided parameters.
     * See the documentation for the file system's copyFiles and moveFiles methods for the rules on the provided parameters
     * @param source the source file that is being copied/moved
     * @param destination the destination directory of this copy/move
     * @param operation the operation for this FileService to complete
     */
    protected FileService(CommonFile source, CommonFile destination, Operation operation) {
        this.source = source;
        this.operation = operation;
        checkDestinationNullity(destination);
        this.destination = destination;
        initService();
        errorMonitor = new ErrorMonitor();
    }

    /**
     * Checks if the destination file is allowed to be null for the given operation type. If destination is null and
     * destinationRequired returns true, a NullPointerException is thrown
     * @param destination the destination file to check
     */
    private void checkDestinationNullity(CommonFile destination) {
        if (destinationRequired.get(operation) && destination == null)
            throw new NullPointerException("The provided destination file is null but for the operation " + operation.toString() + ", a destination file is required");
    }

    /**
     * Initialises the backing service
     */
    private void initService() {
        service = new Service<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() {
                        doOperation();
                        return null;
                    }
                };
            }
        };

        service.setOnFailed(e -> {
            if (!errorMonitor.errorLimitReached) // if we reached our error limit we'd have already called this method
                doCancel(false);
        });
        service.setOnSucceeded(e -> doSuccess());
    }

    /**
     * Sets the handler for when the operation succeeds. If you want to display any success dialogs etc. this handler should
     * do this, otherwise, this service will give no feedback
     * @param onOperationSucceeded the handler to call when the operation succeeds
     * @return this so chaining can be used
     */
    public FileService setOnOperationSucceeded(ActionHandler onOperationSucceeded) {
        this.onOperationSucceeded = onOperationSucceeded;
        return this;
    }

    /**
     * Sets the handler for when the operation fails. If you want to display any error dialogs etc. this handler should
     * do this, otherwise, this service will give no feedback
     * @param onOperationFailed the handler to call when the operation fails
     * @return this so chaining can be used
     */
    public FileService setOnOperationFailed(ActionHandler onOperationFailed) {
        this.onOperationFailed = onOperationFailed;
        return this;
    }

    /**
     * Retrieves the FileSystem to use for this FileService. Lazily initialises this FileService's FileSystem instance
     * @return the file system to use for this FileService
     * @throws FileSystemException if an exception occurs creating the file system
     */
    abstract FileSystem getFileSystem() throws FileSystemException;

    /**
     * Performs the actual copy/move operation
     */
    private void doOperation() {
        try {
            FileSystem fileSystem = getFileSystem();
            errorMonitor.start(); // start the error monitor after we initialise the file system to prevent different filesystems being created if a race condition was to occur
            updateState(State.RUNNING);
            switch (operation) {
                case COPY: operationSucceeded = fileSystem.copyFiles(source, destination);
                            break;
                case MOVE: operationSucceeded = fileSystem.moveFiles(source, destination);
                            break;
                case REMOVE: operationSucceeded = fileSystem.removeFile(source);
                             break;
            }
        } catch (FileSystemException ex) {
            operationSucceeded = false;
            operationException = ex;
            if (FTPSystem.isDebugEnabled())
                ex.printStackTrace();
        }
    }

    /**
     * This operation is called when the copyMoveOperation succeeds
     */
    private void doSuccess() {
        scheduledServices.remove(this);

        if (operationSucceeded) {
            updateState(State.COMPLETED);
            if (onOperationSucceeded != null)
                onOperationSucceeded.doAction();

            if (bundle != null)
                bundle.notify(BundledServices.NotificationType.COMPLETED, this);
        } else {
            updateState(State.FAILED);
            if (onOperationFailed != null)
                onOperationFailed.doAction();

            if (bundle != null)
                bundle.notify(BundledServices.NotificationType.FAILED, this);
        }

        cancelErrorMonitor();

        try {
            FileSystem fileSystem = getFileSystem();
            FTPConnection connection = fileSystem.getFTPConnection();
            if (connection != null && connection.isConnected())
                connection.disconnect();
        } catch (FTPException | FileSystemException ex) {
            if (FTPSystem.isDebugEnabled())
                ex.printStackTrace();
            UI.doError("Service Completion Error", "An error occurred disconnecting the service's connection because: " + ex.getMessage());
        }
    }

    /**
     * Cancels the ErrorMonitor service
     */
    private void cancelErrorMonitor() {
        try {
            errorMonitor.cancel();
        } catch (FileSystemException ex) {
            if (FTPSystem.isDebugEnabled())
                ex.printStackTrace();
        }
    }

    /**
     * Starts the background task and the underlying service.
     *
     * <b>It is preferred to use schedule() over start() since a task started with start() will not be known to subsequent schedule() calls,
     * resulting in scheduling not happening as expected</b>
     */
    @Override
    public void start() {
        updateState(State.STARTED);
        service.start();
    }

    /**
     * Attempts to fully cancel this FileService by disconnection the connection in the file system if it is connected
     * @param wantedCancel true if this was an intended cancel and not a cancel because of an error
     */
    private void doCancel(boolean wantedCancel) {
        try {
            if (!wantedCancel) {
                updateState(State.FAILED);
                if (onOperationFailed != null) {
                    onOperationFailed.doAction();
                } else {
                    UI.doError("Task Failure", "A file service task has failed due to an unknown error");
                }

                if (bundle != null)
                    bundle.notify(BundledServices.NotificationType.FAILED, this);
            } else {
                updateState(State.CANCELLED);
                if (bundle != null)
                    bundle.notify(BundledServices.NotificationType.CANCELLED, this);
            }

            FileSystem fileSystem = getFileSystem();
            FTPConnection connection = fileSystem.getFTPConnection();
            if (connection != null && connection.isConnected())
                connection.disconnect();

            scheduledServices.remove(this);
            cancelErrorMonitor();
        } catch (FTPException | FileSystemException ex) {
            if (FTPSystem.isDebugEnabled())
                ex.printStackTrace();
            UI.doError("Service Cancellation Error", "An error occurred disconnecting the service's connection because: " + ex.getMessage());
            updateState(State.FAILED);
        }
    }

    /**
     * Cancels the background task and the underlying service
     */
    @Override
    public void cancel() {
        service.cancel();
        doCancel(true);
    }

    /**
     * Use this call to determine if a task is ready
     *
     * @return true if ready, false if not
     */
    @Override
    public boolean isReady() {
        return service.getState() == Worker.State.READY;
    }

    /**
     * Checks if there is a task in progress where either the following conditions match:
     * <ol>
     *     <li>This source matches another service with the same source: returns this source file as the key</li>
     *     <li>The destination of this service matches a source of another service: that source is returned</li>
     *     <li>This source matches the destination of another service: that source is returned</li>
     *     <li>This destination matches another services destination: the other service's source is returned</li>
     * </ol>
     * Use this to get the source file to register this service to. Default source file returned is the source of this service
     * @return the source file to register this service to
     */
    private CommonFile getSchedulerKey() {
        CommonFile sourceFile = source;
        for (FileService service : scheduledServices) {
            if (source.equals(service.source)) {
                break;
            } else if (destination != null && destination.equals(service.source)) {
                sourceFile = service.source;
                break;
            } else if (source.equals(service.destination)) {
                sourceFile = service.source;
                break;
            } else if (destination != null && destination.equals(service.destination)) {
                sourceFile = service.source;
                break;
            }
        }

        return sourceFile;
    }

    /**
     * Schedules this FileService to be ran rather than immediately starting it.
     * Use this if you want to avoid conflicts.
     * It schedules the service based on the following criteria:
     * <ol>
     *     <li>This source file matches another service with the same source file: schedules with this source file as the key</li>
     *     <li>The destination file of this service matches a source file of another service: that source file is used as the key</li>
     *     <li>This source file matches the destination file of another service: that source file is used as the key</li>
     *     <li>This destination file matches another services' destination file: the other service's source file is used as the key</li>
     * </ol>
     *
     * This is as far as is reasonably possible to schedule this service. There may be conditions that have not been covered by this and start a service with a
     * different key but that is unavoidable and possibly rare. Some conditions, it may be up to the calling class to ensure there are no conflicting services called through
     * other methods like checking existing FileServices in the UI's background tasks list.
     *
     * <b>It is preferred to call this over start since a task started with start() cannot be seen to subsequent schedule() calls. I.e.
     * A task with the same source file may have been started with start() and then another one with same file started with schedule(). The
     * task started with schedule() will not know about the one started by start() as it is not in the scheduling queue and thus cannot be checked if it is already in
     * progress</b>
     *
     * This should be called from FX thread
     */
    @Override
    public void schedule() {
        scheduler.schedule(getSchedulerKey(), this);
        scheduledServices.add(this);
        updateState(State.SCHEDULED);
        displayTask();
    }

    /**
     * Constructs a new instance of FileService based on the provided parameters.
     * @param source the source file being copied/moved by this FileService
     * @param destination the destination directory being copied/moved to. If the operation is one that involves only one file, leave destination null
     * @param operation the operation for this FileService
     * @param local true if the destination FileSystem is local, false if remote
     * @return the instance of FileService
     */
    public static FileService newInstance(CommonFile source, CommonFile destination, Operation operation, boolean local) {
        if (local) {
            return new LocalFileService(source, destination, operation);
        } else {
            return new RemoteFileService(source, destination, operation);
        }
    }

    /**
     * This class monitors FileOperationErrors in the file system during the operation
     */
    public class ErrorMonitor {
        /**
         * The backing service
         */
        private final Service<Void> service;
        /**
         * Returns true if cancelled
         */
        private boolean cancelled;
        /**
         * A variable that determines the error limit was reacheds
         */
        private boolean errorLimitReached;
        /**
         * The number of error dialogs before it is considered as fatal as if we are getting a lot of errors, something must have went wrong
         */
        private final int maxErrorDialogs = Properties.FILE_OPERATION_ERROR_LIMIT.getValue();

        /**
         * Constructs an ErrorMonitor instance
         */
        ErrorMonitor() {
            service = new Service<>() {
                @Override
                protected Task<Void> createTask() {
                    return new Task<>() {
                        @Override
                        protected Void call() throws Exception {
                            if (!isCancelled()) {
                                FileSystem fileSystem = getFileSystem();

                                int errorsDisplayed = 0;
                                while (!cancelled && !isCancelled()) {
                                    FileOperationError error = fileSystem.getNextFileOperationError();

                                    if (error != null) {
                                        if (errorsDisplayed < maxErrorDialogs) {
                                            displayOperationError(error);
                                            errorsDisplayed++;
                                        } else if (errorsDisplayed == maxErrorDialogs) {
                                            cancelled = errorLimitReached = true;
                                            Platform.runLater(() -> {
                                                if (FileService.this.service.isRunning())
                                                    FileService.this.service.cancel();
                                                FileService.this.doCancel(false);
                                            });
                                        }
                                    }
                                }
                            }

                            return null;
                        }
                    };
                }
            };
        }

        /**
         * Starts this ErrorMonitor
         */
        public void start() {
            service.start();
        }

        /**
         * Displays this file operation error
         * @param fileOperationError the error to display
         */
        private void displayOperationError(FileOperationError fileOperationError) {
            String sourcePath = fileOperationError.getSourcePath();
            String destinationPath = fileOperationError.getDestinationPath();

            String message = fileOperationError.getErrorMessage() + ".";
            if (sourcePath != null)
                message += " File: " + sourcePath;

            if (destinationPath != null)
                message += " Destination: " + destinationPath;

            final String finalMessage = message;
            Platform.runLater(() -> UI.doError("File Operation Error", finalMessage));
        }

        /**
         * Cancels this error monitor displaying all file operations left if any
         */
        public void cancel() throws FileSystemException {
            if (!cancelled) {
                FileSystem fileSystem = getFileSystem();
                while (fileSystem.hasNextFileOperationError())
                    displayOperationError(fileSystem.getNextFileOperationError());

                cancelled = true;
                service.cancel();
            }
        }
    }
}
