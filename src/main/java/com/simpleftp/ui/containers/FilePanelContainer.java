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

package com.simpleftp.ui.containers;

import com.simpleftp.ui.UI;
import com.simpleftp.ui.panels.FilePanel;
import com.simpleftp.ui.panels.LineEntry;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is a "Container" that contains a FilePanel and a toolbar of options for controlling that FilePanel
 * To hook a FilePanelContainer and a FilePanel together, you need to call FilePanel.setParentContainer()
 */
public class FilePanelContainer extends VBox {
    /**
     * The FilePanel this FilePanelContainer is connected tp
     */
    @Getter
    private FilePanel filePanel;
    /**
     * The HBox with combo box and buttons
     */
    private HBox toolBar;
    /**
     * The button for deleting chosen files
     */
    private Button delete;
    /**
     * The button for opening chosen files
     */
    private Button open;
    /**
     * The combobox displaying the base names of files displayed in the FilePanel
     */
    private ComboBox<String> comboBox;
    /**
     * Mapping of file basenames to their respective line entries
     */
    private HashMap<String, LineEntry> entryMappings;

    /**
     * Constructs a FilePanelContainer with the specified filePanel
     * @param filePanel the filePanel this container holds
     */
    public FilePanelContainer(FilePanel filePanel) {
        setStyle(UI.WHITE_BACKGROUND);
        toolBar = new HBox();
        toolBar.setSpacing(5);
        delete = new Button("Delete");
        delete.setOnAction(e -> delete());
        open = new Button("Open");
        open.setOnAction(e -> open());
        comboBox = new ComboBox<>();
        entryMappings = new HashMap<>();
        toolBar.getChildren().addAll(new Label("Files: "), comboBox, delete, open);
        getChildren().add(toolBar);
        setFilePanel(filePanel);
    }

    /**
     * Refreshes the file names displayed in the ComboBox
     */
    private void refreshComboBox() {
        entryMappings.clear();
        comboBox.getItems().clear();
        ArrayList<LineEntry> filesDisplayed = filePanel.filesDisplayed();
        filesDisplayed.forEach(e -> {
            String fileName = e.getFile().getName();
            comboBox.getItems().add(fileName);
            entryMappings.put(fileName, e);
        });

        comboBox.getItems().add(0, "");
    }

    /**
     * Gets the specified LineEntry matching the value chosen in the combo box
     * @return LineEnrty matching file name chosen, can be null
     */
    private LineEntry getLineEntryFromComboBox() {
        String file = comboBox.getValue();
        if (!file.equals("")) {
            return entryMappings.get(file);
        } else {
            return null;
        }
    }

    /**
     * Deletes the chosen line entry from the combobox
     */
    private void delete() {
        LineEntry lineEntry = getLineEntryFromComboBox();

        if (lineEntry != null) {
            if (UI.doConfirmation("Confirm file deletion", "Confirm deletion of " + lineEntry.getFile().getName())) {
                if (filePanel.deleteEntry(lineEntry)) {
                    UI.doInfo("File deleted successfully", "File " + lineEntry.getFile().getName() + " deleted");
                    comboBox.getItems().remove(lineEntry.getFile().getName());
                } else {
                    UI.doError("File not deleted", "File " + lineEntry.getFile().getName() + " wasn't deleted");
                }
            }
        }
    }

    /**
     * Opens the chosen file from the combo box
     */
    private void open() {
        LineEntry lineEntry = getLineEntryFromComboBox();

        if (lineEntry != null) {
            filePanel.openLineEntry(lineEntry);
        }
    }

    /**
     * Sets the file panel for this container.
     * Automatically links the file panel by calling FilePanel.setParentContainer
     * @param filePanel the file panel to set
     */
    public void setFilePanel(FilePanel filePanel) {
        if (this.filePanel != null)
            getChildren().remove(this.filePanel);

        this.filePanel = filePanel;
        if (this.filePanel.getParentContainer() != this) // prevent infinite recursion
            this.filePanel.setParentContainer(this);

        getChildren().add(filePanel);
        refresh();
    }

    /**
     * Refresh this FilePanelContainer
     */
    public void refresh() {
        refreshComboBox();
    }

    /**
     * Sets the combobox to represent this line entry if the name of it is inside the combo box in the first place
     * @param lineEntry the line entry to display
     */
    public void setComboBoxSelection(final LineEntry lineEntry) {
        String name = lineEntry.getFile().getName();

        if (comboBox.getItems().contains(name)) {
            comboBox.setValue(name);
        }
    }
}
