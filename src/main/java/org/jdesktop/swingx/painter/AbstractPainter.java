package org.jdesktop.swingx.painter;

import javafx.scene.canvas.GraphicsContext;

public abstract class AbstractPainter<T> implements Painter<T> {
    private boolean antialiasing = true;
    private boolean visible = true;
    
    public void setAntialiasing(boolean antialiasing) {
        this.antialiasing = antialiasing;
    }
    public boolean isAntialiasing() {
        return antialiasing;
    }
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    public boolean isVisible() {
        return visible;
    }
    public void setCacheable(boolean cacheable) {
        // Ignored for JavaFX
    }
    
    protected abstract void doPaint(GraphicsContext g, T object, int width, int height);
    
    @Override
    public void paint(GraphicsContext g, T object, int width, int height) {
        if (visible) {
            doPaint(g, object, width, height);
        }
    }
}
