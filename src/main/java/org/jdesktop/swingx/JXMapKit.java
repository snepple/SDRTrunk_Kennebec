package org.jdesktop.swingx;
import java.awt.Dimension;


import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.painter.Painter;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class JXMapKit extends Pane {
    private JXMapViewer mainMap;
    private JXMapViewer miniMap;
    private Button zoomInButton;
    private Button zoomOutButton;
    private Slider zoomSlider;
    private boolean zoomSliderUpdating = false;

    public JXMapKit() {
        mainMap = new JXMapViewer();
        miniMap = new JXMapViewer();
        zoomInButton = new Button("+");
        zoomOutButton = new Button("-");
        zoomSlider = new Slider();

        zoomSlider.setOrientation(Orientation.VERTICAL);
        zoomSlider.setShowTickMarks(true);
        zoomSlider.setSnapToTicks(true);
        
        /* mainMap.addPropertyChangeListener("zoom", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                zoomSliderUpdating = true;
                zoomSlider.setValue((Integer) evt.getNewValue());
                zoomSliderUpdating = false;
            }
        }); */

        zoomSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
                if (!zoomSliderUpdating) {
                    mainMap.setZoom(new_val.intValue());
                }
            }
        });

        zoomInButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                mainMap.setZoom(mainMap.getZoom() - 1);
            }
        });

        zoomOutButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                mainMap.setZoom(mainMap.getZoom() + 1);
            }
        });

        VBox controls = new VBox(5, zoomInButton, zoomSlider, zoomOutButton);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(10));
        controls.setMaxWidth(40);
        
        StackPane mapStack = new StackPane();
        mapStack.getChildren().add(mainMap);
        
        // Setup layout
        GridPane grid = new GridPane();
        grid.add(mapStack, 0, 0);
        grid.add(controls, 1, 0);
        getChildren().add(grid);
    }
    
    public JXMapViewer getMainMap() {
        return mainMap;
    }
    
    public JXMapViewer getMiniMap() {
        return miniMap;
    }

    public Slider getZoomSlider() {
        return zoomSlider;
    }

    public Button getZoomInButton() {
        return zoomInButton;
    }

    public Button getZoomOutButton() {
        return zoomOutButton;
    }
    
    public void setDefaultProvider(DefaultProviders prov) {
        // Dummy implementation
    }

    public enum DefaultProviders {
        OpenStreetMaps,
        Custom
    }
    
    public void setZoom(int zoom) {
        mainMap.setZoom(zoom);
    }
    
    public void setAddressLocation(GeoPosition pos) {
        mainMap.setAddressLocation(pos);
    }
    
    public void setCenterPosition(GeoPosition pos) {
        mainMap.setCenterPosition(pos);
    }
}
