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
import com.simpleftp.ftp.connection.FTPConnection;
import com.simpleftp.ftp.exceptions.FTPException;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.login.LoginWindow;
import com.simpleftp.ui.views.MainView;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.util.StringConverter;

/**
 * This class represents a toolbar between the top toolbar and PanelView
 */
public class MiddleToolbar extends HBox {
    /**
     * Button for synchronising (refresh) both panels at the same time
     */
    private final Button synchronize;
    /**
     * The button to login/logout if logged in
     */
    private final Button login;
    /**
     * The MainView this toolbar belongs to
     */
    private final MainView mainView;
    /**
     * A combobox for changing the text transfer type
     */
    private final ComboBox<String> transferType;
    /**
     * Constant for the text transfer mode
     */
    private static final String TEXT_TRANSFER_MODE = "Text";
    /**
     * Constant for the binary transfer mode
     */
    private static final String BINARY_TRANSFER_MODE = "Binary";
    /**
     * Text for login button when we want to Login
     */
    private static final String LOGIN_TEXT = "Login";
    /**
     * Text for login button when we want to logout
     */
    private static final String LOGOUT_TEXT = "Logout";

    /**
     * Constructs a MiddleToolbar object
     * @param mainView the MainView instance this toolbar belongs to
     */
    public MiddleToolbar(MainView mainView) {
        this.mainView = mainView;
        synchronize = new Button("Synchronize");
        login = new Button(LOGIN_TEXT);
        transferType = new ComboBox<>();
        init();
    }

    /**
     * This method initialises the buttons
     */
    private void init() {
        setSpacing(15);
        setPadding(new Insets(5));
        synchronize.setOnAction(e -> mainView.synchronize());
        synchronize.setTooltip(new Tooltip("Refresh both local and remote panels"));
        initLogin();
        getChildren().addAll(synchronize, login);
        initTransferTypeBox();
    }

    /**
     * This method initialises the login button
     */
    private void initLogin() {
        login.textProperty().bindBidirectional(mainView.getLoggedInProperty(), new StringConverter<>() {
            @Override
            public String toString(Boolean aBoolean) {
                return aBoolean ? LOGOUT_TEXT:LOGIN_TEXT;
            } // if we are logged in, clicking the button should log out and if logged out, clicking the button should log you in

            @Override
            public Boolean fromString(String s) {
                if (s.equals(LOGOUT_TEXT)) // if the text of the button is Logout, the panel is logged in, so return true
                    return true;
                else if (s.equals(LOGIN_TEXT)) // if the text of the button is Login, the panel is logged out, so return false
                    return false;
                else
                    throw new IllegalArgumentException("Invalid value set for MiddleToolbar.login text. Value provided: " + s + ", one of {Logout, Login} expected");
            }
        });
        login.setOnAction(e -> handleLoginPress());
        login.setTooltip(new Tooltip("Login/Logout the current session (Ctrl+L)"));
    }

    /**
     * This method handles a login button press event. If MainView is logged in, this logs out,
     * if logged out, it calls UI.doLogin
     */
    public void handleLoginPress() {
        if (mainView.isLoggedIn()) {
            mainView.logout();
        } else {
            LoginWindow loginWindow = mainView.getLoginWindow();
            if (loginWindow.isShowing())
                loginWindow.toFront();
            else
                loginWindow.show(); // this should be the same as UI.doLogin since we only have one MainView instance but call this window's show in case
        }
    }

    /**
     * Initialises the combobox for choosing the transfer type
     */
    private void initTransferTypeBox() {
        HBox comboBox = new HBox();

        Label label = new Label("Transfer Mode: ");
        label.setFont(Font.font("Monospaced"));
        transferType.setTooltip(new Tooltip("Choose the text transfer mode"));
        transferType.getItems().addAll(TEXT_TRANSFER_MODE, BINARY_TRANSFER_MODE);
        transferType.setValue(TEXT_TRANSFER_MODE);
        transferType.setOnAction(e -> setTextTransferMode());
        transferType.disableProperty().bind(mainView.getPanelView().getRemoteConnectedProperty().not()); // if not connected, disable the transfer type combo box

        label.setPadding(new Insets(4.5, 0, 0, 5));
        comboBox.getChildren().addAll(new Separator(Orientation.VERTICAL), label, transferType);

        getChildren().addAll(comboBox);
    }

    /**
     * Set the text transfer mode using the combobox value
     */
    private void setTextTransferMode() {
        String value = transferType.getValue();
        FTPConnection connection = FTPSystem.getConnection();

        if (connection != null && connection.isLoggedIn()) {
            try {
                if (value.equals(TEXT_TRANSFER_MODE)) {
                    connection.setTextTransferMode(true);
                } else if (value.equals(BINARY_TRANSFER_MODE)) {
                    connection.setTextTransferMode(false);
                }
            } catch (FTPException ex) {
                UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
            }
        }
    }
}
