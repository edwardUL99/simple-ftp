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

import com.simpleftp.filesystem.FileUtils;
import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.properties.Properties;
import com.simpleftp.sessions.Sessions;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.files.LineEntry;
import com.simpleftp.ui.panels.FilePanel;
import com.simpleftp.ui.panels.LocalFilePanel;
import com.simpleftp.ui.views.MainView;
import com.simpleftp.ui.views.PanelView;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.StringConverter;

import java.util.regex.Pattern;

/**
 * This class represents the toolbar on the top of the MainView
 */
public class TopToolbar extends MenuBar {
    /**
     * The MainView that this toolbar belongs to
     */
    private final MainView mainView;
    /**
     * The menu representing the file menu
     */
    private final Menu fileMenu;
    /**
     * The menu representing connection options
     */
    private final Menu connectionMenu;
    /**
     * The menu representing session options
     */
    private final Menu sessionMenu;
    /**
     * The menu representing local options
     */
    private final Menu localMenu;
    /**
     * The menu representing remote options
     */
    private final Menu remoteMenu;
    /**
     * The label used to disable/enable sessions
     */
    private final Label disableLabel;
    /**
     * This regex is used to check if a String could possibly represent a path
     */
    private static final String PATH_REGEX = "[A-Za-z0-9_*. s:]*[\\\\/]+[A-Za-z0-9_*. s]*";
    /**
     * A constant for the enable sessions menu option
     */
    private static final String ENABLE_SESSIONS = "_Enable Sessions";
    /**
     * A constant for the disable sessions menu option
     */
    private static final String DISABLE_SESSIONS = "_Disable Sessions";
    /**
     * Tooltip for enabling sessions
     */
    private static final Tooltip ENABLE_SESSIONS_TOOLTIP = new Tooltip("Enables and re-initialises Sessions functionality");
    /**
     * Tooltip for disabling sessions
     */
    private static final Tooltip DISABLE_SESSIONS_TOOLTIP = new Tooltip("Disables Sessions functionality");

    /**
     * An enum used for determining what type of file wants to be created
     */
    private enum CreateFileType {
        /**
         * The file to be created is a directory
         */
        DIRECTORY,
        /**
         * The file to be created is a file
         */
        FILE,
        /**
         * The file to be created is a symbolic link
         */
        SYMBOLIC_LINK
    }

    /**
     * Constructs a TopToolbar object for the provided MainView
     * @param mainView the MainView this Toolbar belongs to
     */
    public TopToolbar(MainView mainView) {
        this.mainView = mainView;
        fileMenu = new Menu();
        fileMenu.setMnemonicParsing(true);
        fileMenu.setText("_File");
        connectionMenu = new Menu();
        connectionMenu.setMnemonicParsing(true);
        connectionMenu.setText("_Connection");
        localMenu = new Menu();
        localMenu.setMnemonicParsing(true);
        localMenu.setText("_Local");
        remoteMenu = new Menu();
        remoteMenu.setMnemonicParsing(true);
        remoteMenu.setText("_Remote");
        sessionMenu = new Menu();
        sessionMenu.setMnemonicParsing(true);
        sessionMenu.setText("_Session");
        disableLabel = new Label();
        disableLabel.setMnemonicParsing(true);

        getMenus().addAll(fileMenu, connectionMenu, sessionMenu, localMenu, remoteMenu);
        init();
    }

    /**
     * Initialises this TopToolbar object
     */
    private void init() {
        initFileMenu();
        initConnectionMenu();
        initSessionMenu();
        initLocalMenu();
        initRemoteMenu();
    }

    /**
     * Initialises the File menu
     */
    private void initFileMenu() {
        Label quitLabel = new Label();
        quitLabel.setMnemonicParsing(true);
        quitLabel.setText("_Quit");
        quitLabel.setTooltip(new Tooltip("Quit the application (Q)"));
        MenuItem quit = new CustomMenuItem(quitLabel);
        quit.setOnAction(e -> mainView.quit());

        fileMenu.getItems().addAll(quit);
    }

