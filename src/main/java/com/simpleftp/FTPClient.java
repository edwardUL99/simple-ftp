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

package com.simpleftp;

import com.simpleftp.ui.UI;
import com.simpleftp.ui.exceptions.UIException;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * This will be the main class. At the moment it is just used for testing
 */
public class FTPClient extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * @param primaryStage the initial stage to display the application on
     * @throws UIException if the UI fails to be initialised
     */
    @Override
    public void start(Stage primaryStage) throws UIException {
        UI.startApplication(primaryStage);
    }
}
