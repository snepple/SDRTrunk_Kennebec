
package io.github.dsheirer.icon;

import jiconfont.IconCode;
import jiconfont.swing.IconFontSwing;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

public class MyFontIcon implements Icon {
    private IconCode iconCode;
    private float size;
    private Color color;
    private Icon cachedIcon;
    private Color lastColor;

    private static final int RENDER_SCALE = 2; // Reduce to avoid getting too big for IconFontSwing

    public MyFontIcon(IconCode iconCode, float size, Color color) {
        this.iconCode = iconCode;
        this.size = size;
        this.color = color;
    }

    private Icon getHiResIcon(Color c) {
        if (cachedIcon == null || !c.equals(lastColor)) {
            cachedIcon = IconFontSwing.buildIcon(iconCode, size * RENDER_SCALE, c);
            lastColor = c;
        }
        return cachedIcon;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Color renderColor = this.color != null ? this.color : (c != null ? c.getForeground() : Color.BLACK);
            Icon hiRes = getHiResIcon(renderColor);

            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Graphics2D g3 = (Graphics2D) g2.create();
            g3.translate(x, y);
            g3.scale(1.0 / RENDER_SCALE, 1.0 / RENDER_SCALE);
            hiRes.paintIcon(c, g3, 0, 0);
            g3.dispose();
        } finally {
            g2.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return Math.round(size);
    }

    @Override
    public int getIconHeight() {
        return Math.round(size);
    }
}
