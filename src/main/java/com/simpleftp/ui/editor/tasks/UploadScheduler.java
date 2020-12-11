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

package com.simpleftp.ui.editor.tasks;

import com.simpleftp.ui.editor.FileEditorWindow;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class schedules the upload of files so that one only runs after one is successful.
 * This class has been implemented for issue #91 to synchronize the moving of files to back them up so that only one thread at a time does that
 */
public class UploadScheduler {
    /**
     * The map of file upload queues for each window
     */
    private static final HashMap<FileEditorWindow, Queue<FileUploader>> uploaderQueues = new HashMap<>();

    /**
     * This class is supposed to be used only statically without instances.
     * Prevent instantiation
     */
    private UploadScheduler() {};

    static {
        initScheduler();
    }

    /**
     * Initialises the scheduler
     */
    private static void initScheduler() {
        /**
         * The scheduling service backing this UploadScheduler
         */
        ScheduledService<Void> scheduler = new ScheduledService<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() {
                        startNextUploader();
                        return null;
                    }
                };
            }
        };

        scheduler.start();
    }

    /**
     * Starts the next uploaders in the window's queues, if not empty. If a window's queues are empty, the entry is removed from the map
     */
    private static void startNextUploader() {
        ArrayList<FileEditorWindow> entriesToRemove = new ArrayList<>();

        for (Map.Entry<FileEditorWindow, Queue<FileUploader>> entry : uploaderQueues.entrySet()) {
            Queue<FileUploader> fileUploaders = entry.getValue();

            if (fileUploaders.isEmpty()) {
                entriesToRemove.add(entry.getKey());
            } else {
                if (!FileUploader.isUploadInProgress(entry.getKey())) {
                    FileUploader uploader = fileUploaders.poll();
                    if (uploader != null && uploader.getState() == Worker.State.READY)
                        uploader.start();
                }
            }
        }

        entriesToRemove.forEach(uploaderQueues::remove);
    }

    /**
     * Schedules the following file uploader to be ran
     * @param editorWindow the editor window to register the uploader to
     * @param fileUploader the uploader to schedule
     */
    public static void scheduleSave(FileEditorWindow editorWindow, FileUploader fileUploader) {
        Queue<FileUploader> uploadQueue = uploaderQueues.get(editorWindow);
        if (uploadQueue == null) {
            uploadQueue = new ConcurrentLinkedQueue<>();
            uploaderQueues.put(editorWindow, uploadQueue);
        }

        uploadQueue.add(fileUploader);

    }
}
