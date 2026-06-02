
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
import javafx.scene.control.Button;

import org.apache.commons.math3.util.FastMath;
import org.jdesktop.swingx.JXMapViewer;

import java.awt.Rectangle;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Point2D;

/**
 * Creates a selection rectangle based on mouse input
 * Also triggers repaint events in the viewer
 * @author Martin Steiger
 */
public class SelectionAdapter
{
	private boolean dragging;
	private JXMapViewer viewer;

	private Point2D startPos = new Point2D(0, 0);
	private Point2D endPos = new Point2D(0, 0);

	/**
	 * @param viewer the jxmapviewer
	 */
	public SelectionAdapter(JXMapViewer viewer)
	{
		this.viewer = viewer;
	}

	public void mousePressed(MouseEvent e)
	{
		if (!e.isSecondaryButtonDown())
			return;
		
		startPos = new Point2D(e.getX(), e.getY());
		endPos = new Point2D(e.getX(), e.getY());
		
		dragging = true;
	}

	public void mouseDragged(MouseEvent e)
	{
		if (!dragging)
			return;
		
		endPos = new Point2D(e.getX(), e.getY());
		
		viewer.repaint();
	}

	public void mouseReleased(MouseEvent e)
	{
		if (!dragging)
			return;
		
		if (!e.isSecondaryButtonDown())
			return;
		
		viewer.repaint();
		
		dragging = false;
	}

	/**
	 * @return the selection rectangle
	 */
	public javafx.geometry.Rectangle2D getRectangle()
	{
		if (dragging)
		{
			double x1 = FastMath.min(startPos.getX(), endPos.getX());
			double y1 = FastMath.min(startPos.getY(), endPos.getY());
			double x2 = FastMath.max(startPos.getX(), endPos.getX());
			double y2 = FastMath.max(startPos.getY(), endPos.getY());
			
			return new javafx.geometry.Rectangle2D(x1, y1, x2-x1, y2-y1);
		}
		
		return null;
	}

}

