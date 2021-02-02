/*
 *  Copyright (C) 2020-2021  Edward Lynch-Milner
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

import lombok.Getter;
import org.fxmisc.richtext.StyleClassedTextArea;

/**
 * This class provides the FileEditor for a FileEditorWindow
 */
public class FileEditor extends StyleClassedTextArea {
    /**
     * This is the parent editor window of this FileEditorWindow
     */
    @Getter
    private final FileEditorWindow editorWindow;

    /**
     * Constructs a FileEditor object
     * @param editorWindow the parent editor window object
     */
    public FileEditor(FileEditorWindow editorWindow) {
        this.editorWindow = editorWindow;
        setOnKeyTyped(e -> {
            if (!e.isControlDown()) {
                if (getText().equals(editorWindow.resetFileContents))
                    editorWindow.setSave(true);
                else if (editorWindow.saved)
                    editorWindow.setSave(false);
            }
        });
    }

    /**
     * Implements ability to paste into the editor window.
     * If the editor window was in a saved state, it isn't saved anymore
     */
    @Override
    public void paste() {
        super.paste();
        if (editorWindow.isSaved()) {
            editorWindow.setSave(false);
        }
    }

    /**
     * Undoes all edits since the last save using Ctrl + Z. This is the equivalent to resetConnection
     */
    @Override
    public void undo() {
        if (!editorWindow.isSaved()) {
            if (getText().equals(editorWindow.getResetFileContents())) {
                editorWindow.setSave(true);
            } else {
                super.undo();
                undo();
            }
        }
    }

    /**
     * Sets the text of the editor
     * @param text the text to set
     */
    public  void setText(String text) {
        clear();
        appendText(text);
    }
}
