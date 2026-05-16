/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.channel.details;

import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.sample.Listener;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ChannelDetailPanel extends VBox implements Listener<ProcessingChain>
{
    private static final Logger mLog = LoggerFactory.getLogger(ChannelDetailPanel.class);
    private ChannelDetailViewController mController;

    public ChannelDetailPanel(ChannelProcessingManager channelProcessingManager)
    {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ChannelDetailView.fxml"));
            loader.setRoot(this);
            loader.load();
            mController = loader.getController();
            mController.setChannelProcessingManager(channelProcessingManager);
        } catch (IOException e) {
            mLog.error("Error loading ChannelDetailView.fxml", e);
        }
    }

    @Override
    public void receive(ProcessingChain processingChain)
    {
        if (mController != null) {
            mController.receive(processingChain);
        }
    }
}
