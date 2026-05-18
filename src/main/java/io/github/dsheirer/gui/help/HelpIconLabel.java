package io.github.dsheirer.gui.help;

import jiconfont.javafx.IconNode;
import jiconfont.icons.font_awesome.FontAwesome;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class HelpIconLabel extends Label {
    public HelpIconLabel(String htmlHelpText) {
        super();

        IconNode iconNode = new IconNode(FontAwesome.INFO_CIRCLE);
        iconNode.setIconSize(14);
        iconNode.setFill(Color.GRAY);

        setGraphic(iconNode);

        // JavaFX Tooltip doesn't support HTML natively like Swing did,
        // so we might need to strip or adapt basic HTML tags, but for simple use cases
        // setting the raw text or stripped text is needed.
        // We'll replace <br> with newlines and strip remaining basic tags if any.
        String plainText = htmlHelpText;
        if (plainText != null) {
            plainText = plainText.replaceAll("(?i)<br\\s*/?>", "\n");
            plainText = plainText.replaceAll("<[^>]+>", ""); // strip other tags like <html>
        }

        Tooltip tooltip = new Tooltip(plainText);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(400); // Reasonable max width for tooltips
        setTooltip(tooltip);
    }
}
