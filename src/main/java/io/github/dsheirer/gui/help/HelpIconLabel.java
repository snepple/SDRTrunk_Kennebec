package io.github.dsheirer.gui.help;

import jiconfont.javafx.IconNode;
import jiconfont.icons.font_awesome.FontAwesome;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;



public class HelpIconLabel extends Label {
    public HelpIconLabel(String htmlHelpText) {
        IconNode iconNode = new IconNode(FontAwesome.INFO_CIRCLE);
        iconNode.setIconSize(14);
        iconNode.setFill(Color.GRAY);
        setGraphic(iconNode);

        // Remove <html> wrapper if present as JavaFX Tooltip doesn't process html directly, but supports multiline text
        String tooltipText = htmlHelpText.replace("<html>", "").replace("</html>", "").replace("<b>", "").replace("</b>", "").replace("<br>", "\n");
        setTooltip(new Tooltip(tooltipText));
    }
}