    /**
     * Initialises the Connection menu
     */
    private void initConnectionMenu() {
        BooleanProperty remoteConnectedProperty = mainView.getPanelView().getRemoteConnectedProperty();

        Label connectLabel = new Label();
        connectLabel.setMnemonicParsing(true);
        connectLabel.setText("C_onnect");
        connectLabel.setTooltip(new Tooltip("Connect to the server without logging in with Sessions (Ctrl+Shift+C)"));
        MenuItem connect = new CustomMenuItem(connectLabel);
        connect.setOnAction(e -> mainView.connect());
        connect.disableProperty().bind(remoteConnectedProperty);

        Label disconnectLabel = new Label();
        disconnectLabel.setMnemonicParsing(true);
        disconnectLabel.setText("_Disconnect");
        disconnectLabel.setTooltip(new Tooltip("Disconnect from the server without logging out of the current session (Ctrl+D)"));
        MenuItem disconnect = new CustomMenuItem(disconnectLabel);
        disconnect.setOnAction(e -> mainView.disconnect());
        disconnect.disableProperty().bind(remoteConnectedProperty.not());

        connectionMenu.getItems().addAll(connect, disconnect);
    }

    /**
     * Initialises the Session menu
     */
    private void initSessionMenu() {
        BooleanBinding binding = mainView.getSessionsInitialisedProperty().not();
        Label saveLabel = new Label();
        saveLabel.setMnemonicParsing(true);
        saveLabel.setText("S_ave Session");
        saveLabel.setTooltip(new Tooltip("Save the current session to disk (Ctrl+Shift+S)"));
        MenuItem save = new CustomMenuItem(saveLabel);
        save.setOnAction(e -> mainView.saveCurrentSession());
        save.disableProperty().bind(binding);

        Label deleteLabel = new Label();
        deleteLabel.setMnemonicParsing(true);
        deleteLabel.setText("Dele_te Session");
        deleteLabel.setTooltip(new Tooltip("Delete the current session from the session file (Ctrl+Shift+D)"));
        MenuItem delete = new CustomMenuItem(deleteLabel);
        delete.setOnAction(e -> mainView.deleteCurrentSession());
        delete.disableProperty().bind(binding);

        disableLabel.setMnemonicParsing(true);
        bindDisableMenuLabel();
        disableLabel.tooltipProperty().bind(Bindings.when(mainView.getSessionsDisabledProperty())
                                                    .then(ENABLE_SESSIONS_TOOLTIP)
                                                    .otherwise(DISABLE_SESSIONS_TOOLTIP));
        MenuItem disable = new CustomMenuItem(disableLabel);
        disable.setOnAction(e -> {
            if (Sessions.isDisabled()) {
                mainView.enableSessions();
            } else {
                mainView.disableSessions();
            }
        });

        sessionMenu.getItems().addAll(save, delete, disable);
    }

    /**
     * Binds the disableLabel text property with the sessions disabled property
     */
    private void bindDisableMenuLabel() {
        disableLabel.textProperty().bindBidirectional(mainView.getSessionsDisabledProperty(), new StringConverter<>() {
            @Override
            public String toString(Boolean aBoolean) {
                return aBoolean ? ENABLE_SESSIONS:DISABLE_SESSIONS;
            }

            @Override
            public Boolean fromString(String s) {
                if (s.equals(ENABLE_SESSIONS))
                    return true;
                else if (s.equals(DISABLE_SESSIONS))
                    return false;
                else
                    throw new IllegalArgumentException("Invalid value given to Disable/Enable sessions label text property. Value given: " + s + ", one of {" + ENABLE_SESSIONS + "," + DISABLE_SESSIONS + "} expected");
            }
        });
    }

    /**
     * This populates the Create New menu for local and remote menus
     * @param local true if local, false if not
     * @param panelView the panel view containing the panels
     * @return the created menu
     */
    private Menu populateCreateMenu(boolean local, PanelView panelView) {
        Menu createMenu = new Menu();
        createMenu.setMnemonicParsing(true);
        createMenu.setText("Create _New");

        MenuItem directory = new MenuItem();
        directory.setMnemonicParsing(true);
        directory.setText("_Directory");
        directory.setOnAction(e -> createFile(local, panelView, CreateFileType.DIRECTORY));

        MenuItem file = new MenuItem();
        file.setMnemonicParsing(true);
        file.setText("F_ile");
        file.setOnAction(e -> createFile(local, panelView, CreateFileType.FILE));

        MenuItem symbolicLink = new MenuItem();
        symbolicLink.setMnemonicParsing(true);
        symbolicLink.setText("Symbolic _Link");
        symbolicLink.setOnAction(e -> createFile(local, panelView, CreateFileType.SYMBOLIC_LINK));

        createMenu.getItems().addAll(directory, file, symbolicLink);

        return createMenu;
    }

