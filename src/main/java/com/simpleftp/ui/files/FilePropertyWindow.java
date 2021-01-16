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

package com.simpleftp.ui.files;

import com.simpleftp.filesystem.FileUtils;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.directories.DirectoryPane;
import com.simpleftp.ui.interfaces.Window;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * This FilePropertyWindow shows a LineEntry object "graphically" as file properties with other properties
 */
public class FilePropertyWindow extends VBox implements Window {
    /**
     * The line entry this property window is displaying
     */
    private final LineEntry lineEntry;

    /**
     * The namePanel containing the line entry icon and name
     */
    private HBox namePanel;

    /**
     * The properties panel displaying properties
     */
    private final PropertiesPanel propertiesPanel;

    /**
     * Display the permissions box or not
     */
    private final boolean displayPermissionsBox;
    /**
     * The stage used to show this window
     */
    private Stage stage;

    /**
     * A separator for separating properties from header
     */
    private final Separator headerSeparator;

    /**
     * A separator for separation permissions from properties
     */
    private Separator permissionsSeparator;

    /**
     * True if this has been fully initialised
     */
    private boolean initialised;

    /**
     * Max length for path before abbreviating
     */
    private static final int MAX_PATH_LENGTH = 25;

    /**
     * Size Units to display file size in
     */
    enum SizeUnit {
        BYTES,
        KILOBYTES,
        MEGABYTES,
        GIGABYTES
    }

