package org.jdesktop.swingx.painter;

import javafx.scene.canvas.GraphicsContext;
import java.util.ArrayList;
import java.util.List;

public class CompoundPainter<T> extends AbstractPainter<T> {
    private List<Painter<T>> painters = new ArrayList<>();
    
    public CompoundPainter(Painter<T>... painters) {
        if (painters != null) {
            for (Painter<T> p : painters) {
                this.painters.add(p);
            }
        }
    }
    
    public List<Painter<T>> getPainters() {
        return painters;
    }
    
    public void setPainters(Painter<T>... painters) {
        this.painters.clear();
        if (painters != null) {
            for (Painter<T> p : painters) {
                this.painters.add(p);
            }
        }
    }

    @Override
    protected void doPaint(GraphicsContext g, T object, int width, int height) {
        for (Painter<T> p : painters) {
            g.save();
            p.paint(g, object, width, height);
            g.restore();
        }
    }
}
