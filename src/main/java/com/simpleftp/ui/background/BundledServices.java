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

import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ui.UI;

import java.util.ArrayList;

/**
 * This class provides a means of bundling multiple FileServices (with the same destination) together so that you only
 * get 1 success message for the whole count of file services in the bundled operation rather than a success message each.
 *
 * 10 files may be involved in a multiple operation so it would be too much to have 10 success messages
 */
public final class BundledServices {
    /**
     * The bundled file services
     */
    private final ArrayList<FileService> services;
    /**
     * A variable to count the number of services completed successfully
     */
    private int completed;
    /**
     * A variable to count the number of services that failed
     */
    private int failed;
    /**
     * A variable to count the number of services that were cancelled
     */
    private int cancelled;
    /**
     * The total number of services bundled
     */
    private int total;
    /**
     * Flag to keep track of if the activate method has been called or not
     */
    private boolean activated;
    /**
     * The operation this bundled service is for
     */
    private final FileService.Operation operation;

    /**
     * This enum outlines the valid notification types to be passed to notify
     */
    enum NotificationType {
        /**
         * The service was completed successfully
         */
        COMPLETED,
        /**
         * The service failed
         */
        FAILED,
        /**
         * The service was cancelled
         */
        CANCELLED
    }

    /**
     * Constructs a BundledServices object
     * @param operation the operation that this class is bundling
     */
    public BundledServices(FileService.Operation operation) {
        services = new ArrayList<>();
        this.operation = operation;
    }

    /**
     * Bundles the provided file services into this bundle. This can be called numerous times provided that the file
     * services haven't already been bundled and activate hasn't been called
     * @param fileServices the file services to bundle
     */
    public void bundle(FileService...fileServices) {
        if (!activated) {
            CommonFile destination = null;

            for (FileService service : fileServices) {
                if (service.operation != operation) {
                    throw new BundleException("One of the provided service's operation (" + service.operation + ") does not match the bundle's operation (" + operation + ")");
                }

                if (destination == null) {
                    destination = service.getDestination();
                } else {
                    if (!service.getDestination().equals(destination)) {
                        // all the destinations should be the same
                        throw new BundleException("All the services in one bundle must have the same destination");
                    } else {
                        destination = service.getDestination();
                    }
                }

                if (!services.contains(service)) {
                    services.add(service);
                    service.bundle = this;
                } else {
                    throw new BundleException("A provided service has already been bundled");
                }
            }

            total += services.size();
        } else {
            throw new BundleException("You cannot call bundle after it has been activated");
        }
    }

    /**
     * Notify this bundled service that the service has completed with the provided notification type
     * @param notificationType the notification type
     * @param fileService the file service doing the notification
     */
    void notify(NotificationType notificationType, FileService fileService) {
        if (activated) {
            if (services.contains(fileService)) {
                if (notificationType == NotificationType.COMPLETED) {
                    completed++;
                } else if (notificationType == NotificationType.FAILED) {
                    failed++;
                } else if (notificationType == NotificationType.CANCELLED) {
                    cancelled++;
                }

                services.remove(fileService);
                checkCompleted();
            } else {
                throw new BundleException("The provided file service is not bundled");
            }
        } else {
            throw new BundleException("Cannot call notify if the bundle has not been activated");
        }
    }

    /**
     * Checks if every service in the bundle has been finished, i.e. all of them notified
     */
    void checkCompleted() {
        if (activated && services.size() == 0) {
            String operationString = "";

            if (operation == FileService.Operation.COPY) {
                operationString = "Copy";
            } else if (operation == FileService.Operation.MOVE) {
                operationString = "Move";
            } else if (operation == FileService.Operation.REMOVE) {
                operationString = "Deletion";
            }

            UI.doInfo(operationString + " finished", operationString + " finished with: "
                + completed + " successfully completed, " + failed + " failed, and " + cancelled + " cancelled");
        }
    }

    /**
     * Activates the bundle by scheduling all the services to run
     */
    public void activate() {
        activated = true;
        services.forEach(FileService::schedule);
    }

    /**
     * This exception is thrown if the BundledServices class is in an illegal state
     */
    private static class BundleException extends RuntimeException {
        /**
         * Construct a BundleException with the provided message
         * @param message the message for the exception
         */
        private BundleException(String message) {
            super(message);
        }
    }
}
