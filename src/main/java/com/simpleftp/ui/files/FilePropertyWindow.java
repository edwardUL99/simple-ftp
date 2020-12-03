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

package com.simpleftp.ui.files;

import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.ui.UI;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * This FilePropertyWindow shows a LineEntry object "graphically" as file properties with other properties
 */
public class FilePropertyWindow extends VBox {
    /**
     * The line entry this property window is displaying
     */
    private final LineEntry lineEntry;

    /**
     * The namePanel containing the line entry icon and name
     */
    private HBox namePanel;

    /**
     * Max length for path before abbreviating
     */
    private static final int MAX_PATH_LENGTH = 25;

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
        Separator separator = new Separator();
        separator.setOrientation(Orientation.HORIZONTAL);
        PropertiesPanel propertiesPanel = new PropertiesPanel();

        getChildren().addAll(namePanel, separator, propertiesPanel);
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
        boolean symLink = lineEntry.file.isSymbolicLink();
        int height = symLink ? UI.PROPERTIES_WINDOW_HEIGHT + 30:UI.PROPERTIES_WINDOW_HEIGHT;
        Scene scene = new Scene(this, UI.PROPERTIES_WINDOW_WIDTH, height);
        Stage stage = new Stage();
        stage.setTitle(lineEntry.file.getName() + " Properties");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

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
            Label permissions = new Label("Permissions:");
            Label modificationTime = new Label("Modification Time:");
            sizeLabel = new Label("Size:");

            try {
                CommonFile file = lineEntry.file;
                permissions.setText("Permissions:\t\t\t\t\t" + file.getPermissions());
                modificationTime.setText("Modification Time:\t\t\t\t" + lineEntry.getModificationTime());
                initFileSize();
                sizeLabel.setText("Size:\t\t\t\t\t\t\t" + fileSize + "B");
            } catch (Exception ex) {
                if (FTPSystem.isDebugEnabled())
                    ex.printStackTrace();
            }

            getChildren().addAll(permissions, modificationTime, sizeLabel);
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
        private void setSizeUnits(SizeUnit sizeUnit) throws Exception {
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
         * Initialises the size options for choosing file size unit
         */
        private void initSizeOptions() {
            VBox sizeOptionsPanel = new VBox();
            sizeOptionsPanel.setSpacing(10);
            sizeOptionsPanel.getChildren().add(new Label("Size Unit"));

            HBox buttonsPanel = new HBox();
            buttonsPanel.setSpacing(20);

            RadioButton bytes = new RadioButton("Bytes");
            bytes.setSelected(true);
            bytes.setOnAction(e -> {
                try {
                    setSizeUnits(SizeUnit.BYTES);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            RadioButton kilos = new RadioButton("Kilobytes");
            kilos.setOnAction(e -> {
                try {
                    setSizeUnits(SizeUnit.KILOBYTES);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            RadioButton mega = new RadioButton("Megabytes");
            mega.setOnAction(e -> {
                try {
                    setSizeUnits(SizeUnit.MEGABYTES);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            RadioButton giga = new RadioButton("Gigabytes");
            giga.setOnAction(e -> {
                try {
                    setSizeUnits(SizeUnit.GIGABYTES);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            ToggleGroup toggleGroup = new ToggleGroup();
            toggleGroup.getToggles().addAll(bytes, kilos, mega, giga);

            buttonsPanel.getChildren().addAll(bytes, kilos, mega, giga);
            sizeOptionsPanel.getChildren().add(buttonsPanel);
            getChildren().add(sizeOptionsPanel);
        }
    }
}
