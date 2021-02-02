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

package com.simpleftp.ui.views.toolbars;

import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.Server;
import com.simpleftp.ui.views.ConnectionTimer;
import com.simpleftp.ui.views.MainView;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;

/**
 * This class represents the bottom toolbar of the MainView
 */
public class BottomToolbar extends HBox {
    /**
     * The MainView that this toolbar belongs to
     */
    private final MainView mainView;
    /**
     * The ConnectionTimer for the Bottom toolbar
     */
    private final ConnectionTimer connectionTimer;
    /**
     * Separator used for separating serverInfo
     */
    private final Separator serverInfoSeparator;
    /**
     * A HBox displaying serverInfo
     */
    private final HBox serverInfo;
    /**
     * Label used for displaying the server
     */
    private final Label server;
    /**
     * Label used for displaying the user
     */
    private final Label user;
    /**
     * Label used for displaying the port
     */
    private final Label port;
    /**
     * A constant pre-fix for displaying the server
     */
    private static final String SERVER_PREFIX = "Server: ";
    /**
     * A constant pre-fix for displaying the user
     */
    private static final String USER_PREFIX = "User: ";
    /**
     * A constant pre-fix for displaying the port;
     */
    private static final String PORT_PREFIX = "Port: ";

    /**
     * Constructs a BottomToolbar object
     * @param mainView the MainView this toolbar belongs to
     */
    public BottomToolbar(MainView mainView) {
        this.mainView = mainView;
        connectionTimer = new ConnectionTimer();
        serverInfo = new HBox();
        serverInfo.setSpacing(5);
        serverInfoSeparator = new Separator(Orientation.VERTICAL);
        server = new Label();
        user = new Label();
        port = new Label();
        init();
    }

    /**
     * Initialises this toolbar
     */
    private void init() {
        setSpacing(5);
        setPadding(new Insets(5));
        setAlignment(Pos.CENTER_RIGHT);

        Font font = Font.font("Monospaced");
        server.setFont(font);
        user.setFont(font);
        port.setFont(font);

        BooleanProperty visibleProperty = serverInfo.visibleProperty();
        serverInfo.managedProperty().bind(visibleProperty);
        serverInfoSeparator.managedProperty().bind(visibleProperty);
        serverInfo.setVisible(false);
        serverInfoSeparator.setVisible(false);
        serverInfo.getChildren().addAll(server, user, port);

        Label timerLabel = new Label();
        timerLabel.textProperty().bind(connectionTimer.getTimeProperty());

        getChildren().addAll(serverInfoSeparator, serverInfo, new Separator(Orientation.VERTICAL), timerLabel);
    }

    /**
     * This should be called on the connection of the remote panel so that connection info on the BottomToolbar is
     * updated
     */
    public void onConnected() {
        startConnectionTimer();
        addConnectionInfo();
    }

    /**
     * This method should be called on the disconnection of the remote panel so that connection info in BottomToolbar can be
     * cleared/resetConnection
     */
    public void onDisconnected() {
        stopConnectionTimer();
        removeConnectionInfo();
    }

    /**
     * Adds connection info to the bottom toolbar
     */
    private void addConnectionInfo() {
        Server server = FTPSystem.getConnection().getServer();
        this.server.setText(SERVER_PREFIX + server.getServer());
        user.setText(USER_PREFIX + server.getUser());
        port.setText(PORT_PREFIX + server.getPort());
        serverInfoSeparator.setVisible(true);
        serverInfo.setVisible(true);
    }

    /**
     * Removes connection info from the bottom toolbar
     */
    private void removeConnectionInfo() {
        server.setText("");
        user.setText("");
        port.setText("");
        serverInfoSeparator.setVisible(false);
        serverInfo.setVisible(false);
    }

    /**
     * Start the connection timer in this toolbar
     */
    private void startConnectionTimer() {
        connectionTimer.start();
    }

    /**
     * Stops and resets the connection timer
     */
    private void stopConnectionTimer() {
        connectionTimer.cancel();
        connectionTimer.reset();
    }
}
