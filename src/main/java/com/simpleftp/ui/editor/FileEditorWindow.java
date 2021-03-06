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

package com.simpleftp.ui.editor;

import com.simpleftp.ftp.FTPSystem;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.ftp.exceptions.FTPConnectionFailedException;
import com.simpleftp.ftp.exceptions.FTPNotConnectedException;
import com.simpleftp.properties.Properties;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.directories.DirectoryPane;
import com.simpleftp.ui.files.LineEntry;
import com.simpleftp.ui.interfaces.Window;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import lombok.Getter;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;

/**
 * This class represents a Window for editing and saving files
 */
public abstract class FileEditorWindow extends VBox implements Window {
    /**
     * The file panel that opened this window
     */
    @Getter
    protected final DirectoryPane creatingPane;
    /**
     * The line entry being edited
     */
    @Getter
    protected final LineEntry lineEntry;
    /**
     * The contents of the file
     */
    private final String fileContents;
    /**
     * The editor being used
     */
    @Getter
    private final FileEditor editor;
    /**
     * The ScrollPane for the editor
     */
    private final VirtualizedScrollPane<StyleClassedTextArea> editorScrollPane;
    /**
     * The stage that will show this window
     */
    private Stage stage;
    /**
     * This is true if the file is unedited or saved
     */
    @Getter
    boolean saved;
    /**
     * The button that will save the file
     */
    private final Button save;
    /**
     * The button to resetConnection the file back to original state
     */
    private final Button reset;
    /**
     * The HBox holding the buttons
     */
    private final HBox buttonBar;
    /**
     * A label displaying if the file is currently being saved
     */
    private final Label savingLabel;
    /**
     * The file contents that the resetConnection button brings us back to
     */
    @Getter
    String resetFileContents;

