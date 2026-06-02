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
package org.jdesktop.swingx.input;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;


import org.jdesktop.swingx.JXMapViewer;



import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Point2D;

/**
 * Centers the map on the mouse cursor
 * if left is double-clicked or middle mouse
 * button is pressed.
 * @author Martin Steiger
 * @author joshy
 */
public class CenterMapListener implements EventHandler<MouseEvent>
{
	private JXMapViewer viewer;
	
	/**
	 * @param viewer the jxmapviewer
	 */
	public CenterMapListener(JXMapViewer viewer)
	{
		this.viewer = viewer;
	}

	// // @Override
	public void handle(MouseEvent evt)
	{
		boolean left = evt.isPrimaryButtonDown() /* TODO */ /* TODO */;
		boolean middle = evt.isMiddleButtonDown();
		boolean doubleClick = (evt.getClickCount() == 2);

		if (middle || (left && doubleClick))
		{
			recenterMap(evt);
		}
	}
	
	private void recenterMap(MouseEvent evt)
	{
		Rectangle2D bounds = viewer.getViewportBounds();
		double x = bounds.getMinX() + evt.getX();
		double y = bounds.getMinY() + evt.getY();
		viewer.setCenter(new Point2D(x, y));
                viewer.setZoom(viewer.getZoom() - 1);
		viewer.requestLayout();
	}
}


