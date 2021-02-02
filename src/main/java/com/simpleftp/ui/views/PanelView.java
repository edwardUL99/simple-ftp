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

package com.simpleftp.ui.views;

import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.properties.Properties;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.directories.DirectoryPane;
import com.simpleftp.ui.exceptions.UIException;
import com.simpleftp.ui.panels.FilePanel;
import com.simpleftp.ui.views.tasks.ConnectionMonitor;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import lombok.Getter;

/**
 * This class represents the main view of FilePanels side by side. It is the view that the user will see provided that they are connected to the remote server
 */
public final class PanelView extends VBox {
    /**
     * The local FilePanel to be displayed
     */
    @Getter
    private FilePanel localPanel;
    /**
     * The remote FilePanel to be displayed
     */
    @Getter
    private FilePanel remotePanel;
    /**
     * A property for determining if the remote panel is connected and initialised
     */
    @Getter
    private final BooleanProperty remoteConnectedProperty;
    /**
     * The boc containing the 2 panels
     */
    private final HBox panelsBox;
    /**
     * The monitor that will check the status of our connection
     */
    private final ConnectionMonitor connectionMonitor;
    /**
     * The MainView this PanelView belongs to
     */
    @Getter
    private final MainView mainView;

    /**
     * Constructs the view, initialising the LocalPanel.
     * The remote panel should be initialised with initialiseRemotePanel(RemoteFile file). This is because a RemotePanel can only be initialised after a connection is established
     * @param localDirectory the local directory to initialise the local panel with
     * @param mainView the MainView this PanelView belongs to
     * @throws UIException if an error occurs initialising the PanelView
     * @throws IllegalArgumentException if localDirectory is not a directory
     */
    public PanelView(LocalFile localDirectory, MainView mainView) throws UIException {
        this.mainView = mainView;
        panelsBox = new HBox();
        panelsBox.setPrefHeight(UI.FILE_PANEL_HEIGHT);
        panelsBox.setPrefWidth(UI.FILE_PANEL_WIDTH);
        setMaxHeight(UI.PANEL_VIEW_HEIGHT);
        setMaxWidth(UI.PANEL_VIEW_WIDTH);

        connectionMonitor = new ConnectionMonitor(this);

        remoteConnectedProperty = new SimpleBooleanProperty(false);

        initialiseLocalPanel(localDirectory);
        createEmptyRemotePanel();

        getChildren().addAll(panelsBox);
    }

    /**
     * Sets the height and width of the given FilePanel
     * @param panel the panel to set the height and width of
     */
    private void setPanelHeightAndWidth(FilePanel panel) {
        panel.setPrefWidth(UI.FILE_PANEL_WIDTH);
        panel.setMinWidth(UI.FILE_PANEL_WIDTH);
        panel.setMaxWidth(UI.FILE_PANEL_WIDTH);
        panel.setPrefHeight(UI.FILE_PANEL_HEIGHT);
        panel.setMinHeight(UI.FILE_PANEL_HEIGHT);
        panel.setMaxHeight(UI.FILE_PANEL_HEIGHT);
    }

    /**
     * Initialises the local file panel
     * @param localDirectory the local directory to initialise the panel with
     * @throws UIException if an error occurs initialising the local panel
     */
    private void initialiseLocalPanel(LocalFile localDirectory) throws UIException {
        try {
            if (localPanel == null) {
                localPanel = FilePanel.newInstance(DirectoryPane.newInstance(localDirectory));
                setPanelHeightAndWidth(localPanel);
                localPanel.setPanelView(this);

                VBox localPanelBox = new VBox();
                localPanelBox.setAlignment(Pos.CENTER);
                localPanelBox.setStyle(UI.WHITE_BACKGROUND);
                Label local = new Label("Local");
                local.setFont(Font.font(15));
                local.setStyle(UI.SMOKE_WHITE_BACKGROUND);
                localPanelBox.getChildren().addAll(local, localPanel);

                panelsBox.getChildren().add(localPanelBox);
            } else {
                DirectoryPane directoryPane = localPanel.getDirectoryPane();
                directoryPane.setDirectory(localDirectory);
                directoryPane.refresh();
            }
        } catch (FileSystemException ex) {
            throw new UIException("An error occurred initialising the Local Panel of the PanelView", ex);
        }
    }

