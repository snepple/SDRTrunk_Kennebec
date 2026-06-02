

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
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.application.Platform;
import javafx.scene.control.ContextMenu;

import javafx.application.Platform;
import javafx.scene.control.Button;

import io.github.dsheirer.settings.SettingsManager;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;



import javafx.geometry.Rectangle2D;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;

public class MapMouseListener 
{
	private JXMapViewer mJXMapViewer;
	private SettingsManager mSettingsManager;
	private Point2D mPreviousPoint;
	private Point2D mCurrentPoint;

	public MapMouseListener( JXMapViewer viewer, SettingsManager settingsManager )
	{
		mJXMapViewer = viewer;
		mSettingsManager = settingsManager;
	}
	
	// // @Override
	public void mouseDragged( MouseEvent event )
	{
		if ( !event.isPrimaryButtonDown() )
		{
			return;
		}

		Point2D current = new Point2D(event.getX(), event.getY());
		
		double x = mJXMapViewer.getCenter().getX() - 
				   ( current.getX() - mPreviousPoint.getX() );

		double y = mJXMapViewer.getCenter().getY() - 
				   (current.getY() - mPreviousPoint.getY() );

		if ( !mJXMapViewer.isNegativeYAllowed() )
		{
			if ( y < 0 )
			{
				y = 0;
			}
		}

		int maxHeight = (int)( mJXMapViewer.getTileFactory()
				.getMapSize( mJXMapViewer.getZoom() ).getHeight() * 
				mJXMapViewer.getTileFactory()
				.getTileSize( mJXMapViewer.getZoom() ) );
		
		if (y > maxHeight)
		{
			y = maxHeight;
		}

		mPreviousPoint = current;
		
		mJXMapViewer.setCenter( new Point2D( x, y ) );
		
		mJXMapViewer.repaint();

		/* Set cursor to dragging */
		mJXMapViewer.setCursor( javafx.scene.Cursor.MOVE );
	}

	// // @Override
	public void mouseReleased( MouseEvent event )
	{
		if ( !event.isPrimaryButtonDown() )
		{
			return;
		}

		mPreviousPoint = null;
		
		/* Reset the curson */
		mJXMapViewer.setCursor( 
				javafx.scene.Cursor.DEFAULT );
	}

	// // @Override
	public void mouseEntered( MouseEvent event )
	{
		Platform.runLater( new Runnable()
		{
			@Override
			public void run()
			{
				mJXMapViewer.requestFocus();
			}
		});
	}

	// // @Override
	public void mousePressed( MouseEvent event )
	{
		mCurrentPoint = new Point2D(event.getX(), event.getY());
		mPreviousPoint = new Point2D(event.getX(), event.getY());

		boolean left = event.isPrimaryButtonDown();
		
		boolean middle = event.isMiddleButtonDown();
		
		boolean right = event.isSecondaryButtonDown();

		boolean doubleClick = ( event.getClickCount() == 2 );

		if (middle || ( left && doubleClick ) )
		{
			recenterMap( event );
		}
		else if( right )
		{
			ContextMenu popup = new ContextMenu();
			javafx.scene.control.MenuItem mapViewItem = new javafx.scene.control.MenuItem( "Set Default Location & Zoom" );
			mapViewItem.setOnAction(arg0 -> {
				GeoPosition position = mJXMapViewer.convertPointToGeoPosition( mCurrentPoint );
				mSettingsManager.getSettingsModel().setMapViewSetting( "Default", position, mJXMapViewer.getZoom() );
			});
			popup.getItems().add(mapViewItem);
			popup.show( mJXMapViewer, event.getScreenX(), event.getScreenY() );
		}
	}
	
	private void recenterMap( MouseEvent event )
	{
		Rectangle2D bounds = mJXMapViewer.getViewportBounds();
		
		double x = bounds.getMinX() + event.getX();
		
		double y = bounds.getMinY() + event.getY();
		
		mJXMapViewer.setCenter( new Point2D( x, y ) );

		mJXMapViewer.setZoom( mJXMapViewer.getZoom() - 1 );

		mJXMapViewer.repaint();
	}
}

