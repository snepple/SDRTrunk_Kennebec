
package io.github.dsheirer.map;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.control.Label;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.icon.IconModel;

import javafx.scene.text.Font;

import javafx.embed.swing.SwingFXUtils;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;



import java.util.Collections;
import java.util.List;

public class PlottableEntityRenderer {
    private AliasModel mAliasModel;
    private IconModel mIconModel;
    private int mTrackHistoryLength = 3;

    public PlottableEntityRenderer(AliasModel aliasModel, IconModel iconModel) {
        mAliasModel = aliasModel;
        mIconModel = iconModel;
    }

    public void setTrackHistoryLength(int length) {
        mTrackHistoryLength = length;
    }

    public int getTrackHistoryLength() {
        return mTrackHistoryLength;
    }

    public void paintPlottableEntity(GraphicsContext gc, JXMapViewer viewer, PlottableEntityHistory entity, boolean antiAliasing) {
        List<TimestampedGeoPosition> locationHistory = entity.getTrackHistoryModel().getTrackHistory();

        if (!locationHistory.isEmpty() && locationHistory.get(0).isValid()) {
            List<Alias> aliases = getAliases(entity);
            Alias alias = aliases.isEmpty() ? null : aliases.get(0);

            javafx.scene.paint.Color awtColor = (alias != null ? alias.getDisplayColor() : javafx.scene.paint.Color.BLUE);
            Color color = Color.rgb((int)(awtColor.getRed() * 255), (int)(awtColor.getGreen() * 255), (int)(awtColor.getBlue() * 255), awtColor.getOpacity());

            paintRoute(gc, viewer, locationHistory, color);

            Point2D point = viewer.getTileFactory().geoToPixel(entity.getLatestPosition(), viewer.getZoom());

            Image icon = getIcon(alias);
            Image fxIcon = icon;
            
            if (fxIcon != null) {
                paintIcon(gc, point, fxIcon);
                String label = (alias != null ? alias.getName() : entity.getIdentifier().toString());
                paintLabel(gc, point, label, (int)(fxIcon.getWidth() / 2.0), 0, color);
            }
        }
    }

    private List<Alias> getAliases(PlottableEntityHistory entityHistory) {
        AliasList aliasList = mAliasModel.getAliasList(entityHistory.getIdentifierCollection());
        if (aliasList != null) {
            return aliasList.getAliases(entityHistory.getIdentifier());
        }
        return Collections.emptyList();
    }

    private javafx.scene.image.Image getIcon(Alias alias) {
        String iconName = (alias != null ? alias.getIconName() : null);
        return mIconModel.getIcon(iconName, io.github.dsheirer.icon.IconModel.DEFAULT_ICON_SIZE);
    }

    

    private void paintIcon(GraphicsContext gc, Point2D point, Image icon) {
        gc.drawImage(icon, point.getX() - (icon.getWidth() / 2.0), point.getY() - (icon.getHeight() / 2.0));
    }

    private void paintLabel(GraphicsContext gc, Point2D point, String label, int xOffset, int yOffset, Color color) {
        gc.setFill(color);
        gc.setFont(Font.font(12));
        gc.fillText(label, point.getX() + xOffset, point.getY() + yOffset);
    }

    private void paintRoute(GraphicsContext gc, JXMapViewer viewer, List<TimestampedGeoPosition> locations, Color color) {
        if (!locations.isEmpty()) {
            // Draw background line
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(3);
            drawRoute(locations, gc, viewer);

            // Draw foreground line
            gc.setStroke(color);
            gc.setLineWidth(1);
            drawRoute(locations, gc, viewer);
        }
    }

    private void drawRoute(List<TimestampedGeoPosition> locations, GraphicsContext gc, JXMapViewer viewer) {
        Point2D previousPoint = null;
        int length = Math.min(locations.size(), mTrackHistoryLength);

        gc.beginPath();
        for (int x = 0; x < length; x++) {
            GeoPosition location = locations.get(x);
            Point2D currentPoint = viewer.getTileFactory().geoToPixel(location, viewer.getZoom());

            if (previousPoint != null) {
                gc.lineTo(currentPoint.getX(), currentPoint.getY());
            } else {
                gc.moveTo(currentPoint.getX(), currentPoint.getY());
            }
            previousPoint = currentPoint;
        }
        gc.stroke();
    }
}
