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

import com.simpleftp.filesystem.FileUtils;
import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPConnectionFailedException;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.sessions.Session;
import com.simpleftp.sessions.Sessions;
import com.simpleftp.sessions.exceptions.SessionSaveException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.background.elements.TaskWindow;
import com.simpleftp.ui.background.interfaces.BackgroundTask;
import com.simpleftp.ui.dialogs.BackgroundTaskRunningDialog;
import com.simpleftp.ui.directories.DirectoryPane;
import com.simpleftp.ui.exceptions.UIException;
import com.simpleftp.ui.login.LoginWindow;
import com.simpleftp.ui.views.toolbars.BottomToolbar;
import com.simpleftp.ui.views.toolbars.MiddleToolbar;
import com.simpleftp.ui.views.toolbars.TopToolbar;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Orientation;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * This class provides the main application view
 */
@Log4j2
public class MainView extends VBox {
    /**
     * The top toolbar for this MainView
     */
    private final TopToolbar topToolbar;
    /**
     * The MiddleToolbar object
     */
    private final MiddleToolbar middleToolbar;
    /**
     * The PanelView displaying the FilePanels
     */
    @Getter
    private final PanelView panelView;
    /**
     * The LoginWindow linked to this MainVie
     */
    @Getter
    private LoginWindow loginWindow;
    /**
     * The bottom toolbar for this MainView
     */
    private final BottomToolbar bottomToolbar;
    /**
     * A property to determine if you are logged in or not
     */
    @Getter
    private final BooleanProperty loggedInProperty;
    /**
     * A property to track the initialisation of sessions. In the context of Sessions initialised means that it has loaded in the
     * SessionFile correctly, but in UI context, it is initialised if Sessions.isInitialised() and Sessions.getCurrentSession() != null
     */
    private final BooleanProperty sessionsInitialisedProperty;
    /**
     * A property to track if sessions are disabled or not
     * The value of this should only be explicitly set by the enableSessions() or disableSessions() methods.
     * Note that the value may change through bi-directional bindings but other than that the explicit setValue should only be in those methods
     */
    @Getter
    private final BooleanProperty sessionsDisabledProperty;
    /**
     * The task window that the UI can display
     */
    @Getter
    private final TaskWindow taskWindow;
    /**
     * This is the singleton instance for MainView
     */
    private static MainView SINGLETON_INSTANCE;
    /**
     * The boolean flag for deferring session initialisation
     */
    private static boolean deferredSessionInitialisation = false;

    /**
     * Constructs a MainView object, initialising the PanelView with user.home if found or root until login changes it if sessions are enabled
     * @throws UIException if initialisation of the UI fails
     */
    private MainView() throws UIException {
        panelView = new PanelView(getHomeDirectory(), this);
        taskWindow = new TaskWindow();

        loggedInProperty = new SimpleBooleanProperty(false);
        sessionsInitialisedProperty = new SimpleBooleanProperty(false);
        sessionsDisabledProperty = new SimpleBooleanProperty(false);

        topToolbar = new TopToolbar(this);
        middleToolbar = new MiddleToolbar(this);
        bottomToolbar = new BottomToolbar(this);

        init();
    }

    /**
     * Initialises this MainView
     */
    private void init() {
        setMaxHeight(UI.MAIN_VIEW_HEIGHT);
        setMinHeight(UI.MAIN_VIEW_HEIGHT);
        setMaxWidth(UI.MAIN_VIEW_WIDTH);
        setMinWidth(UI.MAIN_VIEW_WIDTH);

        getChildren().addAll(topToolbar, middleToolbar, new Separator(Orientation.HORIZONTAL), panelView, new Separator(Orientation.HORIZONTAL), bottomToolbar);
        initKeyBindings();
    }

