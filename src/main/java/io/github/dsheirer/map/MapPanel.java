


package io.github.dsheirer.map;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.settings.MapViewSetting;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.gui.control.ToggleSwitch;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;

import javafx.scene.control.*;
import javafx.scene.layout.*;

import javafx.embed.swing.SwingNode;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.OSMTileFactoryInfo;
import org.jdesktop.swingx.input.PanKeyListener;
import org.jdesktop.swingx.input.ZoomMouseWheelListenerCursor;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.KeyEvent;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MapPanel extends BorderPane implements IPlottableUpdateListener {

    private static final int ZOOM_MINIMUM = 1;
    private static final int ZOOM_MAXIMUM = 16;

    private static final String FOLLOW = "Follow";
    private static final String UNFOLLOW = "Unfollow";
    private static final String SELECT_A_TRACK = "(select a track)";
    private static final String NO_SYSTEM_NAME = "(no system name)";
    private SettingsManager mSettingsManager;
    private MapService mMapService;
    private JXMapViewer mMapViewer;
    private Canvas mMapCanvas;
    private PlottableEntityPainter mMapPainter;
    private SelectionAdapter mSelectionAdapter;
    private SelectionPainter mSelectionPainter;
    private TrackGenerator mTrackGenerator;
    private ToggleSwitch mTrackGeneratorToggle;
    private TableView<PlottableEntityHistory> mPlottedTracksTable;
    private Button mClearMapButton;
    private Button mReplotAllTracksButton;
    private Button mDeleteAllTracksButton;
    private Button mDeleteTrackButton;
    private Button mFollowButton;
    private Label mFollowedEntityLabel;
    private CheckBox mCenterOnSelectedCheckBox;
    private PlottableEntityHistory mFollowedTrack;
    private ComboBox<Integer> mTrackHistoryLengthComboBox;
    private TableView<TimestampedGeoPosition> mTrackHistoryTable;
    private Label mSelectedTrackSystemLabel;

    public MapPanel(MapService mapService, AliasModel aliasModel, IconModel iconModel, SettingsManager settingsManager) {
        mSettingsManager = settingsManager;
        mMapService = mapService;
        mMapPainter = new PlottableEntityPainter(aliasModel, iconModel);

        init();
    }

    private void init() {
        mMapService.addListener(this);

        // Sidebar
        VBox sidebar = new VBox(0);
        sidebar.setStyle("-fx-background-color: #f2f2f7;");

        Label header1 = new Label("Plotted Tracks");
        header1.setStyle("-fx-font-weight: bold; -fx-text-fill: #8e8e93; -fx-padding: 10 10 5 5;");
        sidebar.getChildren().add(header1);

        TableView<PlottableEntityHistory> tracksTable = getPlottedTracksTable();
        VBox.setVgrow(tracksTable, Priority.ALWAYS);
        sidebar.getChildren().add(tracksTable);

        VBox detailPanel = new VBox(5);
        detailPanel.setStyle("-fx-background-color: white; -fx-border-color: #c8c8c8 transparent transparent transparent; -fx-padding: 10;");

        HBox systemBox = new HBox(5, new Label("Selected System:"), getSelectedTrackSystemLabel());
        detailPanel.getChildren().add(systemBox);

        Label header2 = new Label("Track History");
        header2.setStyle("-fx-font-weight: bold; -fx-text-fill: #8e8e93;");
        detailPanel.getChildren().add(header2);

        TableView<TimestampedGeoPosition> historyTable = getTrackHistoryTable();
        VBox.setVgrow(historyTable, Priority.ALWAYS);
        detailPanel.getChildren().add(historyTable);

        GridPane settingsPanel = new GridPane();
        settingsPanel.setHgap(10);
        settingsPanel.setVgap(5);
        settingsPanel.add(new Label("History Length:"), 0, 0);
        settingsPanel.add(getTrackHistoryLengthComboBox(), 1, 0);
        settingsPanel.add(getCenterOnSelectedCheckBox(), 0, 1, 2, 1);
        settingsPanel.add(getTrackGeneratorToggle(), 0, 2, 2, 1);
        detailPanel.getChildren().add(settingsPanel);

        sidebar.getChildren().add(detailPanel);

        // Map Area
        StackPane mapArea = new StackPane();
        
        
        
        mMapCanvas = new Canvas();
        // Bind canvas size to map area size
        mMapCanvas.widthProperty().bind(mapArea.widthProperty());
        mMapCanvas.heightProperty().bind(mapArea.heightProperty());
        mMapCanvas.setMouseTransparent(true);
        mMapCanvas.widthProperty().addListener(e -> repaintCanvas());
        mMapCanvas.heightProperty().addListener(e -> repaintCanvas());

        mapArea.getChildren().addAll(getMapViewer(), mMapCanvas);

        VBox floatingControls = new VBox(8);
        floatingControls.setPadding(new Insets(8));
        floatingControls.setStyle("-fx-background-color: rgba(255, 255, 255, 0.86); -fx-background-radius: 16; -fx-border-color: rgba(0,0,0,0.15); -fx-border-radius: 16;");
        floatingControls.setMaxWidth(40);
        floatingControls.setMaxHeight(180);
        StackPane.setAlignment(floatingControls, Pos.TOP_RIGHT);
        StackPane.setMargin(floatingControls, new Insets(20));

        Button btnZoomIn = createFloatingButton("+", "Zoom In");
        btnZoomIn.setOnAction(e -> adjustZoom(-1));

        Button btnZoomOut = createFloatingButton("-", "Zoom Out");
        btnZoomOut.setOnAction(e -> adjustZoom(1));

        Button btnFollow = createFloatingButton("F", "Follow Selected");
        btnFollow.setOnAction(e -> {
            if (mFollowedTrack == null) {
                follow(getSelected());
            } else {
                follow(null);
            }
        });

        MenuButton btnOptions = new MenuButton("Opts");
        btnOptions.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #007aff;");
        MenuItem clearItem = new MenuItem("Clear Map");
        clearItem.setOnAction(e -> getClearMapButton().fire());
        MenuItem deleteItem = new MenuItem("Delete Selected");
        deleteItem.setOnAction(e -> getDeleteTrackButton().fire());
        MenuItem deleteAllItem = new MenuItem("Delete All");
        deleteAllItem.setOnAction(e -> getDeleteAllTracksButton().fire());
        MenuItem replotItem = new MenuItem("Replot All");
        replotItem.setOnAction(e -> getReplotAllTracksButton().fire());

        btnOptions.getItems().addAll(clearItem, deleteItem, deleteAllItem, new SeparatorMenuItem(), replotItem);

        floatingControls.getChildren().addAll(btnZoomIn, btnZoomOut, new Separator(), btnFollow, btnOptions);
        mapArea.getChildren().add(floatingControls);

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(sidebar, mapArea);
        splitPane.setDividerPositions(0.3);

        this.setCenter(splitPane);

        getFollowButton();
        getFollowedEntityLabel();
        getDeleteTrackButton();
    }

    private Button createFloatingButton(String text, String tooltip) {
        Button btn = new Button(text);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #007aff; -fx-font-weight: bold;");
        return btn;
    }

    private void repaintCanvas() {
        if (mMapCanvas == null || mMapViewer == null) return;
        GraphicsContext gc = mMapCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, mMapCanvas.getWidth(), mMapCanvas.getHeight());
        mMapPainter.paint(gc, mMapViewer, (int)mMapCanvas.getWidth(), (int)mMapCanvas.getHeight());
        if (mSelectionPainter != null) {
            mSelectionPainter.paint(gc, mMapViewer, (int)mMapCanvas.getWidth(), (int)mMapCanvas.getHeight());
        }
    }

    private void setSelected(PlottableEntityHistory selected) {
        if (selected != null) {
            getTrackHistoryTable().setItems(selected.getTrackHistoryModel().getTrackHistory());

            Identifier system = selected.getIdentifierCollection().getIdentifier(IdentifierClass.CONFIGURATION, Form.SYSTEM, Role.ANY);

            if (system != null) {
                getSelectedTrackSystemLabel().setText(system.toString());
            } else {
                getSelectedTrackSystemLabel().setText(NO_SYSTEM_NAME);
            }

            if (mMapPainter.addEntity(getSelected())) {
                repaintCanvas();
            }
        } else {
            getTrackHistoryTable().setItems(FXCollections.emptyObservableList());
            getSelectedTrackSystemLabel().setText(SELECT_A_TRACK);
        }

        if (getCenterOnSelectedCheckBox().isSelected()) {
            centerOn(selected);
        }
    }

    private Button getReplotAllTracksButton() {
        if (mReplotAllTracksButton == null) {
            mReplotAllTracksButton = new Button("Replot All");
            mReplotAllTracksButton.setOnAction(e -> {
                boolean added = mMapPainter.addAll(mMapService.getPlottableEntityModel().getItems());
                if (added) {
                    repaintCanvas();
                }
            });
        }
        return mReplotAllTracksButton;
    }

    private Label getSelectedTrackSystemLabel() {
        if (mSelectedTrackSystemLabel == null) {
            mSelectedTrackSystemLabel = new Label(SELECT_A_TRACK);
        }
        return mSelectedTrackSystemLabel;
    }

    private TableView<TimestampedGeoPosition> getTrackHistoryTable() {
        if (mTrackHistoryTable == null) {
            mTrackHistoryTable = new TableView<>();
            
            TableColumn<TimestampedGeoPosition, String> timeCol = new TableColumn<>("Time");
            timeCol.setCellValueFactory(data -> new SimpleStringProperty(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(data.getValue().getTimestamp())));
            
            TableColumn<TimestampedGeoPosition, String> latCol = new TableColumn<>("Latitude");
            latCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.5f", data.getValue().getLatitude())));
            
            TableColumn<TimestampedGeoPosition, String> lonCol = new TableColumn<>("Longitude");
            lonCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.5f", data.getValue().getLongitude())));

            mTrackHistoryTable.getColumns().addAll(timeCol, latCol, lonCol);
            
            mTrackHistoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && getCenterOnSelectedCheckBox().isSelected()) {
                    Platform.runLater(() -> {
                        mMapViewer.setCenterPosition(newVal);
                    });
                }
            });
        }
        return mTrackHistoryTable;
    }

    private ComboBox<Integer> getTrackHistoryLengthComboBox() {
        if (mTrackHistoryLengthComboBox == null) {
            mTrackHistoryLengthComboBox = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
            mTrackHistoryLengthComboBox.getSelectionModel().select(Integer.valueOf(mMapPainter.getTrackHistoryLength()));

            mTrackHistoryLengthComboBox.setOnAction(e -> {
                Integer length = mTrackHistoryLengthComboBox.getSelectionModel().getSelectedItem();
                if (length != null) {
                    mMapPainter.setTrackHistoryLength(length);
                }
            });
        }
        return mTrackHistoryLengthComboBox;
    }

    private Label getFollowedEntityLabel() {
        if (mFollowedEntityLabel == null) {
            mFollowedEntityLabel = new Label(" ");
        }
        return mFollowedEntityLabel;
    }

    private Button getFollowButton() {
        if (mFollowButton == null) {
            mFollowButton = new Button(FOLLOW);
            mFollowButton.setDisable(true);
            mFollowButton.setOnAction(e -> {
                if (FOLLOW.equals(mFollowButton.getText())) {
                    follow(getSelected());
                } else {
                    follow(null);
                }
            });
        }
        return mFollowButton;
    }

    private Button getClearMapButton() {
        if (mClearMapButton == null) {
            mClearMapButton = new Button("Clear Map");
            mClearMapButton.setOnAction(e -> {
                mMapPainter.clearAllEntities();
                repaintCanvas();
            });
        }
        return mClearMapButton;
    }

    private CheckBox getCenterOnSelectedCheckBox() {
        if (mCenterOnSelectedCheckBox == null) {
            mCenterOnSelectedCheckBox = new CheckBox("Center on Selection");
            mCenterOnSelectedCheckBox.setSelected(true);
        }
        return mCenterOnSelectedCheckBox;
    }

    private Button getDeleteAllTracksButton() {
        if (mDeleteAllTracksButton == null) {
            mDeleteAllTracksButton = new Button("Delete All");
            mDeleteAllTracksButton.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Are you sure you want to delete all tracks?", ButtonType.YES, ButtonType.NO);
                alert.setTitle("Delete All Tracks");
                Optional<ButtonType> confirmation = alert.showAndWait();
                if (confirmation.isPresent() && confirmation.get() == ButtonType.YES) {
                    mMapService.getPlottableEntityModel().deleteAllTracks();
                    mMapPainter.clearAllEntities();
                    follow(null);
                    repaintCanvas();
                }
            });
        }
        return mDeleteAllTracksButton;
    }

    private Button getDeleteTrackButton() {
        if (mDeleteTrackButton == null) {
            mDeleteTrackButton = new Button("Delete");
            mDeleteTrackButton.setDisable(true);
            mDeleteTrackButton.setOnAction(e -> {
                List<PlottableEntityHistory> selected = getPlottedTracksTable().getSelectionModel().getSelectedItems();
                if (selected.isEmpty()) return;

                String message = selected.size() == 1
                        ? "Are you sure you want to delete the selected track?"
                        : "Are you sure you want to delete the " + selected.size() + " selected tracks?";

                Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.YES, ButtonType.NO);
                alert.setTitle("Delete Tracks");
                Optional<ButtonType> confirmation = alert.showAndWait();

                if (confirmation.isPresent() && confirmation.get() == ButtonType.YES) {
                    List<PlottableEntityHistory> toDelete = new ArrayList<>(selected);
                    mMapService.getPlottableEntityModel().delete(toDelete);
                    mMapPainter.clearEntities(toDelete);

                    if (toDelete.contains(mFollowedTrack)) {
                        follow(null);
                    }
                    repaintCanvas();
                }
            });
        }
        return mDeleteTrackButton;
    }

    private PlottableEntityHistory getSelected() {
        return getPlottedTracksTable().getSelectionModel().getSelectedItem();
    }

    private void centerOn(PlottableEntityHistory entityHistory) {
        if (entityHistory != null) {
            GeoPosition geoPosition = entityHistory.getLatestPosition();
            if (geoPosition != null) {
                Platform.runLater(() -> mMapViewer.setCenterPosition(geoPosition));
            }
        }
    }

    private void follow(PlottableEntityHistory entityHistory) {
        mFollowedTrack = entityHistory;
        if (mFollowedTrack != null) {
            centerOn(mFollowedTrack);
            getFollowButton().setText(UNFOLLOW);
            getFollowButton().setDisable(false);
            getFollowedEntityLabel().setText("Following: " + mFollowedTrack.getIdentifier());
            getCenterOnSelectedCheckBox().setDisable(true);
        } else {
            getFollowButton().setText(FOLLOW);
            getFollowButton().setDisable(getSelected() == null);
            getFollowedEntityLabel().setText("");
            getCenterOnSelectedCheckBox().setDisable(false);
        }
    }

    private TableView<PlottableEntityHistory> getPlottedTracksTable() {
        if (mPlottedTracksTable == null) {
            mPlottedTracksTable = new TableView<>();
            mPlottedTracksTable.setItems(mMapService.getPlottableEntityModel().getItems());

            TableColumn<PlottableEntityHistory, String> idCol = new TableColumn<>("ID");
            idCol.setCellValueFactory(data -> {
                Identifier id = data.getValue().getIdentifier();
                return new SimpleStringProperty(id != null ? id.toString() : "(no ID)");
            });

            mPlottedTracksTable.getColumns().add(idCol);

            mPlottedTracksTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                getDeleteTrackButton().setDisable(newVal == null);
                setSelected(newVal);
                follow(mFollowedTrack);
            });
        }
        return mPlottedTracksTable;
    }

    private ToggleSwitch getTrackGeneratorToggle() {
        if (mTrackGeneratorToggle == null) {
            mTrackGeneratorToggle = new ToggleSwitch();
            mTrackGeneratorToggle.switchedOnProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    getTrackGenerator().start();
                } else {
                    getTrackGenerator().stop();
                }
            });
        }
        return mTrackGeneratorToggle;
    }

    private TrackGenerator getTrackGenerator() {
        if (mTrackGenerator == null) {
            mTrackGenerator = new TrackGenerator(mMapService);
        }
        return mTrackGenerator;
    }

    public JXMapViewer getMapViewer() {
        if (mMapViewer == null) {
            mMapViewer = new JXMapViewer();
            
            // JXMapViewer map image source
            TileFactoryInfo info = new OSMTileFactoryInfo();
            DefaultTileFactory tileFactory = new DefaultTileFactory(info);
            mMapViewer.setTileFactory(tileFactory);
            tileFactory.setThreadPoolSize(8);

            GeoPosition syracuse = new GeoPosition(43.048, -76.147);
            int zoom = 7;

            MapViewSetting view = mSettingsManager.getSettingsModel().getMapViewSetting("Default", syracuse, zoom);
            mMapViewer.setAddressLocation(view.getGeoPosition());
            mMapViewer.setZoom(view.getZoom());

            MapMouseListener listener = new MapMouseListener(mMapViewer, mSettingsManager);
            mMapViewer.addEventHandler(MouseEvent.MOUSE_PRESSED, listener::mousePressed);
            mMapViewer.addEventHandler(MouseEvent.MOUSE_DRAGGED, listener::mouseDragged);
            mMapViewer.addEventHandler(MouseEvent.MOUSE_RELEASED, listener::mouseReleased);
            mMapViewer.addEventHandler(MouseEvent.MOUSE_ENTERED, listener::mouseEntered);
            
            ZoomMouseWheelListenerCursor zoomListener = new ZoomMouseWheelListenerCursor(this);
            mMapViewer.addEventHandler(ScrollEvent.SCROLL, zoomListener);
            
            PanKeyListener panListener = new PanKeyListener(mMapViewer);
            mMapViewer.addEventHandler(KeyEvent.KEY_PRESSED, panListener);
            mMapViewer.setFocusTraversable(true);

            mSelectionAdapter = new SelectionAdapter(mMapViewer);
            mMapViewer.addEventHandler(MouseEvent.MOUSE_PRESSED, mSelectionAdapter::mousePressed);
            mMapViewer.addEventHandler(MouseEvent.MOUSE_DRAGGED, mSelectionAdapter::mouseDragged);
            mMapViewer.addEventHandler(MouseEvent.MOUSE_RELEASED, mSelectionAdapter::mouseReleased);

            mSelectionPainter = new SelectionPainter(mSelectionAdapter);

            // Add property change listener to repaint the JavaFX canvas whenever the map updates
            
        }
        return mMapViewer;
    }

    public void adjustZoom(int adjustment) {
        Platform.runLater(() -> {
            int currentZoom = getMapViewer().getZoom();
            int updatedZoom = currentZoom + adjustment;
            if (ZOOM_MINIMUM <= updatedZoom && updatedZoom <= ZOOM_MAXIMUM) {
                getMapViewer().setZoom(updatedZoom);
            }
        });
    }

    @Override
    public void entitiesUpdated() {
        Platform.runLater(this::repaintCanvas);
    }

    @Override
    public void addPlottableEntity(PlottableEntityHistory entity) {
        mMapPainter.addEntity(entity);
        entitiesUpdated();
    }

    @Override
    public void removePlottableEntity(PlottableEntityHistory entity) {
        mMapPainter.removeEntity(entity);
        entitiesUpdated();
    }
}
