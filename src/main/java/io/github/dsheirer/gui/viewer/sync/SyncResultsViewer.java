/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.gui.viewer.sync;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.stage.Stage;


import io.github.dsheirer.util.SwingUtils;
import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Utility for viewing sync detection results
 */
public class SyncResultsViewer implements ISyncResultsListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncResultsViewer.class.getName());
    private ISyncResultsListener mListener;

    public SyncResultsViewer()
    {
        initUI();
    }

    /**
     * View sync detection results.
     * @param symbols that were detected
     * @param sync that was detected
     * @param samples for the symbols
     * @param syncIntervals symbol timing interval pointers into the I/Q sample arrays
     * @param label to display to the user
     * @param release latch to release via the UI when done viewing these results
     */
    @Override
    public void receive(float[] symbols, float[] sync, float[] samples, float[] syncIntervals, float equalizerBalance,
                        float equalizerGain, String label, CountDownLatch release)
    {
        if(mListener != null)
        {
            mListener.receive(symbols, samples, sync, syncIntervals, equalizerBalance, equalizerGain, label, release);
        }
    }

    @Override
    public void symbol(float symbol)
    {
        if(mListener != null)
        {
            mListener.symbol(symbol);
        }
    }

    /**
     * Initializes the viewer UI
     */
    private void initUI()
    {
        CountDownLatch latch = new CountDownLatch(1);

        final javafx.scene.layout.Pane fxPanel = new javafx.scene.layout.Pane();
        Platform.runLater(() -> {
            Platform.setImplicitExit(false);
            SyncVisualizer syncVisualizer = new SyncVisualizer();
            mListener = syncVisualizer;

            Scene scene = new Scene(syncVisualizer, 1400, 1000);

//            URL resource = getClass().getResource("/sdrtrunk_style.css");
//
//            if(resource != null)
//            {
//                scene.getStylesheets().add(resource.toExternalForm());
//            }
//            else
//            {
//                LOGGER.warn("Can't find stylesheet resource for sdrtrunk");
//            }

            Stage frame = new Stage();
            frame.setScene(scene);
            frame.setTitle("Sync Results Viewer");
            // frame.setContentPane
            frame.setWidth(1400); frame.setHeight(1400);
            // frame.setDefaultCloseOperation
            frame.centerOnScreen();
            SwingUtils.run(() -> {
                frame.show();
                latch.countDown();
            });
        });

        try
        {
            latch.await();
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }
}