    /**
     * Sets up KeyBindings
     */
    private void initKeyBindings() {
        setOnKeyPressed(e -> {
            if (!e.isAltDown()) { // we don't want to interfere with mnemonics
                KeyCode code = e.getCode();
                boolean ctrlDown = e.isControlDown();
                boolean special = ctrlDown && e.isShiftDown(); // some of these key commands conflict with common commands like Ctrl+C for copy and Ctrl+S for save so use Shift to distinguish them

                if (code == KeyCode.Q) {
                    quit();
                } else if (ctrlDown && code == KeyCode.L) {
                    middleToolbar.handleLoginPress();
                } else if (code == KeyCode.D) {
                    if (ctrlDown && !special) {
                        if (panelView.isRemoteConnected())
                            disconnect();
                    } else if (special) {
                        deleteCurrentSession();
                    }
                } else if (special && code == KeyCode.C) {
                    if (!panelView.isRemoteConnected())
                        connect();
                } else if (special && code == KeyCode.S) {
                    if (isSessionsInitialised())
                        saveCurrentSession();
                }
            }
        });
    }

    /**
     * Retrieve the singleton instance with a LoginWindow initialised and tied to it
     * @return the singleton MainView instance
     * @throws UIException if an exception occurs initialising the instance
     */
    public static MainView getInstance() throws UIException {
        if (SINGLETON_INSTANCE == null) {
            SINGLETON_INSTANCE = new MainView();
            SINGLETON_INSTANCE.loginWindow = new LoginWindow(SINGLETON_INSTANCE);
        }

        return SINGLETON_INSTANCE;
    }

    /**
     * Call this method to defer session initialisation until after a primaryStage is shown.
     * This defers session initialisation in this class' enableSessions() method. Calling that method disables
     * deferral so any other calls after will enable sessions instantly.
     *
     * This is useful as if the primaryStage has not been shown yet and there are any errors related to session initialisation,
     * these errors will show but no other windows and this looks strange.
     *
     * If deferred, the call to initialisation is done with Platform.runLater so that it is run when the JavaFX platform is
     * fully set up.
     */
    public static void deferSessionInitialisation() {
        deferredSessionInitialisation = true;
    }

    /**
     * Deletes the current session if confirmed
     */
    public void deleteCurrentSession() {
        if (Sessions.isInitialised()) {
            Session current = Sessions.getCurrentSession();

            if (current != null && UI.doConfirmation("Delete Current Session", "Are you sure you want to delete the current session? This cannot be undone.")) {
                try {
                    Sessions.deleteSession(current);
                    logoutSession();
                    UI.doInfo("Session Deleted", "The current session has been deleted successfully");
                } catch (Exception ex) {
                    if (FTPSystem.isDebugEnabled())
                        ex.printStackTrace();

                    UI.doError("Session Deletion Error", "An error occurred deleting the current session with the message: " + ex.getMessage());
                }
            }
        }
    }

    /**
     * Saves the last session details if the current Session is not null. Sessions should be initialised before calling this
     * @return true if successful, false if not
     */
    private boolean saveLastSessionDetails() {
        Session session = Sessions.getCurrentSession();
        if (session != null) {
            String localDirectory = panelView.getLocalPanel()
                    .getDirectoryPane()
                    .getCurrentWorkingDirectory();

            String remoteDirectory = panelView.isRemoteConnected() ? panelView.getRemotePanel()
                    .getDirectoryPane()
                    .getCurrentWorkingDirectory():session.getLastSession().getLastRemoteWD();

            Session.LastSession lastSession = new Session.LastSession(remoteDirectory, localDirectory);
            session.setLastSession(lastSession);

            try {
                session.save();

                return true;
            } catch (SessionSaveException ex) {
                if (FTPSystem.isDebugEnabled())
                    ex.printStackTrace();

                UI.doError("Session Save Error", "An error occurred saving the current session with the message: " + ex.getMessage());
            }
        }

        return false;
    }

    /**
     * Saves the current session if found and sessions are initialised
     */
    public void saveCurrentSession() {
        if (isSessionsInitialised() && saveLastSessionDetails())
            UI.doInfo("Session Saved", "The current session has been saved successfully");
    }

