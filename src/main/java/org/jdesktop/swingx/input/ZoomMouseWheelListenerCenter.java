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

import org.jdesktop.swingx.JXMapViewer;

import javafx.scene.input.ScrollEvent;
import javafx.event.EventHandler;

/**
 * zooms using the mouse wheel on the view center
 * @author joshy
 */
public class ZoomMouseWheelListenerCenter implements EventHandler<ScrollEvent>
{
	private JXMapViewer viewer;
	
	/**
	 * @param viewer the jxmapviewer
	 */
	public ZoomMouseWheelListenerCenter(JXMapViewer viewer)
	{
		this.viewer = viewer;
	}

	@Override
	public void handle(ScrollEvent e)
	{
		viewer.setZoom(viewer.getZoom() + (int)(-e.getDeltaY() / 40.0));
	}
}
