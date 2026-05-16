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

import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.config.SourceConfiguration;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.slf4j.LoggerFactory;

public class ChannelDetailPanel extends JFXPanel implements Listener<ProcessingChain>
{
    private ChannelProcessingManager mChannelProcessingManager;
    private ChannelDetailPanelController mController;

    public ChannelDetailPanel(ChannelProcessingManager channelProcessingManager)
    {
        mChannelProcessingManager = channelProcessingManager;
        init();
    }

    private void init()
    {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/channel/details/ChannelDetailPanel.fxml"));
                Parent root = loader.load();

                mController = loader.getController();
                mController.setChannelProcessingManager(mChannelProcessingManager);

                Scene scene = new Scene(root);
                java.net.URL cssUrl = getClass().getResource("/sdrtrunk_style.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                }

                setScene(scene);
            } catch (Exception e) {
                LoggerFactory.getLogger(ChannelDetailPanel.class).error("Error loading ChannelDetailPanel FXML", e);
            }
        });
    }

    @Override
    public void receive(ProcessingChain processingChain)
    {
        if (mController != null) {
            mController.receive(processingChain);
        } else {
            // Queue receive until controller is loaded
            Platform.runLater(() -> {
                if (mController != null) {
                    mController.receive(processingChain);
                }
            });
        }
    }
}