    /**
     * Disconnects the system connection if connected
     */
    private void disconnectConnection() {
        panelView.stopConnectionMonitor();
        panelView.disconnectRemotePanel();

        try {
            FTPConnection connection = FTPSystem.getConnection();

            if (connection != null && connection.isConnected()) {
                connection.disconnect();
            }
        } catch (FTPException ex) {
            if (FTPSystem.isDebugEnabled())
                ex.printStackTrace();

            UI.doError("Connection Error", "Failed to disconnect connection on exit with the message: " + ex.getMessage());
        }

        bottomToolbar.onDisconnected();
    }

    /**
     * This method manages quitting the application and saving of the final session
     */
    public void quit() {
        UI.doQuit(this::finishSession);
    }

    /**
     * Perform the final actions of this session by saving last session if initialised and clearing the remote panel
     */
    private void finishSession() {
        if (isSessionsInitialised())
            saveLastSessionDetails();

        disconnectConnection();
    }

    /**
     * Gets the directory representing user.home, root otherwise
     * @return the home directory
     */
    private LocalFile getHomeDirectory() {
        String userHome = System.getProperty("user.home");
        String rootPath = FileUtils.getRootPath(true);
        userHome = userHome == null ? rootPath:userHome;
        LocalFile file = new LocalFile(userHome);

        return !file.isADirectory() ? new LocalFile(rootPath):file;
    }

    /**
     * Gets the last local directory if still exists, home otherwise
     * @param session the session to retrieve from
     * @return the LocalFile representing the directory to set the local panel to, null if cant be initialised
     */
    private LocalFile getLocalLastDirectory(Session session) {
        Session.LastSession lastSession = session.getLastSession();
        LocalFile localFile = new LocalFile(lastSession.getLastLocalWD());

        return localFile.isADirectory() ? localFile:null;
    }

    /**
     * Gets the last remote directory if still exists, home otherwise
     * @param session the session to retrieve from
     * @return the RemoteFile representing the directory to set the remote panel to, null if cant be initialised
     * @throws FileSystemException if an error occurs
     */
    private RemoteFile getRemoteLastDirectory(Session session) throws FileSystemException {
        Session.LastSession lastSession = session.getLastSession();
        RemoteFile remoteFile = new RemoteFile(lastSession.getLastRemoteWD());

        return remoteFile.isADirectory() ? remoteFile:null;
    }

    /**
     * This method checks if the system connection is not null
     * An IllegalStateException is thrown otherwise
     */
    private void checkSystemConnection() {
        FTPConnection connection = FTPSystem.getConnection();

        if (connection == null)
            throw new IllegalStateException("Cannot login to a MainView when the FTPSystem connection is null");

        if (!connection.isLoggedIn())
            throw new IllegalStateException("onLogin should only be called when the FTPSystem connection has been connected and logged in");
    }

    /**
     * If sessions are enabled (initialised) and current session is not null, the Session's Server is checked to match the system connection.
     * If it doesn't match, an IllegalStateException is thrown
     */
    private void checkSessionDetails() {
        if (isSessionsInitialised()) {
            Session currentSession = Sessions.getCurrentSession();

            if (currentSession != null && !currentSession.getServerDetails().equals(FTPSystem.getConnection().getServer()))
                throw new IllegalStateException("The current Session's Server must match the System's connection Server details");
        }
    }

    /**
     * Logs in the local panel
     */
    private void loginLocalPanel() {
        LocalFile localFile = getHomeDirectory();

        if (Sessions.isInitialised()) {
            Session session = Sessions.getCurrentSession();

            if (session != null) {
                LocalFile localFile1 = getLocalLastDirectory(session);
                localFile = localFile1 == null ? localFile:localFile1;
            }
        }

        try {
            DirectoryPane localPane = panelView.getLocalPanel().getDirectoryPane();
            if (!FileUtils.pathEquals(localPane.getCurrentWorkingDirectory(), localFile.getFilePath(), true)) {
                localPane.setDirectory(localFile);
                localPane.refresh();
            }
        } catch (FileSystemException ex) {
            if (FTPSystem.isDebugEnabled())
                ex.printStackTrace();

            UI.doError("Login Error", "Failed to initialise remote file panel");
        }
    }

