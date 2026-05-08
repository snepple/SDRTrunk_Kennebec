/*
 * *****************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */
package io.github.dsheirer.module.decode.event;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.Component;

public class ActivitySummaryFrame
{
    public ActivitySummaryFrame( String summary )
    {
    	this( summary, null );
    }
    
	public ActivitySummaryFrame( String summary, Component displayOver )
	{
		Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.setTitle( "Activity Summary" );

            VBox root = new VBox(10);
            root.setPadding(new Insets(10));
            root.setAlignment(Pos.CENTER);

            TextArea summaryText = new TextArea( summary );
            summaryText.setEditable(false);
            summaryText.setWrapText(true);
            VBox.setVgrow(summaryText, Priority.ALWAYS);

            Button close = new Button( "Close" );
            close.setOnAction( e -> stage.close() );

            root.getChildren().addAll(summaryText, close);

            Scene scene = new Scene(root, 400, 400);
            stage.setScene(scene);
            stage.show();
        });
	}
}
