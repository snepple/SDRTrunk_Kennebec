package io.github.dsheirer.gui.playlist.channel;

import io.github.dsheirer.controller.channel.Channel;
import javax.swing.JPanel;
import javax.swing.JLabel;
import java.awt.BorderLayout;

public class TetraConfigPanel extends JPanel {
    private Channel mChannel;

    public TetraConfigPanel(Channel channel) {
        mChannel = channel;
        setLayout(new BorderLayout());
        add(new JLabel("TETRA Configuration (Placeholder)"), BorderLayout.CENTER);
    }
}
