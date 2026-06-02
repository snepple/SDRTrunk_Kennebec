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

import org.jdesktop.swingx.JXMapViewer;

import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import javafx.geometry.Point2D;
import javafx.geometry.Bounds;

/**
 * used to pan using the arrow keys
 * @author joshy
 */
public class PanKeyListener implements EventHandler<KeyEvent>
{
	private static final int OFFSET = 10;

	private JXMapViewer viewer;
	
	/**
	 * @param viewer the jxmapviewer
	 */
	public PanKeyListener(JXMapViewer viewer)
	{
		this.viewer = viewer;
	}

	@Override
	public void handle(KeyEvent e)
	{
		int delta_x = 0;
		int delta_y = 0;
		int requestedZoom = 0;

		switch ( e.getCode() )
		{
			case LEFT:
			case NUMPAD4:
				delta_x = -OFFSET;
				break;
			case RIGHT:
			case NUMPAD6:
				delta_x = OFFSET;
				break;
			case UP:
			case NUMPAD8:
				delta_y = -OFFSET;
				break;
			case DOWN:
			case NUMPAD2:
				delta_y = OFFSET;
				break;
			case MINUS:
			case SUBTRACT:
				requestedZoom = 1;
				break;
			case ADD:
			case EQUALS:
				requestedZoom = -1;
				break;
			default:
				break;
		}

		if (delta_x != 0 || delta_y != 0)
		{
			javafx.geometry.Rectangle2D bounds = viewer.getViewportBounds();
			double x = bounds.getMinX() + bounds.getWidth() / 2 + delta_x;
			double y = bounds.getMinY() + bounds.getHeight() / 2 + delta_y;
			viewer.setCenter(new javafx.geometry.Point2D(x, y));
			viewer.requestLayout();
		}
		
		if( requestedZoom != 0 )
		{
			int zoomLevel = viewer.getZoom() + requestedZoom;
			
			viewer.setZoom( zoomLevel );
		}
	}
}
