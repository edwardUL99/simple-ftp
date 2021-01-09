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

import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.filesystem.interfaces.FileSystem;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.background.scheduling.TaskScheduler;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import lombok.Getter;

import java.util.ArrayList;

/**
 * This class provides a service for doing copy/move operations on files in the background.
 * It is designed to have a local or remote file service for the type of copy/move required.
 * It uses a copy FTPSystem's connection to do all its operations.
 */
public abstract class FileService implements BackgroundTask {
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
     * A boolean flag to determine if this FileService is for copying or moving files
     */
    private final boolean copy;
    /**
     * A boolean flag for the service to set if the operation succeeded
     */
    private boolean operationSucceeded;
    /**
     * A FileSystemException if occurred from any operation
     */
    private FileSystemException operationException;
    /**
     * The FileSystem backing the operations this FileService is doing
     */
    protected FileSystem fileSystem;
    /**
     * A boolean determining if the service is finished or not
     */
    private boolean finished;
    /**
     * The scheduler that will schedule this service
     */
    private static final TaskScheduler<CommonFile, FileService> scheduler = new TaskScheduler<>();
    /**
     * Keep track of all scheduled FileServices
     */
    private static final ArrayList<FileService> scheduledTasks = new ArrayList<>();

    /**
     * Constructs a FileService object with the provided parameters.
     * See the documentation for the file system's copyFiles and moveFiles methods for the rules on the provided parameters
     * @param source the source file that is being copied/moved
     * @param destination the destination directory of this copy/move
     * @param copy true if the source file is to be copied, false if it is to be moved
     */
    protected FileService(CommonFile source, CommonFile destination, boolean copy) {
        this.source = source;
        this.destination = destination;
        this.copy = copy;
        initService();
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

        service.setOnFailed(e -> doCancel(false));
        service.setOnSucceeded(e -> doSuccess());
    }

    /**
     * Retrieves the FileSystem to use for this FileService. Lazily initiliases this FileService's FileSystem instance
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
            if (copy) {
                operationSucceeded = fileSystem.copyFiles(source, destination);
            } else {
                operationSucceeded = fileSystem.moveFiles(source, destination);
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
        UI.removeBackgroundTask(this);
        scheduledTasks.remove(this);
        finished = true;
        String copyString = copy ? "copy":"move";

        if (operationSucceeded) {
            UI.doInfo("File Operation Successful", "The " + copyString + " of " + source.getFilePath() + " to " + destination.getFilePath() + " has completed successfully");
        } else {
            String error = operationException != null ? operationException.getMessage():null;
            UI.doError("File Operation Unsuccessful", "The " + copyString + " of " + source.getFilePath() + " to " + destination.getFilePath() + " has failed"
                + (error != null ? " because of the error: " + error:""));
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
        finished = false;
        service.start();
        UI.addBackgroundTask(this);
    }

    /**
     * Attempts to fully cancel this FileService by disconnection the connection in the file system if it is connected
     * @param wantedCancel true if this was an intended cancel and not a cancel because of an error
     */
    private void doCancel(boolean wantedCancel) {
        try {
            finished = true;
            if (!wantedCancel)
                UI.doError("Task Failure", "A file service task has failed due to an unknown error");
            FTPConnection connection = fileSystem.getFTPConnection();
            if (connection.isConnected())
                connection.disconnect();
        } catch (FTPException ex) {
            if (FTPSystem.isDebugEnabled())
                ex.printStackTrace();
            UI.doError("Cancellation Failed", "Failed to cancel this task because of the error: " + ex.getMessage());
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
     * Returns true if this task is running
     *
     * @return true if running, false if not
     */
    @Override
    public boolean isRunning() {
        return service.isRunning();
    }

    /**
     * Retrieves the state of the service backing this
     * @return state of backing service
     */
    public Worker.State getState() {
        return service.getState();
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
     * Returns a boolean determining if the task is finished.
     * This should be tracked by a variable in the implementing class and not by checking any underlying JavaFX Service state since that has to be done from the JavaFX thread
     *
     * @return true if finished, false if not
     */
    @Override
    public boolean isFinished() {
        return finished;
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
        for (FileService service : scheduledTasks) {
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
     */
    @Override
    public void schedule() {
        scheduler.schedule(getSchedulerKey(), this);
        scheduledTasks.add(this);
    }

    /**
     * Constructs a new instance of FileService based on the provided parameters.
     * @param source the source file being copied/moved by this FileService
     * @param destination the destination directory being copied/moved to
     * @param copy true to copy, false to move
     * @param local true if the destination FileSystem is local, false if remote
     * @return the instance of FileService
     */
    public static FileService newInstance(CommonFile source, CommonFile destination, boolean copy, boolean local) {
        if (local) {
            return new LocalFileService(source, destination, copy);
        } else {
            return new RemoteFileService(source, destination, copy);
        }
    }
}