    /**
     * Logs in the remote panel
     */
    private void loginRemotePanel() {
        try {
            RemoteFile remoteFile = new RemoteFile("/");

            if (Sessions.isInitialised()) {
                Session session = Sessions.getCurrentSession();

                if (session != null) {
                    RemoteFile remoteFile1 = getRemoteLastDirectory(session);
                    remoteFile = remoteFile1 == null ? remoteFile:remoteFile1;
                }
            }

            if (!panelView.isRemoteConnected()) {
                try {
                    panelView.createRemotePanel(remoteFile);
                    bottomToolbar.onConnected();
                } catch (UIException ex) {
                    UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
                }
            } else {
                DirectoryPane remotePane = panelView.getRemotePanel().getDirectoryPane();
                if (!FileUtils.pathEquals(remotePane.getCurrentWorkingDirectory(), remoteFile.getFilePath(), false)) {
                    remotePane.setDirectory(remoteFile);
                    remotePane.refresh();
                }
                bottomToolbar.onConnected();
            }
        } catch (FileSystemException ex) {
            if (FTPSystem.isDebugEnabled())
                ex.printStackTrace();

            UI.doError("Login Error", "Failed to initialise remote file panel");
        }
    }

    /**
     * This method connects the system connection by logging in
     */
    public boolean connectToServer() {
        try {
            FTPConnection connection = FTPSystem.getConnection();
            boolean loggedIn = connection.isLoggedIn();
            if (!loggedIn) {
                if (!connection.isConnected())
                    connection.connect();

                loggedIn = connection.login();
            }

            return loggedIn;
        } catch (FTPException ex) {
            if (ex instanceof FTPConnectionFailedException)
                log.error("Connection to server has failed");
            else
                UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
        }

        return false;
    }

    /**
     * Logs in the connection with login window if system connection is null
     * @return connection that has been logged in
     */
    private FTPConnection loginConnection() {
        FTPConnection connection = FTPSystem.getConnection();
        if (connection == null) {
            if (loginWindow.isShowing())
                loginWindow.close();
            disableSessions();
            UI.doLogin();
            connection = FTPSystem.getConnection();
        }

        return connection;
    }

    /**
     * This method provides connection functionality for the TopToolbar Connect option in Connection menu
     */
    public void connect() {
        FTPConnection connection = loginConnection();

        if (connection != null) {
            if (connectToServer()) {
                loginRemotePanel();

                if (!Sessions.isInitialised())
                    loggedInProperty.setValue(connection.isLoggedIn()); // if sessions aren't enabled, connect is the same as login, so set the onLogin property
            } else {
                UI.doConnectionError();
            }
        }
    }

    /**
     * Disconnects the remote panel and the connection without logging out
     */
    public void disconnect() {
        if (checkBackgroundTasks()) {
            disconnectConnection();
            if (!isSessionsInitialised()) { // if sessions aren't initialised logging out is the same as disconnecting, so logout also
                loggedInProperty.setValue(false);
                FTPSystem.resetConnection(); // reset connection so that we force a login window to be displayed on subsequent press of connect
            }
        }
    }

    /**
     * This method provides the logic to run on successful login when correct details are entered in the login page.
     * The Login window should call this method, rather than calling directly from a button handler etc.
     *
     * This method does not connect to the server. Connection (and login) should be achieved by the LoginPage
     *
     * If the system connection is null, an IllegalStateException is thrown.
     * If sessions are enabled and the current session is not null, but it's Server doesn't match the system connection, an
     * IllegalStateException is also thrown.
     *
     * If the system connection is not logged in, an IllegalStateException is thrown
     */
    public void onLogin() {
        checkSystemConnection();
        checkSessionDetails();
        sessionsDisabledProperty.setValue(Sessions.isDisabled());
        sessionsInitialisedProperty.setValue(Sessions.isInitialised() && Sessions.getCurrentSession() != null);

        loginRemotePanel();
        loginLocalPanel();

        if (isSessionsInitialised())
            loggedInProperty.setValue(true);
        else
            loggedInProperty.setValue(panelView.isRemoteConnected());
    }