    /**
     * Constructs a FilePropertyWindow with the provided container and line entry
     * @param lineEntry the LineEntry to display properties of
     * @throws FileSystemException if an error occurs accessing the file system to query the file
     */
    public FilePropertyWindow(LineEntry lineEntry) throws FileSystemException {
        this.lineEntry = lineEntry;
        setSpacing(10);
        setStyle(UI.WHITE_BACKGROUND);
        initNamePanel();
        headerSeparator = new Separator();
        headerSeparator.setOrientation(Orientation.HORIZONTAL);
        propertiesPanel = new PropertiesPanel();

        String osName = UI.OS_NAME.toLowerCase();
        displayPermissionsBox = !lineEntry.isLocal() || osName.contains("nix") || osName.contains("nux") || osName.contains("aix");

        if (displayPermissionsBox) {
            // we have a linux (or remote window) os, show properties editing
            permissionsSeparator = new Separator();
            permissionsSeparator.setOrientation(Orientation.HORIZONTAL);
        }

        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE)
                close();
        });
    }

    /**
     * Does all necessary initialisations and adds all elements to children
     */
    private void initChildren() {
        getChildren().addAll(namePanel, headerSeparator, propertiesPanel);

        if (displayPermissionsBox) {
            getChildren().addAll(permissionsSeparator, new PermissionsBox());
        }

        initialised = true;
    }

    /**
     * Determines the type of the line entry and returns the associated image url
     * @return image url to display
     */
    private String getImageURL() throws FileSystemException {
        CommonFile file = lineEntry.getFile();
        if (file.isADirectory()) {
            return file.isSymbolicLink() ? "dir_icon_symlink.png":"dir_icon.png";
        } else if (file.isNormalFile()) {
            return file.isSymbolicLink() ? "file_icon_symlink.png":"file_icon.png";
        } else {
            throw new FileSystemException("Cannot determine the file type, it may not exist");
        }
    }

    /**
     * Initialises the namePanel
     */
    private void initNamePanel() throws FileSystemException {
        namePanel = new HBox();
        namePanel.setSpacing(10);
        namePanel.setPrefHeight(100);
        namePanel.setPadding(UI.PROPERTIES_WINDOW_INSETS);
        namePanel.setStyle(UI.SMOKE_WHITE_BACKGROUND);
        ImageView image = new ImageView(getImageURL());
        image.setFitHeight(50);
        image.setFitWidth(50);

        Label name = new Label(lineEntry.getFile().getName());
        name.setFont(Font.font("Monospaced", 20));
        namePanel.setAlignment(Pos.CENTER_LEFT);

        namePanel.getChildren().addAll(image, name);
    }

    /**
     * Shows this FilePropertyWindow
     */
    public void show() {
        if (!initialised) {
            initChildren();
        }

        boolean symLink = lineEntry.file.isSymbolicLink();
        int height = displayPermissionsBox ? UI.PROPERTIES_WINDOW_HEIGHT_PERMISSIONS:UI.PROPERTIES_WINDOW_HEIGHT;
        height = symLink ? height + 30:height;
        Scene scene = new Scene(this, UI.PROPERTIES_WINDOW_WIDTH, height);
        stage = new Stage();
        stage.setTitle(lineEntry.file.getName() + " Properties");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Defines the basic operation of closing a window.
     * This usually entails doing some clean up and then calling the stage that was opened to close.
     */
    @Override
    public void close() {
        if (stage != null)
            stage.close();
    }

    /**
     * This class represents the panel of properties
     */
    private class PropertiesPanel extends VBox {
        /**
         * The dynamic label where the size units can change
         */
        private Label sizeLabel; // need to have this as an instance variable for the toggle buttons to access it
        /**
         * The fileSize of the line entry
         */
        private Long fileSize;
        /**
         * The permissions string to display for permissions
         */
        private Label permissionsString;
        /**
         * Radio button for choosing to display size in bytes
         */
        private RadioButton bytes;
        /**
         * Radio button for choosing to display size in kilobytes
         */
        private RadioButton kilos;
        /**
         * Radio button for choosing to display size in megabytes
         */
        private RadioButton mega;
        /**
         * Radio button for choosing to display size in gigabytes
         */
        private RadioButton giga;
        /**
         * The toggle group containing the radio buttons
         */
        private ToggleGroup toggleGroup;

        /**
         * Creates a properties panel
         */
        public PropertiesPanel() {
            setSpacing(20);
            setPadding(UI.PROPERTIES_WINDOW_INSETS);
            setStyle(UI.WHITE_BACKGROUND);
            initPropertiesText();
            initSizeOptions();
        }

        /**
         * Checks if the file path is within MAX_PATH_LENGTH and if not, abbreviates it
         * @param filePath the file path to abbreviate
         * @return the file path if under the size limit, abbreviated if over
         */
        private String abbreviateFilePath(String filePath) {
            if (filePath.length() > MAX_PATH_LENGTH) {
                filePath = filePath.substring(0, MAX_PATH_LENGTH - 3) + "...";
            }

            return filePath;
        }

        /**
         * Initialises the text for the properties
         */
        private void initPropertiesText() {
            initFilePathText();
            initFileType();
            initSymLinkTarget();
            initFileStats();
        }

        /**
         * Initialises the file path text
         */
        private void initFilePathText() {
            HBox pathBox = new HBox();
            Label pathTitle = new Label("Full Path:\t\t\t\t\t\t");
            String filePath = lineEntry.getFilePath();

            Tooltip pathTooltip = new Tooltip(filePath);
            filePath = abbreviateFilePath(filePath);

            Label pathLabel = new Label(filePath);
            pathLabel.setTooltip(pathTooltip);
            pathBox.getChildren().addAll(pathTitle, pathLabel);
            getChildren().add(pathBox);
        }

        /**
         * Initialises the file type property
         */
        private void initFileType() {
            String fileType;
            CommonFile file = lineEntry.file;

            try {
                if (file.isSymbolicLink()) {
                    fileType = "Symbolic Link";
                } else if (file.isADirectory()) {
                    fileType = "Directory";
                } else if (file.isNormalFile()) {
                    fileType = "File";
                } else {
                    fileType = "Unknown Type";
                }
            } catch (FileSystemException ex) {
                if (FTPSystem.isDebugEnabled())
                    ex.printStackTrace();
                fileType = "Unknown Type";
            }

            Label type = new Label("File Type:\t\t\t\t\t\t" + fileType);
            getChildren().add(type);
        }

        /**
         * Initialises the text displaying the Symbolic link target. This is a no-op of the file is not a symbolic link
         */
        private void initSymLinkTarget() {
            CommonFile file = lineEntry.file;
            if (file.isSymbolicLink()) {
                HBox symPathBox = new HBox();
                Label pathTitle = new Label("Target:\t\t\t\t\t\t");

                String target;

                try {
                    target = lineEntry.getSymbolicLinkTarget();
                } catch (Exception ex) {
                    target = "Could not be determined";
                }

                Tooltip pathTooltip = new Tooltip(target);
                target = abbreviateFilePath(target);

                Label pathLabel = new Label(target);
                pathLabel.setTooltip(pathTooltip);
                symPathBox.getChildren().addAll(pathTitle, pathLabel);
                getChildren().add(symPathBox);
            }
        }

        /**
         * Initialises the size, permissions and modification time
         */
        private void initFileStats() {
            HBox permissionsBox = new HBox();
            Label permissions = new Label("Permissions:\t\t\t\t\t");
            permissionsString = new Label();
            permissionsBox.getChildren().addAll(permissions, permissionsString);
            Label modificationTime = new Label("Modification Time:");
            sizeLabel = new Label("Size:");

            try {
                CommonFile file = lineEntry.file;
                permissionsString.setText(file.getPermissions());
                modificationTime.setText("Modification Time:\t\t\t\t" + lineEntry.getModificationTime());
                initFileSize();
                sizeLabel.setText("Size:\t\t\t\t\t\t\t" + fileSize + "B");
            } catch (Exception ex) {
                if (FTPSystem.isDebugEnabled())
                    ex.printStackTrace();
            }

            getChildren().addAll(permissionsBox, modificationTime, sizeLabel);
        }

        /**
         * Lazily Initialises the size of the file
         */
        private void initFileSize() throws Exception {
            if (fileSize == null) {
                fileSize = lineEntry.getSize();
            }
        }

        /**
         * Sets the units of the size label
         * @param sizeUnit the size unit to change to
         */
        private void setSizeUnits(SizeUnit sizeUnit) {
            String sizeStr;

            try {
                float size = Float.parseFloat("" + fileSize);
                String unit;

                if (sizeUnit == SizeUnit.BYTES) {
                    unit = "B";
                    // size returned by textSplit is already in bytes
                    sizeStr = "" + fileSize;
                } else if (sizeUnit == SizeUnit.KILOBYTES) {
                    unit = "K";
                    size /= 1000;
                    sizeStr = String.format("%.4f", size);
                } else if (sizeUnit == SizeUnit.MEGABYTES) {
                    unit = "M";
                    size /= 1000000;
                    sizeStr = String.format("%.4f", size);
                } else {
                    unit = "G";
                    size /= 1000000000;
                    sizeStr = String.format("%.4f", size);
                }

                if (sizeStr.equals("0.0000"))
                    sizeLabel.setTooltip(new Tooltip("File size is too small to be displayed in 4 decimal places"));
                else
                    sizeLabel.setTooltip(null);

                sizeLabel.setText("Size:\t\t\t\t\t\t\t" + sizeStr + unit);
            } catch (NumberFormatException nf) {
                nf.printStackTrace();
            }
        }

        /**
         * Returns true if this key event represents an arrow key
         * @param keyEvent the key event to check
         * @return true if arrow key, false if not
         */
        private boolean isKeyArrow(KeyEvent keyEvent) {
            KeyCode code = keyEvent.getCode();
            return code == KeyCode.LEFT || code == KeyCode.RIGHT || code == KeyCode.UP || code == KeyCode.DOWN;
        }

        /**
         * Handles an arrow key pressed on the associated key event
         * @param keyEvent the associated key event
         */
        private void handleArrowKey(KeyEvent keyEvent) {
            Toggle selectedToggle = toggleGroup.getSelectedToggle();
            if (isKeyArrow(keyEvent) && selectedToggle != null) {
                if (selectedToggle.equals(bytes)) {
                    setSizeUnits(SizeUnit.BYTES);
                } else if (selectedToggle.equals(kilos)) {
                    setSizeUnits(SizeUnit.KILOBYTES);
                } else if (selectedToggle.equals(mega)) {
                    setSizeUnits(SizeUnit.MEGABYTES);
                } else if (selectedToggle.equals(giga)) {
                    setSizeUnits(SizeUnit.GIGABYTES);
                }
            }
        }

        /**
         * Sets up the arrow key handler for selecting radio buttons.
         */
        private void setArrowKeyHandler() {
            bytes.setOnKeyPressed(this::handleArrowKey);
            kilos.setOnKeyPressed(this::handleArrowKey);
            mega.setOnKeyPressed(this::handleArrowKey);
            giga.setOnKeyPressed(this::handleArrowKey);
        }

        /**
         * Initialises the size options for choosing file size unit
         */
        private void initSizeOptions() {
            VBox sizeOptionsPanel = new VBox();
            sizeOptionsPanel.setSpacing(10);
            sizeOptionsPanel.getChildren().add(new Label("Size Unit"));

            HBox buttonsPanel = new HBox();
            buttonsPanel.setSpacing(20);

            bytes = new RadioButton("Bytes");
            bytes.setSelected(true);
            bytes.setOnAction(e -> setSizeUnits(SizeUnit.BYTES));

            kilos = new RadioButton("Kilobytes");
            kilos.setOnAction(e -> setSizeUnits(SizeUnit.KILOBYTES));

            mega = new RadioButton("Megabytes");
            mega.setOnAction(e -> setSizeUnits(SizeUnit.MEGABYTES));

            giga = new RadioButton("Gigabytes");
            giga.setOnAction(e -> setSizeUnits(SizeUnit.GIGABYTES));

            toggleGroup = new ToggleGroup();
            toggleGroup.getToggles().addAll(bytes, kilos, mega, giga);

            setArrowKeyHandler();
            buttonsPanel.getChildren().addAll(bytes, kilos, mega, giga);
            sizeOptionsPanel.getChildren().add(buttonsPanel);
            getChildren().add(sizeOptionsPanel);
        }
    }

    /**
     * This class represents the box to change file permissions in the property window
     */
    private class PermissionsBox extends VBox {
        /**
         * The text field containing the permissions octal number
         */
        private final TextField permissionsField;
        /**
         * The button to submit changes
         */
        private final Button submit;
        /**
         * A button to preview what the resulting permission would be
         */
        private final Button preview;
        /**
         * The last octal before a permissions change
         */
        private String currentOctal;

        /**
         * Constructs the PermissionsBox object
         */
        public PermissionsBox() {
            currentOctal = FileUtils.permissionsToOctal(lineEntry.getFile().getPermissions());
            permissionsField = new TextField(currentOctal);
            permissionsField.setTooltip(new Tooltip("Enter new permissions value as a 3 digit octal notation number"));
            permissionsField.setOnAction(e -> submitPermissions());

            submit = new Button("Submit");
            submit.setOnAction(e -> submitPermissions());
            submit.setTooltip(new Tooltip("Submit permissions changes"));

            preview = new Button("Preview");
            preview.setOnAction(e -> previewPermissions());
            preview.setTooltip(new Tooltip("Preview the resulting permissions from the entered octal notation"));

            initLayout();
            initSymbolicMessage();
        }

        /**
         * Initialises the layout of the components
         */
        private void initLayout() {
            Label label = new Label("Change Permissions: ");
            setSpacing(5);
            setPadding(new Insets(0, 0, 0, 5));

            HBox permissionsBox = new HBox();
            permissionsBox.setAlignment(Pos.CENTER);
            permissionsBox.getChildren().add(permissionsField);

            HBox buttonBox = new HBox();
            buttonBox.setSpacing(5);
            buttonBox.setAlignment(Pos.CENTER);
            buttonBox.getChildren().addAll(preview, submit);

            setAlignment(Pos.CENTER_LEFT);
            getChildren().addAll(label, permissionsBox, buttonBox);
        }

        /**
         * Checks if the line entry represents a symbolic file and if so, disables the buttons and changes the tool tip
         */
        private void initSymbolicMessage() {
            if (lineEntry.getFile().isSymbolicLink()) {
                submit.setDisable(true);
                preview.setDisable(true);
                permissionsField.setEditable(false);
                permissionsField.setTooltip(new Tooltip("Can only change permissions of the link's target"));
            }
        }

        /**
         * Displays an error message stating why the octal is not valid and resets the text field
         * @param octal the octal that is invalid
         * @param errorMessage the error message to display
         * @param resetPermissionsField true to reset permissions field to the current octal
         */
        private void doInvalidOctalMessage(String octal, String errorMessage, boolean resetPermissionsField) {
            UI.doError("Invalid Octal Notation", "The octal notation " + octal + " is not valid because: " + errorMessage
                    + ". Please fix the error and submit again");
            if (resetPermissionsField)
                permissionsField.setText(currentOctal);
        }

        /**
         * Changes permissions on a local file
         * @param chmodOctal the octal permissions
         * @return true if successful, false if not
         * @throws Exception if any exception occurs
         */
        private boolean changeLocalPermissions(String chmodOctal) throws Exception {
            Path path = Files.setPosixFilePermissions(Path.of(lineEntry.getFilePath()), PosixFilePermissions.fromString(FileUtils.convertOctalToPermissions(chmodOctal)));
            return Files.exists(path);
        }

        /**
         * Changes permissions on a remote file
         * @param chmodOctal the octal permissions
         * @return true if successful, false if not
         * @throws Exception if any exception occurs
         */
        private boolean changeRemotePermissions(String chmodOctal) throws Exception {
            String path = lineEntry.getFilePath();

            return lineEntry.getOwningPane()
                    .getFileSystem()
                    .getFTPConnection()
                    .chmod(chmodOctal, path);
        }

        /**
         * Attempts to submit the permissions change
         * @param chmodOctal the octal permissions string
         * @return true if succeeded, false if not
         */
        private boolean changePermissions(String chmodOctal) throws Exception {
            boolean local = lineEntry.isLocal();

            if (local)
                return changeLocalPermissions(chmodOctal);
            else
                return changeRemotePermissions(chmodOctal);
        }

        /**
         * The handler for submitting permissions changes
         */
        private void submitPermissions() {
            String permissions = permissionsField.getText();

            if (!permissions.equals(currentOctal)) {
                Object[] validity = FileUtils.isValidOctal(permissions);
                boolean valid = (boolean) validity[0];

                if (!valid) {
                    doInvalidOctalMessage(permissions, (String)validity[1], true);
                } else {
                    try {
                        if (changePermissions(permissions)) {
                            permissionsField.setText(permissions);
                            String lastOctal = currentOctal;
                            currentOctal = permissions;
                            propertiesPanel.permissionsString.setText(lineEntry.getFile().getPermissions());
                            UI.doInfo("Permissions Changed", "Permissions have been changed successfully from " + lastOctal + " to " + permissions);

                            DirectoryPane directoryPane = lineEntry.getOwningPane();
                            if (directoryPane.getCurrentWorkingDirectory().equals(FileUtils.getParentPath(lineEntry.getFilePath(), lineEntry.isLocal())))
                                lineEntry.refresh();
                        }
                    } catch (Exception ex) {
                        if (FTPSystem.isDebugEnabled())
                            ex.printStackTrace();
                        UI.doError("Permissions Change Exception", "Failed to change permissions due to an exception with the message: " + ex.getMessage());
                        permissionsField.setText(currentOctal);
                    }
                }
            }
        }

        /**
         * Allows to preview the resulting permissions from the entered octal
         */
        private void previewPermissions() {
            String enteredPermissions = permissionsField.getText();
            Object[] validity = FileUtils.isValidOctal(enteredPermissions);
            boolean valid = (boolean)validity[0];

            if (valid) {
                char firstChar = lineEntry.getFile().getPermissions().charAt(0);
                UI.doInfo("Permissions Preview", "The permissions that would be produced for octal notation "
                    + enteredPermissions + " is: " + (firstChar + FileUtils.convertOctalToPermissions(enteredPermissions))
                    + (enteredPermissions.equals(currentOctal) ? ". This operation will not result in any changes as the octal permissions given equals the current permissions":""));
            } else {
                doInvalidOctalMessage(enteredPermissions, (String)validity[1], false);
            }
        }
    }
}