    /**
     * Creates the panel representing a connection not established
     */
    private void createEmptyRemotePanel() {
        int height = Properties.FILE_EDITOR_HEIGHT.getValue();
        VBox emptyPane = new VBox();
        emptyPane.setPrefHeight(height);
        emptyPane.setPrefWidth(UI.FILE_PANEL_WIDTH);

        HBox labelBox = new HBox();
        labelBox.setPadding(new Insets(0, 0, 5, 0));
        labelBox.setAlignment(Pos.CENTER);
        labelBox.setStyle(UI.WHITE_BACKGROUND);
        labelBox.getChildren().add(getRemotePanelLabel());

        StackPane connectionPane = new StackPane();
        connectionPane.setPrefHeight(height - 5);
        connectionPane.setAlignment(Pos.CENTER);
        Label connectionLabel = new Label("Remote connection not established");
        connectionLabel.setFont(Font.font(20));
        connectionPane.getChildren().add(connectionLabel);

        emptyPane.getChildren().addAll(labelBox, connectionPane);

        if (panelsBox.getChildren().size() > 1)
            panelsBox.getChildren().remove(1);

        panelsBox.getChildren().add(emptyPane);
        remotePanel = null;
        remoteConnectedProperty.setValue(false);
    }

    /**
     * Disconnects the remote panel from the FTP Server and replaces it with the empty panel and the local panel.
     * This can also be used in the event a connection is lost and to clear the remote panel. If called due to a logout,
     * stopConnectionMonitor should be called first and this should be called before disconnecting.
     *
     * This closes all open remote FileEditorWindows
     *
     * You can still access the remote panel with getRemotePanel to get access to the directory it was at etc. Note that the connection may no longer be created so calls to other methods
     * of the panel may fail. getDirectoryPane().getDirectory() should always succeed however
     */
    public void disconnectRemotePanel() {
        if (isRemoteConnected()) {
            DirectoryPane directoryPane = remotePanel.getDirectoryPane();
            directoryPane.removeInstance(); // we are no longer using this directory pane instance. Remove it so mouse events etc. are not propagated to it
            createEmptyRemotePanel();
            UI.closeAllFiles(false);
        }
    }

    /**
     * Stops the monitor monitoring the connection if it is scheduled or running
     */
    public void stopConnectionMonitor() {
        if (connectionMonitor.getState() == Worker.State.SCHEDULED || connectionMonitor.isRunning())
            connectionMonitor.cancel();
    }

    /**
     * Creates and returns the RemotePanelBox label
     * @return the label for the remote pane;
     */
    private Label getRemotePanelLabel() {
        Label remote = new Label("Remote");
        remote.setFont(Font.font(15));
        remote.setStyle(UI.SMOKE_WHITE_BACKGROUND);

        return remote;
    }

    /**
     * Used to initialise the remote panel after object creation or to resetConnection the existing one. This is required as a connection may not be established on constructing this PanelView.
     * Upon login and successful initialisation of a connection, you will want to call this method.
     * This resets the remote file panel used on each call, so if you want to get the file the panel is on before resetting it with this method, call getRemotePanel().getDirectoryPane().getDirectory().
     *
     * Even if the connection is lost and clearRemotePanel is called, you can still access the directory the panel was in before another call to createRemotePanel. This could be used to re-initialise with the same directory it was on before
     * the connection was lost. You may also just create a copy of the file by calling new RemoteFile(getRemotePanel().getDirectoryPane().getCurrentWorkingDirectory()) and let the file use the appropriate re-connected connection to re-initialise.
     *
     * @param remoteDirectory the remote directory to initialise the remotePanel with.
     */
    public void createRemotePanel(RemoteFile remoteDirectory) throws UIException {
        try {
            if (remotePanel != null)
                remotePanel.setPanelView(null); // remove the association of the PanelView

            remotePanel = FilePanel.newInstance(DirectoryPane.newInstance(remoteDirectory));
            setPanelHeightAndWidth(remotePanel);
            remotePanel.setPanelView(this);

            VBox remotePanelBox = new VBox();
            remotePanelBox.setAlignment(Pos.CENTER);
            remotePanelBox.setStyle(UI.WHITE_BACKGROUND);
            remotePanelBox.getChildren().add(getRemotePanelLabel());

            panelsBox.getChildren().remove(1);
            remotePanelBox.getChildren().add(remotePanel);
            panelsBox.getChildren().add(remotePanelBox);

            connectionMonitor.start();
            remoteConnectedProperty.setValue(true);
        } catch (FileSystemException ex) {
            remotePanel = null;
            remoteConnectedProperty.setValue(false);
            throw new UIException("A FileSystemException was thrown initialising the Remote Panel of the PanelView", ex);
        }
    }

    /**
     * Determines if the remote panel is initialised (i.e. a connection was established) or not
     * @return true if initialised, false if not
     */
    public boolean isRemoteConnected() {
        return remoteConnectedProperty.get();
    }
}
