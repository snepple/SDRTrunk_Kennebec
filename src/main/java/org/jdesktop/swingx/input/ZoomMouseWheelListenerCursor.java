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
import javafx.geometry.Rectangle2D;
import javafx.geometry.Dimension2D;


import io.github.dsheirer.map.MapPanel;
import org.jdesktop.swingx.JXMapViewer;


import javafx.scene.input.ScrollEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;

/**
 * zooms to the current mouse cursor 
 * using the mouse wheel
 * @author Martin Steiger
 */
public class ZoomMouseWheelListenerCursor implements EventHandler<ScrollEvent>
{
	private MapPanel mMapPanel;
	private JXMapViewer mViewer;

	/**
	 * @param viewer the jxmapviewer
	 */
	public ZoomMouseWheelListenerCursor(MapPanel mapPanel)
	{
		mMapPanel = mapPanel;
		mViewer = mMapPanel.getMapViewer();
	}

	// // @Override
	public void handle(javafx.scene.input.ScrollEvent evt)
	{
		Point2D current = new Point2D(evt.getX(), evt.getY());
		Rectangle2D bound = mViewer.getViewportBounds();
		
		double dx = current.getX() - bound.getWidth() / 2;
		double dy = current.getY() - bound.getHeight() / 2;
		
		Dimension2D oldMapSize = mViewer.getTileFactory().getMapSize(mViewer.getZoom());

		mMapPanel.adjustZoom((int)-Math.signum(evt.getDeltaY()));

		Dimension2D mapSize = mViewer.getTileFactory().getMapSize(mViewer.getZoom());

		Point2D center = mViewer.getCenter();

		double dzw = (mapSize.getWidth() / oldMapSize.getWidth());
		double dzh = (mapSize.getHeight() / oldMapSize.getHeight());

		double x = center.getX() + dx * (dzw - 1);
		double y = center.getY() + dy * (dzh - 1);

		mViewer.setCenter(new Point2D(x, y));
	}
}