    /**
     * Creates the file type on the provided panel view
     * @param local true if a local file, false if remote
     * @param panelView the panel view containing the destination panels
     * @param fileType the file type to create
     */
    private void createFile(boolean local, PanelView panelView, CreateFileType fileType) {
        FilePanel filePanel = local ? panelView.getLocalPanel():panelView.getRemotePanel();

        switch (fileType) {
            case DIRECTORY: filePanel.createNewDirectory();
                            break;
            case FILE: filePanel.createNewFile();
                        break;
            case SYMBOLIC_LINK: filePanel.createSymbolicLink();
                                break;
        }
    }

    /**
     * Initialises the local or remote panel
     * @param local true if local, false if remote
     */
    private void initialisePanelMenus(boolean local) {
        PanelView panelView = mainView.getPanelView();
        Menu menu = local ? localMenu:remoteMenu;
        ObservableList<MenuItem> items = menu.getItems();

        MenuItem gotoFile = new MenuItem();
        gotoFile.setMnemonicParsing(true);
        gotoFile.setText("_Go To");
        if (local)
            gotoFile.setOnAction(e -> panelView.getLocalPanel().gotoPath());
        else
            gotoFile.setOnAction(e -> panelView.getRemotePanel().gotoPath());
        items.add(gotoFile);

        MenuItem root = new MenuItem();
        root.setMnemonicParsing(true);
        root.setText("R_oot");
        if (local)
            root.setOnAction(e -> panelView.getLocalPanel().goToRootDirectory());
        else
            root.setOnAction(e -> panelView.getRemotePanel().goToRootDirectory());
        items.add(root);

        if (local && ((LocalFilePanel)panelView.getLocalPanel()).getHomeDefinedProperty().get()) {
            MenuItem home = new MenuItem();
            home.setMnemonicParsing(true);
            home.setText("_Home");
            home.setOnAction(e -> ((LocalFilePanel)panelView.getLocalPanel()).home());
            items.add(home);
        }

        items.add(populateCreateMenu(local, panelView));

        MenuItem refresh = new MenuItem();
        refresh.setMnemonicParsing(true);
        refresh.setText("R_efresh");
        if (local)
            refresh.setOnAction(e -> panelView.getLocalPanel().getDirectoryPane().refresh());
        else
            refresh.setOnAction(e -> panelView.getRemotePanel().getDirectoryPane().refresh());
        items.add(refresh);

        Label copyPathLabel = new Label();
        copyPathLabel.setMnemonicParsing(true);
        copyPathLabel.setText("Cop_y Path");
        copyPathLabel.setTooltip(new Tooltip("Copy current directory/selected file path to clipboard"));
        MenuItem copyPath = new CustomMenuItem(copyPathLabel);
        copyPath.setOnAction(e -> copyPathMenuHandler(local, panelView));
        items.add(copyPath);

        Label pastePathLabel = new Label();
        pastePathLabel.setMnemonicParsing(true);
        pastePathLabel.setText("_Paste Path");
        pastePathLabel.setTooltip(new Tooltip("Paste absolute path from clipboard if available and navigate to it"));
        MenuItem pastePath = new CustomMenuItem(pastePathLabel);
        pastePath.setOnAction(e -> pastePathMenuHandler(local, panelView));
        items.add(pastePath);
    }

