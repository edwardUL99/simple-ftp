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

import com.simpleftp.ui.panels.FileLineEntry;
import com.simpleftp.ui.panels.LineEntry;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import javax.sound.sampled.Line;

public class FTPClient extends Application {

    private int clickNum = 1;

    public static void main(String[] args) {
        launch(args);
    }

    public void click(LineEntry lineEntry) {
        if (clickNum < 2) {
            clickNum++;
        } else {
            Stage newStage = new Stage();
            newStage.setTitle(lineEntry.getChildren().get(1).toString());

            StackPane stackPane = new StackPane();
            stackPane.getChildren().add(new Text("Popup"));
            newStage.setScene(new Scene(stackPane, 300, 300));
            newStage.show();
            clickNum = 1;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        FileLineEntry lineEntry = new FileLineEntry();

        lineEntry.setOnMouseClicked(e -> click(lineEntry));

        StackPane stackPane = new StackPane();
        stackPane.getChildren().add(lineEntry);

        Scene scene = new Scene(stackPane, 500, 200);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Test");
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}
