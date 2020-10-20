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

package com.simpleftp.ui.panels;

import com.simpleftp.filesystem.interfaces.CommonFile;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.layout.*;

import javafx.scene.image.ImageView;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This is an abstract class representing a line entry on the panel.
 * It can represent a Directory or File Line entry
 */
public abstract class LineEntry extends HBox {
    private ImageView image;
    private CommonFile file;

    protected LineEntry(String imageURL, CommonFile file) {
        setSpacing(20);
        setBorder(new Border(new BorderStroke(Paint.valueOf("BLACK"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        setMaxHeight(5);

        setAlignment(Pos.CENTER_LEFT);

        image = new ImageView();
        image.setImage(new Image(ClassLoader.getSystemResourceAsStream(imageURL)));
        this.file = file;
        getChildren().add(image);

        init();
    }

    private void init() {
        String name = file.getName() + "\t\t";
        String modificationTime = LocalDateTime.parse("19/10/2020 20:45", DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")).format(DateTimeFormatter.ofPattern("MMM dd HH:mm"));
        String permissions = "rwxrwxrw-";
        getChildren().addAll(new Text(name), new Text(modificationTime), new Text(permissions));
    }
}
