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

package com.simpleftp.ui.login;

import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.connection.Server;
import com.simpleftp.properties.Properties;
import com.simpleftp.sessions.Session;
import com.simpleftp.sessions.Sessions;
import com.simpleftp.sessions.exceptions.SessionSaveException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.interfaces.Window;
import com.simpleftp.ui.views.MainView;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import lombok.Getter;

/**
 * This class provides a LoginWindow for providing functionality to login to the FTP server graphically.
 */
public class LoginWindow extends VBox implements Window {
    /**
     * The MainView this LoginWindow operates on
     */
    @Getter
    private final MainView mainView;
    /**
     * The stage that displays this window
     */
    private final Stage stage;
    /**
     * A HBox providing the contents of the LoginWindow
     */
    private final VBox contentBox;
    /**
     * The TextField for entering server
     */
    private final TextField serverField;
    /**
     * The TextField for entering server port
     */
    private final TextField portField;
    /**
     * The TextField for entering timeout
     */
    private final TextField timeoutField;
    /**
     * The TextField for entering username
     */
    private final TextField userField;
    /**
     * The field for entering the password
     */
    private final PasswordField passwordField;
    /**
     * A text field that matches whats entered into the PasswordField but its visible
     */
    private final TextField unmaskedPasswordField;
    /**
     * A check box to display the password
     */
    private final CheckBox displayPassword;
    /**
     * Checkbox for enabling/disabling sessions
     */
    private final CheckBox sessionsDisabled;
    /**
     * The button for matching sessions
     */
    private final Button matchSession;
    /**
     * An instance variable to keep track of a matched session
     */
    private Session matchedSession;
    /**
     * A property to determine if the fields required for matching sessions is filled
     */
    private final BooleanProperty matchFieldsFilledProperty;
    /**
     * A property to determine if login is possible
     */
    private final BooleanProperty allFieldsFilledProperty;
    /**
     * Regex for validating Server hostname
     */
    private static final String SERVER_REGEX = "^(?!-)[A-Za-z0-9.\\-]{0,255}";
    /**
     * Regex for validation of port and timeout value
     */
    private static final String NUMBER_REGEX = "[0-9]*";
    /**
     * The default port for port field
     */
    private static final String DEFAULT_PORT = "" + Server.DEFAULT_FTP_PORT;
    /**
     * The default timeout for timeout field
     */
    private static final String DEFAULT_TIMEOUT = "" + 200;

    /**
     * Constructs a login window linking it to the provided MainView
     * @param mainView the MainView instance this LoginWindow will operate on
     */
    public LoginWindow(MainView mainView) {
        this.mainView = mainView;

        stage = new Stage() {
            /**
             * Overridden show and wait so that we can request focus on the login window
             */
            @Override
            public void showAndWait() {
                LoginWindow.this.requestFocus();
                super.showAndWait();
            }
        };

        matchFieldsFilledProperty = new SimpleBooleanProperty(false);
        allFieldsFilledProperty = new SimpleBooleanProperty(false);

        contentBox = new VBox();
        serverField = new TextField();
        portField = new TextField(DEFAULT_PORT);
        timeoutField = new TextField(DEFAULT_TIMEOUT);
        userField = new TextField();
        passwordField = new PasswordField();
        unmaskedPasswordField = new TextField();
        displayPassword = new CheckBox();
        sessionsDisabled = new CheckBox();
        matchSession = new Button("Match Session");

        init();
    }

    /**
     * Initialises this LoginWindow
     */
    private void init() {
        setOnMouseClicked(e -> requestFocus());

        initPadding();
        initStage();
        initHeaderAndStyle();
        initContentBox();
        initServerField();
        initPortAndTimeoutFields();
        initUserAndPassFields();
        initSessionsEnabled();
        initButtons();
        initPropertyBindings();
    }

