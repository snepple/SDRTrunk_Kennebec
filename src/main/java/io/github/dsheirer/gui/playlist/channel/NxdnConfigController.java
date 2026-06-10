package io.github.dsheirer.gui.playlist.channel;

import io.github.dsheirer.controller.channel.Channel;
import javafx.fxml.FXML;

public class NxdnConfigController {
    private Channel mChannel;

    public void setChannel(Channel channel) {
        this.mChannel = channel;
    }

    @FXML
    public void initialize() {
        // Initialization logic
    }
}
