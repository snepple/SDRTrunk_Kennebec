/*
 * WaypointRenderer.java
 *
 * Created on March 30, 2006, 5:24 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.mapviewer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;

import org.jdesktop.swingx.JXMapViewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;

/**
 * This is a standard waypoint renderer.
 * @author joshy
 */
public class DefaultWaypointRenderer implements WaypointRenderer<Waypoint>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( DefaultWaypointRenderer.class );

	private Image img = null;

	/**
	 * Uses a default waypoint image
	 */
	public DefaultWaypointRenderer()
	{
		try
		{
			img = new Image(getClass().getResource("resources/standard_waypoint.png").toExternalForm());
		}
		catch (Exception ex)
		{
			mLog.error("couldn't read standard_waypoint.png", ex );
		}
	}

	@Override
	public void paintWaypoint(GraphicsContext g, JXMapViewer map, Waypoint w)
	{
		if (img == null)
			return;

		Point2D point = map.getTileFactory().geoToPixel(w.getPosition(), map.getZoom());
		
		int x = (int)point.getX() - (int)img.getWidth() / 2;
		int y = (int)point.getY() - (int)img.getHeight();
		
		g.drawImage(img, x, y);
	}
}
