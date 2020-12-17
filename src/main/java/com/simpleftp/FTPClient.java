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

package com.simpleftp;

import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnectionDetails;
import com.simpleftp.ftp.connection.Server;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.security.PasswordEncryption;
import com.simpleftp.security.exceptions.PasswordEncryptionException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.exceptions.UIException;
import com.simpleftp.ui.views.PanelView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * This will be the main class. At the moment it is just used for testing
 */
public class FTPClient extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private void initialiseConnection() throws FTPException {
        try {
            String password = PasswordEncryption.encrypt(System.getenv("SIMPLEFTP_TEST_PASSWORD"));
            System.setProperty("ftp-server", System.getenv("SIMPLEFTP_TEST_SERVER"));
            System.setProperty("ftp-user",  System.getenv("SIMPLEFTP_TEST_USER"));
            System.setProperty("ftp-pass", password);
            String port = System.getenv("SIMPLEFTP_TEST_PORT");
            port = port == null ? "" + Server.DEFAULT_FTP_PORT :port;
            System.setProperty("ftp-port", port);

            FTPConnection connection = FTPConnection.createSharedConnection(FTPSystem.getPropertiesDefinedDetails(), new FTPConnectionDetails(100, 200));
            connection.connect();
            connection.login();
        } catch (PasswordEncryptionException ex) {
            UI.doException(ex, UI.ExceptionType.EXCEPTION, FTPSystem.isDebugEnabled(), false); // set false as this is a fatal exception and application cant run after it
        }
    }

    /**
     * @param primaryStage
     * @throws FileSystemException
     */
    @Override
    public void start(Stage primaryStage) throws FileSystemException, UIException {
        try {
            initialiseConnection();
        } catch (FTPException ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled(), false);
        }

        PanelView panelView = new PanelView(new LocalFile("/"));

        FTPConnection connection = FTPSystem.getConnection();

        if (connection.isConnected() && connection.isLoggedIn())
            panelView.createRemotePanel(new RemoteFile("/"));

        Scene scene = new Scene(panelView, UI.PANEL_VIEW_WIDTH, UI.PANEL_VIEW_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Test");
        primaryStage.setResizable(false);
        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> {
            UI.doQuit();
            e.consume(); // consume so you don't close if user doesn't want to quit
        });
    }
}
