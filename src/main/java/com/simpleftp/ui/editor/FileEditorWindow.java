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

package com.simpleftp.ui.editor;

import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.filesystem.LocalFileSystem;
import com.simpleftp.filesystem.RemoteFile;
import com.simpleftp.filesystem.exceptions.FileSystemException;
import com.simpleftp.filesystem.interfaces.CommonFile;
import com.simpleftp.ui.UI;
import com.simpleftp.ui.panels.FilePanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * This class represents a Window for editing and saving files
 */
public class FileEditorWindow extends VBox {
    /**
     * The file panel that opened this window
     */
    private final FilePanel creatingPanel;
    /**
     * The file to display in this editor
     */
    private final CommonFile file;
    /**
     * The editor being used
     */
    private final TextArea editor;
    /**
     * The stage that will show this window
     */
    private Stage stage;
    /**
     * This is true if the file is unedited or saved
     */
    private boolean saved;
    /**
     * The button that will save the file
     */
    private Button save;
    /**
     * The button to reset the file back to original state
     */
    private Button reset;
    /**
     * The HBox holding the buttons
     */
    private HBox buttonBar;
    /**
     * The original text before edits
     */
    private String originalText;

    /**
     * Constructs a FileEditorWindow with the specified panel and file
     * @param creatingPanel the FilePanel opening this window
     * @param file the file being edited
     */
    public FileEditorWindow(FilePanel creatingPanel, CommonFile file) {
        this.creatingPanel = creatingPanel;
        this.file = file;
        editor = new TextArea();
        ScrollPane editorScrollPane = new ScrollPane();
        editorScrollPane.setContent(editor);
        editorScrollPane.setFitToHeight(true);
        editorScrollPane.setFitToWidth(true);
        buttonBar = new HBox();
        VBox.setVgrow(editorScrollPane, Priority.ALWAYS);
        getChildren().addAll(buttonBar, editorScrollPane);
        saved = true;
        save = new Button();
        save.setMnemonicParsing(true);
        save.setText("_Save");
        save.setOnAction(e -> save());
        reset = new Button("Reset"); // don't have mnemonic for rest as it is destructive
        reset.setTooltip(new Tooltip("This will discard all your changes"));
        reset.setOnAction(e -> reset());
        setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.S) {
                save();
            }
        });
        initButtonBar();
    }

    /**
     * Initialises the button bar
     */
    private void initButtonBar() {
        buttonBar.setSpacing(10);
        buttonBar.getChildren().addAll(save, reset);
        buttonBar.setBorder(new Border(new BorderStroke(Paint.valueOf("BLACK"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        buttonBar.setStyle(UI.GREY_BACKGROUND);
    }

    /**
     * Resets the edited text to original
     */
    private void reset() {
        editor.setText(originalText);
        stage.setTitle(stage.getTitle().replaceAll("\\*", ""));
        saved = true;
        editor.requestFocus();
    }

    /**
     * Saves the file if edited
     */
    private boolean save() {
        if (!saved) {
            try {
                String text = editor.getText();
                FileSaver saver = new FileSaver(creatingPanel.getFileSystem());
                saver.saveFile(file.getFilePath(), text);
                creatingPanel.refresh();
                stage.setTitle(stage.getTitle().replaceAll("\\*", ""));
                saved = true;
                editor.requestFocus();
                originalText = text;
                return true;
            } catch (Exception ex) {
                ex.printStackTrace();
                UI.doError("File not saved", "An error occurred saving the file: " + ex.getMessage());
            }
        }

        return false;
    }

    /**
     * Opens the file and returns it as a string
     * @file the file to display
     * @return the file contents as a String
     * @throws IOException if the reader fails to read the file
     */
    private String fileToString(CommonFile file) throws IOException {
        String str = "";

        if (file instanceof LocalFile) {
            LocalFile localFile = (LocalFile)file;
            BufferedReader reader = new BufferedReader(new FileReader(localFile));

            String line;
            while ((line = reader.readLine()) != null) {
                str += line + "\n";
            }
        } else {
            try {
                RemoteFile remoteFile = (RemoteFile) file;
                LocalFile downloaded = new LocalFile(System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + remoteFile.getName());
                new LocalFileSystem(creatingPanel.getFileSystem().getFTPConnection()).addFile(remoteFile, downloaded.getParentFile().getAbsolutePath()); // download the file
                String ret = fileToString(downloaded);
                downloaded.delete();

                return ret;
            } catch (FileSystemException ex) {
                UI.doException(ex, UI.ExceptionType.EXCEPTION, true);
            }
        }

        return str;
    }

    /**
     * Initialises this window
     */
    private void initWindow() throws Exception {
        String text = fileToString(file);
        editor.setText(text);
        originalText = text;
        editor.setOnKeyTyped(e -> {
            if (!e.isControlDown()) {
                if (saved) {
                    saved = false;
                    stage.setTitle("*" + stage.getTitle());
                }
            }
        });
    }

    /**
     * Gets the file path to display as a title on the opened file window
     * @return the file path
     */
    private String getFilePath() {
        String filePath = file.getFilePath();

        if (file instanceof RemoteFile) {
            filePath = "ftp://" + filePath;
        }

        return filePath;
    }

    /**
     * Shows this FileEditorWindow
     */
    public void show() {
        try {
            initWindow();
            stage = new Stage();
            stage.setTitle(getFilePath());
            stage.setScene(new Scene(this, UI.FILE_EDITOR_WIDTH, UI.FILE_EDITOR_HEIGHT));
            stage.show();
            editor.requestFocus();
            stage.setOnCloseRequest(e -> {
                if (!saved) {
                    if (UI.doUnsavedChanges()) {
                        if (!save()) {
                            e.consume(); // consume so you can view what happened
                        }
                    }
                }
            });
        } catch (Exception ex) {
            UI.doException(ex, UI.ExceptionType.EXCEPTION, true);
        }
    }

}