    /**
     * Initialises the padding of the LoginWindow
     */
    private void initPadding() {
        double topBottomPadding = UI.LOGIN_WINDOW_HEIGHT / 74.0;
        double leftRightPadding = UI.LOGIN_WINDOW_WIDTH / 20.0;

        contentBox.setPadding(new Insets(topBottomPadding, leftRightPadding, topBottomPadding, leftRightPadding));
        setSpacing(10);
    }

    /**
     * This method initialises the stage
     */
    private void initStage() {
        Scene scene = new Scene(this, UI.LOGIN_WINDOW_WIDTH, UI.LOGIN_WINDOW_HEIGHT);
        stage.setScene(scene);
        stage.setTitle("SimpleFTP Login Window");
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> cancel());
        stage.setOnShowing(e -> checkMatchButtonProperty());
    }

    /**
     * Initialise header and styles
     */
    private void initHeaderAndStyle() {
        Label header = new Label("SimpleFTP Login");
        header.setFont(Font.font("Monospaced", 15));
        Label subtitle = new Label("Login to the remote server and session if enabled");
        subtitle.setFont(Font.font("Monospaced", 12));

        VBox headerBox = new VBox();
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setStyle(UI.GREY_BACKGROUND_TRANSPARENT);
        headerBox.setSpacing(15);

        headerBox.getChildren().addAll(header, subtitle);
        getChildren().addAll(headerBox, new Separator(Orientation.HORIZONTAL));
        setStyle(UI.WHITE_BACKGROUND);
    }

    /**
     * This method initialises the HBox storing contents
     */
    private void initContentBox() {
        contentBox.setSpacing(10);
        getChildren().add(contentBox);
    }

    /**
     * Creates a label and wraps it with a HBox to keep left alignment
     *
     * @param text the text of the label
     * @return the HBox containing the label
     */
    private HBox createLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Monospaced"));

        HBox labelBox = new HBox();
        labelBox.getChildren().add(label);
        labelBox.setStyle(UI.WHITE_BACKGROUND);

        return labelBox;
    }

    /**
     * Creates a VBox/HBox for holding items like text fields etc
     * @param vBox true to create a VBox, false to create a HBox
     * @return the VBox/HBox with styles set
     */
    private Node createHoldingBox(boolean vBox) {
        Node node;

        if (vBox) {
            VBox box = new VBox();
            box.setAlignment(Pos.CENTER);
            node = box;
        } else {
            HBox box = new HBox();
            box.setAlignment(Pos.CENTER);
            node = box;
        }

        node.setStyle(UI.WHITE_BACKGROUND);

        return node;
    }

    /**
     * Initialises the server, port and timeout fields
     */
    private void initServerField() {
        serverField.setPromptText("Server Hostname");
        serverField.setTooltip(new Tooltip("Server hostname must be between 1 and 255 alphanumeric characters, not start with - . Periods (.) are allowed"));
        serverField.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getControlNewText();
            return (text.matches(SERVER_REGEX) ? change : null);
        }));

        VBox serverBox = (VBox)createHoldingBox(true);
        serverBox.setSpacing(5);
        serverBox.setAlignment(Pos.CENTER);
        serverBox.getChildren().addAll(createLabel("Server:"), serverField);
        contentBox.getChildren().add(serverBox);
    }

    /**
     * Initialises the port and timeout fields
     */
    private void initPortAndTimeoutFields() {
        portField.setPromptText("Server Port");
        portField.setTextFormatter(new TextFormatter<>(change -> (change.getControlNewText().matches(NUMBER_REGEX) ? change : null)));
        timeoutField.setPromptText("Server Timeout");
        timeoutField.setTooltip(new Tooltip("Enter the server timeout time in seconds"));
        timeoutField.setTextFormatter(new TextFormatter<>(change -> (change.getControlNewText().matches(NUMBER_REGEX) ? change : null)));

        GridPane gridPane = new GridPane();
        gridPane.setHgap(20);
        gridPane.setVgap(5);

        gridPane.add(createLabel("Port:"), 0, 0);
        gridPane.add(portField, 0, 1);
        gridPane.add(createLabel("Timeout:"), 1, 0);
        gridPane.add(timeoutField, 1, 1);

        contentBox.getChildren().add(gridPane);
    }

    /**
     * Bind the passwordField and unmaskedPasswordField properties
     */
    private void bindPassFields() {
        BooleanProperty selectedProperty = displayPassword.selectedProperty();
        passwordField.visibleProperty().bind(selectedProperty.not());
        passwordField.managedProperty().bind(passwordField.visibleProperty());
        unmaskedPasswordField.visibleProperty().bind(selectedProperty);
        unmaskedPasswordField.managedProperty().bind(unmaskedPasswordField.visibleProperty());
    }

    /**
     * Creates the GridPane containing password fields and display password field checkkbox
     * @return the GridPane containing password fields
     */
    private GridPane getPassFieldsPane() {
        GridPane passFieldPane = new GridPane();
        passFieldPane.setVgap(5);
        passFieldPane.setHgap(5);

        passFieldPane.add(createLabel("Password:"), 0, 0);
        passFieldPane.add(passwordField, 0, 1);
        passFieldPane.add(unmaskedPasswordField, 0, 1);
        passFieldPane.add(displayPassword, 1, 1);

        return passFieldPane;
    }

    /**
     * Initialises user and password fields
     */
    private void initUserAndPassFields() {
        String passwordPrompt = "Server Password";
        int prefWidth = 215;
        userField.setPromptText("Server Username");
        passwordField.setPromptText(passwordPrompt);
        passwordField.setOnAction(e -> passwordFieldEnter());
        passwordField.setPrefWidth(prefWidth);
        unmaskedPasswordField.setPromptText(passwordPrompt);
        unmaskedPasswordField.setPrefWidth(prefWidth);
        unmaskedPasswordField.setOnAction(e -> passwordFieldEnter());
        unmaskedPasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        displayPassword.setText("Display Password");
        bindPassFields();

        VBox userPassBox = (VBox)createHoldingBox(true);
        userPassBox.setSpacing(5);
        userPassBox.getChildren().addAll(createLabel("Username:"), userField, getPassFieldsPane());

        contentBox.getChildren().add(userPassBox);
    }

    /**
     * This method handles when enter is pressed on the password field.
     * It logs in if all required fields are not empty
     */
    private void passwordFieldEnter() {
        if (allFieldsFilledProperty.getValue())
            login();
    }

    /**
     * This method enables/disables sessions based on the initial value of the sessionsDisabled checkbox
     */
    private void enableDisableSessions() {
        if (!sessionsDisabled.isSelected())
            mainView.enableSessions();
        else
            mainView.disableSessions();
    }

    /**
     * Initialises sessions enabled checkbox
     */
    private void initSessionsEnabled() {
        sessionsDisabled.setText("Disable Sessions");
        sessionsDisabled.setTooltip(new Tooltip("Check this to login with just server connection and no Sessions functionality"));
        sessionsDisabled.selectedProperty().bindBidirectional(mainView.getSessionsDisabledProperty());
        enableDisableSessions();

        sessionsDisabled.setOnAction(e -> {
            if (sessionsDisabled.isSelected()) {
                mainView.disableSessions();
                setSessionFieldsEditable(true);
            } else {
                mainView.enableSessions();
            }
        });

        VBox sessionsBox = (VBox)createHoldingBox(true);
        sessionsBox.getChildren().add(sessionsDisabled);

        contentBox.getChildren().add(sessionsBox);
    }

    /**
     * Initialises the property bindings
     */
    private void initPropertyBindings() {
        matchFieldsFilledProperty.bind(getFieldsBinding(true));
        allFieldsFilledProperty.bind(getFieldsBinding(false));
    }

    /**
     * Gets the binding for the fields required for either login or matching sessions
     * @param match true if the fields are for the match session button, false if login
     * @return the boolean binding. The binding is for if the fields are filled
     */
    private BooleanBinding getFieldsBinding(boolean match) {
        if (match)
            return Bindings.and(
                    sessionsDisabled.selectedProperty().not(),
                    Bindings.and(userField.textProperty().isEmpty().not(),
                            Bindings.and(serverField.textProperty().isEmpty().not(), portField.textProperty().isEmpty().not())));
        else
            return Bindings.and(
                    Bindings.and(serverField.textProperty().isEmpty().not(),
                            Bindings.and(portField.textProperty().isEmpty().not(), timeoutField.textProperty().isEmpty().not())),
                    Bindings.and(userField.textProperty().isEmpty().not(), passwordField.textProperty().isEmpty().not()));
    }

    /**
     * Initialises Cancel, Match Session and Login buttons
     */
    private void initButtons() {
        Button reset = new Button("Reset");
        reset.setTooltip(new Tooltip("Reset all fields to their original values"));
        reset.setOnAction(e -> reset());

        Button cancel = new Button("Cancel");
        cancel.setTooltip(new Tooltip("Close login window and don't login in"));
        cancel.setOnAction(e -> {
            cancel();
            close();
        });

        matchSession.visibleProperty().bind(sessionsDisabled.selectedProperty().not());
        matchSession.managedProperty().bind(matchSession.visibleProperty());
        matchSession.setTooltip(new Tooltip("Match the provided details with a Session and auto-fill password"));
        matchSession.disableProperty().bind(matchFieldsFilledProperty.not());
        matchSession.setOnAction(e -> matchSession());

        Button login = new Button("Login");
        login.setTooltip(new Tooltip("Login to the provided server and session if enabled.\n" +
                "Server, Port, User and Password need to be filled in"));
        login.disableProperty().bind(allFieldsFilledProperty.not());
        login.setOnAction(e -> login());

        HBox buttonsBox = (HBox)createHoldingBox(false);
        buttonsBox.setSpacing(5);
        buttonsBox.getChildren().addAll(reset, cancel, matchSession, login);

        contentBox.getChildren().add(buttonsBox);
    }

    /**
     * A method to check the MATCH_SESSION_LOGIN_BUTTON for enabled and if enabled, binds it to the sessions disabled property, if not, unbinds it and hides it.
     * This method is to be called on the stage OnShowing event so if the property value changes, the match session button will update on the next showing
     */
    private void checkMatchButtonProperty() {
        boolean enabled = Properties.MATCH_SESSION_LOGIN_BUTTON.getValue().equals("ENABLED");
        BooleanProperty visibleProperty = matchSession.visibleProperty();

        if (!enabled) {
            if (visibleProperty.isBound())
                visibleProperty.unbind();

            matchSession.setVisible(false);
        } else {
            if (!visibleProperty.isBound())
                visibleProperty.bind(sessionsDisabled.selectedProperty().not());
        }
    }

    /**
     * Makes all fields that identify sessions editable/not editable.
     * This should be called by matchSession so between match and login, the fields to identify a session won't be edited
     */
    private void setSessionFieldsEditable(boolean editable) {
        TextField[] fields = {serverField, portField, userField};

        for (TextField field : fields)
            field.setEditable(editable);
    }

    /**
     * Returns the server represented by the entered details
     * @return the Server object represented by the entered details
     */
    private Server getEnteredServer() {
        return new Server(
                serverField.getText(),
                userField.getText(),
                passwordField.getText(),
                Integer.parseInt(portField.getText()),
                Integer.parseInt(timeoutField.getText())
        );
    }

    /**
     * Matches the session with the identification fields entered. Therefore, this method should call setSessionFieldEditable with false
     * so that these identification fields (like server port and user) aren't changed between a matchSession and login. This
     * is because login syncs up the server object of the matched session by calling matchedSession.setServerDetails so
     * the identification fields need to be the same.
     */
    private void matchSession() {
        Server server = getEnteredServer();
        Session session = Sessions.matchSession(server);

        if (session != null) {
            Server sessionServer = session.getServerDetails();
            timeoutField.setText("" + sessionServer.getTimeout());
            passwordField.setText(sessionServer.getPassword());

            matchedSession = session;
            setSessionFieldsEditable(false);
            UI.doInfo("Session Found", "A session has matched the provided details successfully. " +
                    "The password field has been filled with the password that was saved on file");
        } else {
            UI.doInfo("No Session Found", "There is no existing session matching the entered details. " +
                    "Logging in with sessions enabled will create a new session for these details");
        }
    }

    /**
     * Saves the provided session on login
     * @param session the session to save
     */
    private void saveSession(Session session) {
        try {
            session.save();
        } catch (SessionSaveException ex) {
            if (FTPSystem.isDebugEnabled())
                ex.printStackTrace();

            UI.doError("Session Save Failure", "Failed to save the session on login with the message: " + ex.getMessage() + ". Try Session > Save Session " +
                    "on the top toolbar to try and save it again");
        }
    }

    /**
     * Handles the login functionality.
     * If this is called after matchSession, the setSessionFieldsEditable should be called with a value of false, so that
     * the identification fields aren't changed between match session and login (as this method calls matchedSession.setServerDetails
     * to update non-identifying fields (like timeout and password). It's easier to do this just by setting the Server object of the session with the
     * entered server object retrieved from getEnteredServer().
     */
    private void login() {
        Server server = getEnteredServer();

        boolean sessionsEnabled = !sessionsDisabled.isSelected();

        Session session = null;
        if (sessionsEnabled)
            session = matchedSession != null ? matchedSession:Sessions.getSession(server);

        FTPConnection connection = FTPSystem.getConnection();
        if (connection == null)
            FTPConnection.createSharedConnection(server);
        else
            connection.setServer(server);

        if (mainView.connectToServer()) {
            if (sessionsEnabled) {
                if (matchedSession != null)
                    matchedSession.setServerDetails(server); // any fields used to identify the server shouldn't have changed since matchSession should have prevented editing the identifying fields.
                                                            // Call this method to update non-identifying fields such as timeout and password
                saveSession(session);
                Sessions.setCurrentSession(session);
            }

            mainView.onLogin();
            close();
        } else {
            UI.doConnectionError();
            passwordField.clear();
            FTPSystem.resetConnection();
        }

        matchedSession = null;
    }

    /**
     * Cancels this login
     */
    private void cancel() {
        reset();
        if (!mainView.getPanelView().isRemoteConnected())
            FTPSystem.resetConnection();
    }

    /**
     * Resets this login window
     */
    public void reset() {
        serverField.clear();
        portField.setText(DEFAULT_PORT);
        timeoutField.setText(DEFAULT_TIMEOUT);
        userField.clear();
        passwordField.clear();
        matchedSession = null;
        setSessionFieldsEditable(true);
    }

    /**
     * Retrieves the children of this node.
     * Made final so we can call it from constructor warning about calling overridable method
     * @return the children of this window
     */
    @Override
    public final ObservableList<Node> getChildren() {
        return super.getChildren();
    }

    /**
     * Defines the basic operation of showing a window.
     * This usually entails defining a scene to place a pane in and then using a Stage to show it.
     */
    @Override
    public void show() {
        if (!isShowing())
            stage.showAndWait();
    }

    /**
     * Defines the basic operation of closing a window.
     * This usually entails doing some clean up and then calling the stage that was opened to close
     */
    @Override
    public void close() {
        if (isShowing()) {
            stage.close();
            passwordField.clear();
            setSessionFieldsEditable(true);
        }
    }

    /**
     * Determines if the LoginWindow is showing
     * @return true if showing, false if not
     */
    public boolean isShowing() {
        return stage.isShowing();
    }

    /**
     * Brings the login window to front
     */
    public void toFront() {
        stage.toFront();
    }
}
