/*******************************************************************************
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
 ******************************************************************************/
package io.github.dsheirer.map;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;

import org.jdesktop.swingx.painter.Painter;



/**
 * Paints a selection rectangle
 * @author Martin Steiger
 */
public class SelectionPainter implements Painter<Object>
{
	private javafx.scene.paint.Color fillColor = javafx.scene.paint.Color.rgb(128, 192, 255, 128 / 255.0);
	private javafx.scene.paint.Color frameColor = javafx.scene.paint.Color.rgb(0, 0, 255, 128 / 255.0);

	private SelectionAdapter adapter;
	
	/**
	 * @param adapter the selection adapter
	 */
	public SelectionPainter(SelectionAdapter adapter)
	{
		this.adapter = adapter;
	}

	// // @Override
	public void paint(GraphicsContext g, Object t, int width, int height) {
        javafx.geometry.Rectangle2D rc = adapter.getRectangle();
        if (rc != null) {
            g.setStroke(frameColor);
            g.strokeRect(rc.getMinX(), rc.getMinY(), rc.getWidth(), rc.getHeight());
            g.setFill(fillColor);
            g.fillRect(rc.getMinX(), rc.getMinY(), rc.getWidth(), rc.getHeight());
        }
    }


}