    /**
     * Constructs a FileEditorWindow with the specified panel and file
     * @param creatingPane the DirectoryPane opening this window
     * @param fileContents the contents of the file as this class does not download the contents
     * @param lineEntry the line entry representing the file to edit
     */
    FileEditorWindow(DirectoryPane creatingPane, String fileContents, LineEntry lineEntry) {
        this.creatingPane = creatingPane;
        this.fileContents = resetFileContents = fileContents;
        this.lineEntry = lineEntry;
        this.editor = new FileEditor(this);
        editorScrollPane = new VirtualizedScrollPane<>(editor);
        buttonBar = new HBox();
        VBox.setVgrow(editorScrollPane, Priority.ALWAYS);
        saved = true;
        save = new Button("Save");
        save.setOnAction(e -> save());
        save.setTooltip(new Tooltip("Save all unsaved changes"));

        reset = new Button("Reset"); // don't have mnemonic for resetConnection as it is destructive
        reset.setTooltip(new Tooltip("Reset any changes made since the last successful save"));
        reset.setOnAction(e -> reset());
        setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            if (e.isControlDown() && code == KeyCode.S) {
                save();
            } else if (code == KeyCode.ESCAPE) {
                close();
            }
        });

        savingLabel = new Label("Saving...");
        savingLabel.setVisible(false);
    }

    /**
     * Adds all necessary objects to children
     */
    private void initChildren() {
        getChildren().addAll(buttonBar, editorScrollPane);
        initButtonBar();
    }

    /**
     * Initialises the button bar
     */
    private void initButtonBar() {
        buttonBar.setSpacing(10);
        buttonBar.getChildren().addAll(save, reset, savingLabel);
        buttonBar.setBorder(new Border(new BorderStroke(Paint.valueOf("BLACK"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        buttonBar.setStyle(UI.GREY_BACKGROUND);
        buttonBar.setAlignment(Pos.CENTER_LEFT);
    }

    /**
     * Resets the edited text to original
     */
    public void reset() {
        editor.undo();
    }

    /**
     * Adds the star to the file name section of the editor stage title
     */
    private void addStarToStageTitle() {
        String filePath = stage.getTitle().split("-")[1];
        if (!filePath.contains("*")) {
            filePath = "*" + filePath.trim();

            stage.setTitle("File Editor - " + filePath);
        }
    }

    /**
     * Removes the star from the file name section of the editor stage title
     */
    private void removeStarFromStageTitle() {
        String filePath = stage.getTitle().split("-")[1];
        filePath = filePath.trim().replaceAll("\\*", "");

        stage.setTitle("File Editor - " + filePath);
    }

    /**
     * Gets the path of where to save the file to. If this file is a symbolic link, it is the target path, as uploading to the parent path of the link will break the link
     * @return the path where to save the file
     */
    abstract String getSaveFilePath() throws Exception;

    /**
     * Sets the text to set the file contents that the resetConnection button will bring us back to
     * @param resetFileContents the file contents resetConnection should use
     */
    public void setResetFileContents(String resetFileContents) {
        this.resetFileContents = resetFileContents;
    }

    /**
     * Saves the file if edited
     */
    private boolean save() {
        if (!saved) {
            try {
                String text = editor.getText();
                FileSaver saver = new FileSaver(this);
                String filePath = getSaveFilePath();
                displaySavingLabel(true);
                saver.saveFile(filePath, text);
                setSave(true);
                editor.requestFocus();

                return true;
            } catch (Exception ex) {
                ex.printStackTrace();
                UI.doError("File not saved", "An error occurred saving the file: " + ex.getMessage());
            }
        }

        return false;
    }

    /**
     * Initialises this window
     */
    private void initWindow() {
        editor.setText(fileContents);
        editorScrollPane.scrollXToPixel(0);
        editorScrollPane.scrollYToPixel(0);
    }

    /**
     * Gets the file path to display as a title on the opened file window.
     * If this is remote, it should append what form of remote path it is, e.g. ftp
     * @return the file path
     */
    abstract String getFilePath();

    /**
     * Sets the value for save field variable and determines whether to show star or not on title. THIS DOES NOT SAVE THE FILE
     * @param saved the value for saved
     */
    public void setSave(boolean saved) {
        if (saved) {
            removeStarFromStageTitle();
        } else {
            addStarToStageTitle();
        }

        this.saved = saved;
    }

    /**
     * Sets the visibility of the "Saving..." label based on the display value
     * @param display the value for whether saving should be displayed or not
     */
    public void displaySavingLabel(boolean display) {
        savingLabel.setVisible(display);
    }

    /**
     * Checks if the file still exists.
     * @return true if exists, false if not or it couldn't be determined
     * @throws FileSystemException if an error occurs
     */
    private boolean checkFileStillExists() throws FileSystemException {
        return lineEntry.getFile().exists(); // call exists as this will refresh the file
    }

    /**
     * On exit, checks if the file still exists and if not, does the appropriate action
     * @return true if the event should be consumed, false if not
     * @throws FileSystemException if an error occurs and cannot check if file is saved
     */
    private boolean checkSaveOnExit() throws FileSystemException {
        boolean consumeEvent = false;
        boolean stillExists = checkFileStillExists();

        if (!saved || stillExists) {
            if (!saved) {
                if (!stillExists)
                    UI.doError("File No Longer Exists", "The file may no longer exist, if you want to keep the file, press save in the next dialog", true);
                if (UI.doUnsavedChanges()) {
                    if (!save()) {
                        consumeEvent = true;
                    }
                }
            }
        } else {
            UI.doError("File No Longer Exists", "File may no longer exist, go back and decide to save or just quit");
            setSave(false);
            consumeEvent = true;
        }

        return consumeEvent;
    }

    /**
     * Manages the exception that could be thrown when closing the window
     * @param ex the exception to handle
     * @return true if you should still exit false if not
     */
    private boolean doExitException(FileSystemException ex) {
        Throwable cause = ex.getCause();

        if (cause instanceof FTPConnectionFailedException || cause instanceof FTPNotConnectedException) {
            String messageText = saved ? "Cannot check if the file still exists on the server before closing the window.":"Cannot save any unsaved changes to the file.";
            return UI.doConfirmation("Connection Lost", "The connection to the remote server has been lost. " + messageText + "Confirm that you still want to exit or cancel", true);
        } else {
            String message = cause != null ? cause.getMessage():ex.getMessage();
            return UI.doConfirmation("Connection Error", "An error with the message: " + message + ". You may lose the file if you exit", true);
        }
    }

    /**
     * Shows this FileEditorWindow
     */
    public void show() {
        try {
            initWindow();
            stage = new Stage();
            stage.setTitle("File Editor - " + getFilePath());
            stage.setScene(new Scene(this, Properties.FILE_EDITOR_WIDTH.getValue(), Properties.FILE_EDITOR_HEIGHT.getValue()));
            stage.show();
            editor.requestFocus();
            stage.setOnCloseRequest(e -> {
                boolean consumeEvent;
                try {
                    consumeEvent = checkSaveOnExit();
                } catch (FileSystemException ex) {
                    consumeEvent = !doExitException(ex);
                }

                if (consumeEvent)
                    e.consume();
                else
                    UI.closeFile(lineEntry.getFilePath(), creatingPane.isLocal());
            });
        } catch (Exception ex) {
            UI.doException(ex, UI.ExceptionType.ERROR, FTPSystem.isDebugEnabled());
        }
    }

    /**
     * Closes this FileEditorWindow
     */
    public void close() {
        if (stage != null) {
            boolean closeStage;

            try {
                closeStage = !checkSaveOnExit();
            } catch (FileSystemException ex) {
                closeStage = doExitException(ex);
            }

            if (closeStage) {
                stage.close();
                UI.closeFile(lineEntry.getFilePath(), creatingPane.isLocal());
            }
        }
    }

    /**
     * Constructs a FileEditorWindow with the specified panel and file
     * @param creatingPane the DirectoryPane opening this window
     * @param fileContents the contents of the file as this class does not download the contents
     * @param lineEntry the line entry to edit
     */
    public static FileEditorWindow newInstance(DirectoryPane creatingPane, String fileContents, LineEntry lineEntry) {
        FileEditorWindow editorWindow;
        if (lineEntry.isLocal()) {
            editorWindow = new LocalFileEditorWindow(creatingPane, fileContents, lineEntry);
        } else {
            editorWindow = new RemoteFileEditorWindow(creatingPane, fileContents, lineEntry);
        }

        editorWindow.initChildren();

        return editorWindow;
    }

    /**
     * Returns the hash code of the file in this editor window
     * @return the hash code of the file in this editor window
     */
    @Override
    public int hashCode() {
        return lineEntry.getFile().hashCode();
    }

    /**
     * Determines if this editor window is equal to another based on its file
     * @param obj the object to check equality with
     * @return true if this editor's window equals another irregardless of contents
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FileEditorWindow)) {
            return false;
        } else if (this == obj) {
            return true;
        } else {
            FileEditorWindow editorWindow = (FileEditorWindow)obj;
            return lineEntry.getFile().equals(editorWindow.lineEntry.getFile());
        }
    }
}