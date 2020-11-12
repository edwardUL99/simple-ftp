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

import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.containers.FilePanelContainer;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
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
     * The container that opened this property window
     */
    private final FilePanelContainer parentContainer;

    /**
     * The line entry this property window is displaying
     */
    private final LineEntry lineEntry;

    /**
     * The namePanel containing the line entry icon and name
     */
    private HBox namePanel;

    /**
     * The properties panel containing all the properties and toggles (to change size unit)
     */
    private PropertiesPanel propertiesPanel;

    enum SizeUnit {
        BYTES,
        KILOBYTES,
        MEGABYTES,
        GIGABYTES
    }

    /**
     * Constructs a FilePropertyWindow with the provided container and line entry
     * @param parentContainer the container that opened this window
     * @param lineEntry the LineEntry to display properties of
     */
    public FilePropertyWindow(FilePanelContainer parentContainer, LineEntry lineEntry) {
        this.parentContainer = parentContainer;
        this.lineEntry = lineEntry;
        setSpacing(10);
        setStyle(UI.WHITE_BACKGROUND);
        initNamePanel();
        Separator separator = new Separator();
        separator.setOrientation(Orientation.HORIZONTAL);
        propertiesPanel = new PropertiesPanel(this);

        getChildren().addAll(namePanel, separator, propertiesPanel);
    }

    /**
     * Determines the type of the line entry and returns the associated image url
     * @return image url to display
     */
    private String getImageURL() {
        if (lineEntry instanceof DirectoryLineEntry) {
            return "dir_icon.png";
        } else {
            return "file_icon.png";
        }
    }

    /**
     * Initialises the namePanel
     */
    private void initNamePanel() {
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
        Scene scene = new Scene(this, UI.PROPERTIES_WINDOW_WIDTH, UI.PROPERTIES_WINDOW_HEIGHT);
        Stage stage = new Stage();
        stage.setTitle(lineEntry.file.getName() + " Properties");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    private class PropertiesPanel extends VBox {
        /**
         * The property window this properties panel is a part of
         */
        private final FilePropertyWindow propertyWindow;
        /**
         * The dynamic label where the size units can change
         */
        private Label sizeLabel; // need to have this as an instance variable for the toggle buttons to access it
        /**
         * The fileSize of the line entry
         */
        private Integer fileSize;

        /**
         * Creates a properties panel
         * @param propertyWindow the window this panel is a part of
         */
        public PropertiesPanel(FilePropertyWindow propertyWindow) {
            this.propertyWindow = propertyWindow;
            setSpacing(20);
            setPadding(UI.PROPERTIES_WINDOW_INSETS);
            setStyle(UI.WHITE_BACKGROUND);
            initPropertiesText();
            initSizeOptions();
        }

        /**
         * Initialises the text for the properties
         */
        private void initPropertiesText() {
            LineEntry lineEntry = propertyWindow.lineEntry;
            CommonFile file = lineEntry.file;
            Label fullPath = new Label("Full Path:\t\t\t\t\t\t" + file.getFilePath());
            Label type = new Label("File Type:\t\t\t\t\t\t" + (lineEntry instanceof FileLineEntry ? "File":"Directory"));
            Label permissions = new Label("Permissions:");
            Label modificationTime = new Label("Modification Time:");
            sizeLabel = new Label("Size:");

            try {
                permissions.setText("Permissions:\t\t\t\t\t" + lineEntry.calculatePermissionsString());
                String[] modificationSize = lineEntry.getModificationTimeAndSize().split(" ");
                String modificationTimeStr = "";
                for (int i = 1; i < modificationSize.length; i++) {
                    modificationTimeStr += modificationSize[i] + " ";
                }
                modificationTime.setText("Modification Time:\t\t\t\t" + modificationTimeStr);
                sizeLabel.setText("Size:\t\t\t\t\t\t\t" + modificationSize[0] + "B");
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            getChildren().addAll(fullPath, type, permissions, modificationTime, sizeLabel);
        }

        /**
         * Lazily Initialises the size of the file
         */
        private void initFileSize() throws Exception {
            if (fileSize == null) {
                String[] textSplit = propertyWindow.lineEntry.getModificationTimeAndSize().trim().split(" ");
                fileSize = Integer.valueOf(textSplit[0]);
            }
        }

        /**
         * Sets the units of the size label
         * @param sizeUnit the size unit to change to
         */
        private void setSizeUnits(SizeUnit sizeUnit) throws Exception {
            initFileSize();
            String sizeStr = "" + fileSize;

            try {
                float size = Float.parseFloat("" + fileSize);
                String unit;

                if (sizeUnit == SizeUnit.BYTES) {
                    unit = "B";
                    // size returned by textSplit is already in bytes
                } else if (sizeUnit == SizeUnit.KILOBYTES) {
                    unit = "K";
                    size /= 1000;
                } else if (sizeUnit == SizeUnit.MEGABYTES) {
                    unit = "M";
                    size /= 1000000;
                } else {
                    unit = "G";
                    size /= 1000000000;
                }

                sizeStr = "" + size;
                if (sizeStr.endsWith(".0")) {
                    sizeStr = sizeStr.substring(0, sizeStr.indexOf("."));
                }

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

            RadioButton kilos = new RadioButton("KiloBytes");
            kilos.setOnAction(e -> {
                try {
                    setSizeUnits(SizeUnit.KILOBYTES);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            RadioButton mega = new RadioButton("MegaBytes");
            mega.setOnAction(e -> {
                try {
                    setSizeUnits(SizeUnit.MEGABYTES);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            RadioButton giga = new RadioButton("GigaBytes");
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
