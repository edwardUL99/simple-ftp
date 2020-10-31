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

import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.panels.FilePanel;
import com.simpleftp.ui.panels.LineEntry;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Getter;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * This is a "Container" that contains a FilePanel and a toolbar of options for controlling that FilePanel
 * To hook a FilePanelContainer and a FilePanel together, you need to call FilePanel.setParentContainer()
 */
public class FilePanelContainer extends VBox {
    @Getter
    private FilePanel filePanel;
    private HBox toolBar;
    private Button delete;
    private Button open;
    private StackPane filePanelPane;
    private ComboBox<String> comboBox;

    /**
     * Constructs a FilePanelContainer with the specified filePanel
     * @param filePanel the filePanel this container holds
     */
    public FilePanelContainer(FilePanel filePanel) {
        toolBar = new HBox();
        toolBar.setSpacing(5);
        delete = new Button("Delete");
        delete.setOnAction(e -> delete());
        open = new Button("Open");
        open.setOnAction(e -> open());
        filePanelPane = new StackPane();
        comboBox = new ComboBox<>();
        toolBar.getChildren().addAll(new Label("Files: "), comboBox, delete, open);
        setFilePanel(filePanel);
        getChildren().add(toolBar);
        getChildren().add(filePanelPane);
    }

    private void refreshComboBox() {
        ArrayList<String> fileNames = new ArrayList<>(filePanel.filesDisplayed()
                                                .stream()
                                                .map(LineEntry::getFile)
                                                .map(CommonFile::getName)
                                                .collect(Collectors.toSet()));
        fileNames.forEach(name -> comboBox.getItems().add(name));
        comboBox.getItems().add(0, "");
    }

    private LineEntry getLineEntryFromComboBox() {
        String file = comboBox.getValue();
        if (!file.equals("")) {
            return filePanel.filesDisplayed()
                                .stream()
                                .filter(e -> e.getFile().getName().equals(file))
                                .findFirst().orElse(null);
        } else {
            return null;
        }
    }

    private void delete() {
        LineEntry lineEntry = getLineEntryFromComboBox();

        if (lineEntry != null) {
            if (filePanel.deleteEntry(lineEntry)) {
                UI.doInfo("File deleted successfully", "File " + lineEntry.getFile().getName() + " deleted");
                comboBox.getItems().remove(lineEntry.getFile().getName());
            } else {
                UI.doError("File not deleted", "File " + lineEntry.getFile().getName() + " wasn't deleted");
            }
        }
    }

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
        if (this.filePanel != null) {
            filePanelPane.getChildren().remove(this.filePanel);
        }
        this.filePanel = filePanel;
        if (this.filePanel.getParentContainer() != this) // prevent infinite recursion
            this.filePanel.setParentContainer(this);
        filePanelPane.getChildren().add(filePanel);
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