    /**
     * Checks if theres any background tasks running and confirms that logging out will cancel them and if confirmed, cancels
     * them
     * @return true if confirmed, false if not
     */
    private boolean checkBackgroundTasks() {
        if (UI.backgroundTaskInProgress()) {
            if (new BackgroundTaskRunningDialog().showAndGetConfirmation()) {
                UI.getBackgroundTasks().forEach(BackgroundTask::cancel);
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * Logouts from the remote server and the current session
     */
    public void logout() {
        if (checkBackgroundTasks()) {
            finishSession();
            logoutSession();
            loggedInProperty.setValue(false);
            FTPSystem.resetConnection(); // reset connection so that we force a login window to be displayed on subsequent press of connect
        }
    }

    /**
     * Logs out the current session only rather than disconnecting the connection. But if the remote panel is not connected,
     * the loggedInProperty is set to false
     */
    public void logoutSession() {
        if (isSessionsInitialised())
            Sessions.setCurrentSession(null); // we are no longer using the session when we logout. The login window should set the session if sessions are being used
        sessionsInitialisedProperty.setValue(false);
        loggedInProperty.setValue(panelView.isRemoteConnected());
    }

    /**
     * This synchronizes the 2 panels (i.e. refreshes both of them)
     */
    public void synchronize() {
        panelView.getLocalPanel()
                .getDirectoryPane().refresh();

        if (panelView.isRemoteConnected())
            panelView.getRemotePanel()
                .getDirectoryPane().refresh();
    }

    /**
     * Returns true if the remote panel is logged in
     * @return true if logged in, false if not
     */
    public boolean isLoggedIn() {
        return loggedInProperty.get();
    }

    /**
     * Initialises sessions for the entire application
     */
    private void initialiseSessions() {
        if (UI.initialiseSessions()) {
            sessionsDisabledProperty.setValue(false);
            if (Sessions.isInitialised())
                loggedInProperty.setValue(Sessions.getCurrentSession() != null);
            else
                loggedInProperty.setValue(panelView.isRemoteConnected());
            synchronize(); // synchronize after enabling sessions in the case that the enabling may affect files displayed
        } else {
            disableSessions();
        }
    }

    /**
     * Enables sessions functionality
     */
    public void enableSessions() {
        Sessions.enable();
        if (deferredSessionInitialisation) {
            Platform.runLater(this::initialiseSessions);
            deferredSessionInitialisation = false;
        } else {
            initialiseSessions();
        }
    }

    /**
     * Disables Sessions in the MainView
     */
    public void disableSessions() {
        if (isSessionsInitialised()) {
            saveLastSessionDetails();
            logoutSession();
        }

        Sessions.disable();
        sessionsDisabledProperty.setValue(true);
        loggedInProperty.setValue(panelView.isRemoteConnected());
        synchronize(); // synchronize after disabling sessions in the case that the disabling may affect files displayed
    }

    /**
     * Checks the value of the sessionsInitialisedProperty
     * @return true if initialised and current session is not null, false if not
     */
    public boolean isSessionsInitialised() {
        return sessionsInitialisedProperty.get();
    }

    /**
     * The sessionsInitialisedProperty in the MainView context is if the Sessions.isInitialised() method returns true and
     * Sessions.getCurrentSession() is not null
     * @return the property representing UI initialised sessions
     */
    public ReadOnlyBooleanProperty getSessionsInitialisedProperty() {
        return sessionsInitialisedProperty;
    }
}
