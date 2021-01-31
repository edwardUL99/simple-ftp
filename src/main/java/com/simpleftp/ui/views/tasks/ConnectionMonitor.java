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

package com.simpleftp.ui.views.tasks;

import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.properties.Properties;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.views.PanelView;
import javafx.concurrent.Service;
import javafx.concurrent.Task;


/**
 * This class checks the status of the FTPSystem connection and if it is lost, it disconnects the remote panel on the provided panel view.
 * This should be cancelled on a normal disconnect/logout as that is an expected disconnection. To restart, reset and then start should be called.
 */
public class ConnectionMonitor extends Service<Void> {
    /**
     * The panel view to control
     */
    private final PanelView panelView;
    /**
     * This flag determines if the monitor has been cancelled
     */
    private boolean cancelled;

    /**
     * Constructs a connection monitor with the provided parameters.
     * @param panelView the panel view to monitor the connection for
     */
    public ConnectionMonitor(PanelView panelView) {
        this.panelView = panelView;
        setOnSucceeded(e -> doSucceed());
    }

    /**
     * Performs the onSucceeded handler
     */
    private void doSucceed() {
        if (!cancelled) {
            UI.doError("Connection Lost", "The connection to the Server has been lost");
            panelView.disconnectRemotePanel();
            panelView.getMainView().disconnect();
        }
    }

    /**
     * Creates the task that this service will run
     * @return the task that wil do the connection monitoring
     */
    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                boolean monitor = true;

                while (monitor && !cancelled) {
                    FTPConnection connection = FTPSystem.getConnection();
                    try {
                        monitor = connection.isConnected() && connection.isLoggedIn(); // when we break out of this loop, we lost connection and will call the succeeded code
                        Thread.sleep(Properties.CONNECTION_MONITOR_INTERVAL.getValue());
                        connection.sendNoop();
                    } catch (FTPException ex) {
                        monitor = false;
                    }
                }

                return null;
            }
        };
    }

    /**
     * Starts this service, or restarts it if it was succeeded.
     */
    @Override
    public void start() {
        cancelled = false;
        State state = getState();

        if (state == State.SUCCEEDED || state == State.CANCELLED || state == State.FAILED) {
            restart();
        } else if (state == State.READY) {
            super.start();
        }
    }

    @Override
    public boolean cancel() {
        cancelled = true;
        return super.cancel();
    }
}
