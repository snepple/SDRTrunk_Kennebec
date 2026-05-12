package io.github.dsheirer.gui;

import io.github.dsheirer.gui.sidebar.SidebarJFXPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JPanel;

public class SidebarPanel extends JPanel {
    private SidebarJFXPanel jfxPanel;

    public interface SidebarListener {
        void onItemSelected(String id);
        void onActionRequested(String actionId);
    }

    public SidebarPanel(SidebarListener listener) {
        setLayout(new BorderLayout());
        jfxPanel = new SidebarJFXPanel(listener);
        add(jfxPanel, BorderLayout.CENTER);
        setPreferredSize(new Dimension(250, 0));
    }

    public void setActive(String id) {
        if (jfxPanel != null) {
            jfxPanel.setActive(id);
        }
    }
}
