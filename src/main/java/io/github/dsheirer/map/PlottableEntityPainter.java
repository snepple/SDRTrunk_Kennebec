package io.github.dsheirer.map;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.icon.IconModel;
import javafx.scene.canvas.GraphicsContext;
import org.jdesktop.swingx.JXMapViewer;

import javafx.geometry.Rectangle2D;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class PlottableEntityPainter {
    private PlottableEntityRenderer mRenderer;
    private Set<PlottableEntityHistory> mEntities = new CopyOnWriteArraySet<>();

    public PlottableEntityPainter(AliasModel aliasModel, IconModel iconModel) {
        mRenderer = new PlottableEntityRenderer(aliasModel, iconModel);
    }

    public void setTrackHistoryLength(int length) {
        mRenderer.setTrackHistoryLength(length);
    }

    public int getTrackHistoryLength() {
        return mRenderer.getTrackHistoryLength();
    }

    public boolean addEntity(PlottableEntityHistory entity) {
        if (entity != null && !mEntities.contains(entity)) {
            mEntities.add(entity);
            return true;
        }
        return false;
    }

    public boolean addAll(List<PlottableEntityHistory> entities) {
        boolean added = false;
        for (PlottableEntityHistory entity : entities) {
            added |= addEntity(entity);
        }
        return added;
    }

    public void removeEntity(PlottableEntityHistory entity) {
        mEntities.remove(entity);
    }

    public void clearAllEntities() {
        mEntities.clear();
    }

    public void clearEntities(List<PlottableEntityHistory> toDelete) {
        mEntities.removeAll(toDelete);
    }

    public void paint(GraphicsContext gc, JXMapViewer map, int width, int height) {
        Rectangle2D viewportBounds = map.getViewportBounds();
        gc.save();
        gc.translate(-viewportBounds.getMinX(), -viewportBounds.getMinY());

        for (PlottableEntityHistory entity : mEntities) {
            mRenderer.paintPlottableEntity(gc, map, entity, true);
        }

        gc.restore();
    }
}