    /**
     * The handler for clipboard path copying
     * @param local true if you want to copy local directory, false if remote one
     * @param panelView the panelView containing the panels
     */
    private void copyPathMenuHandler(boolean local, PanelView panelView) {
        FilePanel panel = local ? panelView.getLocalPanel():panelView.getRemotePanel();
        LineEntry selectedEntry = panel.getSelectedEntry();
        String path = selectedEntry != null ? selectedEntry.getFilePath():panel.getDirectoryPane().getCurrentWorkingDirectory();

        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(path);
        boolean copied = clipboard.setContent(content);

        if (copied)
            UI.doInfo("Path Copied", "The " + (local ? "local ":"remote ") + "path " + path + " has been copied to the clipboard successfully");
        else
            UI.doError("Path Copy Failed", "The copy of " + (local ? "local ":"remote ") + "path " + path + " to the clipboard has not completed successfully");
    }

    /**
     * Validates that the provided path is a valid path for the respective panel
     * @param path the path to validate
     * @param local true if local, false if remote
     * @return true if valid, false if not
     */
    private boolean validatePath(String path, boolean local) {
        String pathSeparator = local ? FileUtils.PATH_SEPARATOR:"/";

        if (!FileUtils.isPathAbsolute(path, local))
            return false;

        return path.contains(pathSeparator); // check if it is a valid path for that panel as the separator should match
    }

    /**
     * The handler for the clipboard path pasting
     * @param local true if local, false if remote
     * @param panelView the panel view this operation is taking place on
     */
    private void pastePathMenuHandler(boolean local, PanelView panelView) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasString()) {
            String path = clipboard.getString();
            if (Pattern.compile(PATH_REGEX).matcher(path).lookingAt()
                    && !path.contains("\\n") && !path.contains("\\r") && !path.contains(" ")) {
                if (validatePath(path, local)) {
                    if (gotoClipboardFile(path, local, panelView) && Properties.CLEAR_CLIPBOARD_PATH_PASTE.getValue())
                        clipboard.clear();
                } else {
                    String header = "Invalid Path";
                    if (local && System.getProperty("os.name").contains("windows"))
                        UI.doError(header, "The provided path " + path + " is not valid for a local panel."
                        + " It needs to be absolute and start with the C:\\ drive and contain \\");
                    else if (local)
                        UI.doError(header, "The provided path " + path + " is not valid for a local panel."
                                + " It needs to be absolute and start with / (and uses / for path separator)");
                    else
                        UI.doError(header, "The provided path " + path + " is not valid for a remote panel."
                        + " The path needs to be absolute and contain the / path separator");
                }

                return;
            }
        }

        UI.doInfo("No Path On Clipboard", "There has been no path found on the Clipboard");
    }

    /**
     * This method handles going to the clipboard path
     * @param path the path to go to
     * @param local true if a local path, false if remote
     * @param panelView the panel view to interact with
     * @return true if successful, false if not
     */
    private boolean gotoClipboardFile(String path, boolean local, PanelView panelView) {
        try {
            FilePanel filePanel = local ? panelView.getLocalPanel() : panelView.getRemotePanel();
            CommonFile file = local ? new LocalFile(path) : new RemoteFile(path);

            boolean pathEquals = filePanel.getDirectoryPane().getCurrentWorkingDirectory().equals(path);
            LineEntry lineEntry = pathEquals ? null:LineEntry.newInstance(file, filePanel.getDirectoryPane()); // we need a LIneEntry to open
            if (lineEntry != null || pathEquals) {
                if (!pathEquals)
                    filePanel.getDirectoryPane().openLineEntry(lineEntry);
                UI.doInfo("Path Pasting Complete", "The path " + path + " from clipboard has been navigated to successfully");

                return true;
            } else {
                UI.doError("Path Pasting Failed", "The path " + path + " from clipboard has failed to be navigated to. It may not exist on the "
                        + (local ? "local" : "remote") + " panel.");
            }
        } catch (FileSystemException ex) {
            if (FTPSystem.isDebugEnabled())
                ex.printStackTrace();
            UI.doError("Path Pasting Failed", "The pasting of a clipboard path has failed with the following message: " + ex.getMessage());
        }

        return false;
    }

    /**
     * Initialises the local menu
     */
    private void initLocalMenu() {
        initialisePanelMenus(true);
    }

    /**
     * Initialises the remote menu
     */
    private void initRemoteMenu() {
        initialisePanelMenus(false);
        remoteMenu.disableProperty().bind(mainView.getPanelView().getRemoteConnectedProperty().not());
    }
}
