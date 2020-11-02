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

import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.ftp.connection.FTPConnectionDetails;
import com.simpleftp.ftp.connection.FTPServer;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.connection.FTPConnectionManager;
import com.simpleftp.ftp.connection.FTPConnectionManagerBuilder;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.security.PasswordEncryption;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.containers.FilePanelContainer;
import com.simpleftp.ui.panels.FilePanel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class FTPClient extends Application {
    private FTPConnectionManager connectionManager;

    public static void main(String[] args) {
        launch(args);
    }

    private void initialiseConnection() throws FTPException {
        String password = PasswordEncryption.encrypt("ruggerBugger2015");
        System.setProperty("ftp-server", "pi");
        System.setProperty("ftp-user", "pi");
        System.setProperty("ftp-pass", password);
        System.setProperty("ftp-port", "" + FTPServer.DEFAULT_PORT);
        FTPConnectionManagerBuilder builder = new FTPConnectionManagerBuilder();
        connectionManager = builder.setBuiltManagerAsSystemManager(true).build();
        FTPConnection connection = connectionManager.createIdleConnection("pi", "pi", PasswordEncryption.decrypt(password), FTPServer.DEFAULT_PORT);
        connection.setFtpConnectionDetails(new FTPConnectionDetails(100, 300));
        connection.setTimeoutTime(300);
        connection.connect();
        connection.login();
        FTPSystem.setConnection(connection);

        Runtime.getRuntime().addShutdownHook(new ShutDownHandler());
    }

    static class ShutDownHandler extends Thread {
        @Override
        public void run() {
            if (FTPSystem.getConnection() != null) {
                try {
                    FTPSystem.getConnection().disconnect();
                } catch (FTPException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public void start(Stage primaryStage) throws FileSystemException, FTPException {
        initialiseConnection();
        FTPSystem.getConnectionManager().addConnection(FTPSystem.getConnection()); // add so all remote files use the same

        FilePanel panel = new FilePanel(new RemoteFile("/test"));
        //FilePanel panel = new FilePanel(new LocalFile("/home/eddy"));
        FilePanelContainer panelContainer = new FilePanelContainer(panel);

        /*FileLineEntry lineEntry = new FileLineEntry(new LocalFile("/home/eddy/Coding/C++/StudSysCppCLI/ubuntu.login"));
        FileLineEntry lineEntry1 = new FileLineEntry(new LocalFile("/home/eddy/Coding/C++/StudSysCppCLI/sources/ModuleHomePage.cpp"));

        panel.addLineEntry(lineEntry);
        panel.addLineEntry(lineEntry1);

        try {
            /*FileLineEntry lineEntry2 = new FileLineEntry(new RemoteFile("test/Listener.java"));
            numClicked.put(lineEntry2, 1);
            lineEntry2.setOnMouseClicked(e -> {
                try {
                    click(lineEntry2);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            });
            lineEntry2.setActionMenu(e -> {
                if (deleteFile(lineEntry2.getFile())) {
                    vBox.getChildren().remove(lineEntry2);
                }
            });
            vBox.getChildren().add(lineEntry2);*/
            /*FTPConnection connection = FTPSystem.getConnection();
            if (connection != null) {
                FTPFile[] files = connection.listFiles("test");

                for (FTPFile f : files) {
                    String name = f.getName();
                    if (!f.getName().contains("/")) {
                        name = "test/" + name;
                    }
                    FileLineEntry fileLineEntry = new FileLineEntry(new RemoteFile(name));
                    panel.addLineEntry(fileLineEntry);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        Scene scene = new Scene(panelContainer, UI.FILE_PANEL_WIDTH, UI.FILE_PANEL_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Test");
        primaryStage.setResizable(false);
        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
    }
}
