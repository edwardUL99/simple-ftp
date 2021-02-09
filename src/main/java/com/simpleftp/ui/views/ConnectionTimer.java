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

package com.simpleftp.ui.views;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class provides a timer to indicate how long the connection has been active for
 */
public class ConnectionTimer {
    /**
     * The time property
     */
    private final StringProperty timeProperty;
    /**
     * The timer backing this connection timer
     */
    private Timer timer;
    /**
     * True if cancelled, false if not
     */
    private boolean cancelled;
    /**
     * True if started, false if not
     */
    private boolean started;
    /**
     * The long variable representing the time
     */
    private long time;

    /**
     * Constructs a connection timer object
     */
    public ConnectionTimer() {
        timeProperty = new SimpleStringProperty("0:00:00");
        timer = new Timer(true);
    }

    /**
     * This method initialises the TimerTask
     * @return timer task to use with the timer
     */
    private TimerTask getTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                if (!cancelled)
                    updateTimeProperty();
            }
        };
    }

    /**
     * The StringProperty you can bind label text to representing the time
     * @return the time StringProperty
     */
    public StringProperty getTimeProperty() {
        return timeProperty;
    }

    /**
     * Starts the timer. Restarts it if it was cancelled
     */
    public void start() {
        if (!started) {
            if (cancelled)
                reset();

            started = true;
            timer.scheduleAtFixedRate(getTimerTask(), 10, 10);
        }
    }

    /**
     * Cancel this timer
     */
    public void cancel() {
        started = false;
        cancelled = true;
        timer.cancel();
        timer = new Timer(true);
    }

    /**
     * Reset this timer
     */
    public void reset() {
        started = cancelled = false;
        Platform.runLater(() -> timeProperty.setValue("0:00:00")); // use platform run later so that it is set to 0 after all other updates to the property are completed
        time = 0;
    }

    /**
     * This method updates the time property
     */
    private void updateTimeProperty() {
        if (!cancelled) {
            time += 10;
            String[] timeSplit = new SimpleDateFormat("HH:mm:ss").format(new Date(time)).split(":");
            int hour = Integer.parseInt(timeSplit[0]) - 1;
            Platform.runLater(() -> timeProperty.setValue(hour + ":" + timeSplit[1] + ":" + (timeSplit[2].length() == 1 ? "0" + timeSplit[2] : timeSplit[2].substring(0, 2))));
        } else {
            Platform.runLater(() -> timeProperty.setValue("0:00:00"));
        }
    }
}
