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

package com.simpleftp.ui.background.scheduling;

import com.simpleftp.ui.background.BackgroundTask;
import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class is used to schedule a task using T as the object to schedule by. This can be used if a task should be run one after another based on a certain
 * key/parameter being the same for the subsequent task. If this key is different for a different task of the same type, allow it run in parallel.
 *
 * Basically, it maintains a map of key -> Queue of tasks which all share that key. So when you schedule a service, you add it to the queue that is mapped by that key.
 * Tasks are started for each key on a FIFO basis.
 *
 * On each round of checking for tasks to start, each key is iterated through. If there is not already a task running for that key (identified by a mapping of key to running task. A
 * check is done to see if the task is still running by checking if isFinished returns true and removed then), the next task in the queue is removed and started. If the queue is empty, the key and queue are removed from the mappings.
 *
 * If there are no key -> queue mappings in this class, the Service checking for tasks to start is cancelled and will only be restarted on the next schedule call.
 *
 * An example is when we are copying/moving files. When we have a FileService task in progress for copying some source file, let's call it <i>a</i>, it could happen that a move
 * FileService is started for the same file <i>a</i>. What happens if the move thread moves a file just before it is copied by the other FileService? Most likely,
 * an error would be thrown saying the file no longer exists. The solution to this would be to make the move FileService wait until the copy FileService is done.
 * How can we make it wait without having to check if the copy thread is finished? Simple, we can schedule it so that it will run sometime after the copy thread is finished by calling
 * taskScheduler.schedule(a, fileService). This will add fileService to a's queues of FileServices to be started next. Now we have the issue of wanting to start another service that is not the
 * same file. Does that have to wait until a FileService belonging to a has completed? No, because that would have been added to the other file's queue, so would run regardless of the in progress
 * task of a.
 *
 * @param <T> the "key" to synchronize by, i.e. if you want to register a task to a key (could be if the task is for the same file for e.g. use a CommonFile as a key and then if 2 tasks with the same file object are registered, run one after another),
 *           basically this key is what determines if a task is a duplicate of another and they should be run one after another based on this key.
 *           It is recommended that T overrides both equals() and hashCode() - following their contract if possible
 * @param <R> the type of the task
 */
public class TaskScheduler<T, R extends BackgroundTask> {
    /**
     * The hashmap of queues of tasks per type T
     */
    private final HashMap<T, Queue<R>> taskQueues;
    /**
     * A flag to keep track of if the scheduler has been started or not
     */
    private boolean started;
    /**
     * A flag to keep track of if the scheduler has been cancelled or not
     */
    private boolean cancelled;
    /**
     * The scheduler service backing this class
     */
    private ScheduledService<Void> scheduler;
    /**
     * A hash map to keep track of a running task for the specified key
     */
    private final HashMap<T, R> runningTask = new HashMap<>();

    /**
     * Constructs a TaskScheduler
     */
    public TaskScheduler() {
        taskQueues = new HashMap<>();
        initScheduler();
    }

    /**
     * Initialises the scheduler
     */
    private void initScheduler() {
        scheduler = new ScheduledService<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        if (isCancelled())
                            throw new InterruptedException();
                        startNextTask();
                        return null;
                    }
                };
            }
        };

        scheduler.setOnCancelled(e -> {
            started = false;
            cancelled = true;
        });

        scheduler.setOnFailed(e -> started = cancelled = false);
    }

    /**
     * Checks the status of the current running task for the provided key, if any. If it isn't running, it is removed from the hashmap
     * @param key the key to check
     * @return true if no task has finished for the given key and therefore not removed from the running task map for that key
     */
    private boolean isTaskInProgress(T key) {
        R task = runningTask.get(key);
        if (task != null) {
            if (task.isFinished()) {
                runningTask.remove(key);
            } else {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks all running tasks for if they are still running
     */
    private void checkAllRunningTasks() {
        ArrayList<T> tasksToRemove = new ArrayList<>();
        for (Map.Entry<T, R> entry : runningTask.entrySet()) {
            if (entry.getValue().isFinished())
                tasksToRemove.add(entry.getKey());
        }

        tasksToRemove.forEach(runningTask::remove);
    }

    /**
     * Starts the next tasks available to run in the file's queues if not empty. If the source file's queue is empty, it is removed from the map
     */
    private void startNextTask() {
        if (taskQueues.isEmpty()) {
            checkAllRunningTasks();
            if (runningTask.isEmpty()) {
                Platform.runLater(() -> {
                    scheduler.cancel();
                    started = false;
                    cancelled = true;
                });
            }
        } else {
            ArrayList<T> entriesToRemove = new ArrayList<>();

            for (Map.Entry<T, Queue<R>> entry : taskQueues.entrySet()) {
                Queue<R> tasks = entry.getValue();
                T key = entry.getKey();
                boolean taskInProgress = isTaskInProgress(key);

                if (tasks.isEmpty()) {
                    entriesToRemove.add(key);
                } else {
                    if (!taskInProgress) { // if a task is currently in progress for the provided key, don't start a next task for that key
                        R task = tasks.poll();
                        if (task != null && task.isReady()) {
                            runningTask.put(key, task);
                            Platform.runLater(task::start); // tasks should only be started from the JavaFX thread
                        }
                    }
                }
            }

            entriesToRemove.forEach(taskQueues::remove);
        }
    }

    /**
     * This method checks the status of the service and starts it, restarting if it was cancelled before
     */
    private void checkServiceStatus() {
        if (!started && !cancelled) {
            scheduler.start();
            started = true;
        } else if (cancelled) {
            scheduler.reset();
            scheduler.start();
            started = true;
            cancelled = false;
        }
    }

    /**
     * Schedules the task against the provided key.
     * This method adds the task to the queue identified by the key if found, adds a new queue for that key if not and then adds the task
     * to it.
     * @param key the key to register this task to
     * @param task the task to register to the key
     */
    public void schedule(T key, R task) {
        Queue<R> taskQueue = taskQueues.get(key);
        if (taskQueue == null) {
            taskQueue = new ConcurrentLinkedQueue<>();
            taskQueues.put(key, taskQueue);
        }

        taskQueue.add(task);

        checkServiceStatus(); // check the service status after adding the task queue or else the task queue may still be empty when startNextTask is called and the service may just get cancelled
    }
}
