package io.github.dsheirer.module.decode.event;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.preference.UserPreferences;

import com.google.common.base.Joiner;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.embed.swing.SwingNode;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class DecodeEventPanelController {

    @FXML
    private VBox mainContainer;

    @FXML
    private SwingNode historyManagementNode;

    @FXML
    private TableView<IDecodeEvent> tableView;

    private DecodeEventModel mEventModel;
    private DecodeEventModel mGlobalEventModel;
    private DecodeEventModel mCurrentModel;
    private ObservableList<IDecodeEvent> rowData = FXCollections.observableArrayList();
    private FilteredList<IDecodeEvent> filteredData;
    private FilterSet<IDecodeEvent> mEventFilterSet;

    private IconModel mIconModel;
    private AliasModel mAliasModel;
    private UserPreferences mUserPreferences;

    private TableModelListener mTableModelListener;

    public void init(DecodeEventModel eventModel, DecodeEventModel globalEventModel, HistoryManagementPanel<IDecodeEvent> historyManagementPanel,
                     IconModel iconModel, AliasModel aliasModel, UserPreferences userPreferences) {
        mEventModel = eventModel;
        mGlobalEventModel = globalEventModel;
        mIconModel = iconModel;
        mAliasModel = aliasModel;
        mUserPreferences = userPreferences;

        javax.swing.SwingUtilities.invokeLater(() -> {
            historyManagementNode.setContent(historyManagementPanel);
        });

        mTableModelListener = e -> {
            Platform.runLater(() -> {
                if (e.getType() == TableModelEvent.INSERT) {
                    for (int i = e.getFirstRow(); i <= e.getLastRow(); i++) {
                        IDecodeEvent item = mCurrentModel.getItem(i);
                        if (item != null) {
                            rowData.add(i, item);
                        }
                    }
                } else if (e.getType() == TableModelEvent.DELETE) {
                    for (int i = e.getLastRow(); i >= e.getFirstRow(); i--) {
                        if (i < rowData.size() && i >= 0) {
                            rowData.remove(i);
                        }
                    }
                } else if (e.getType() == TableModelEvent.UPDATE) {
                    if (e.getFirstRow() == 0 && e.getLastRow() == Integer.MAX_VALUE) {
                        refreshData();
                    } else {
                        for (int i = e.getFirstRow(); i <= e.getLastRow(); i++) {
                            if (i < rowData.size() && i >= 0) {
                                IDecodeEvent item = mCurrentModel.getItem(i);
                                if (item != null) {
                                    rowData.set(i, item);
                                }
                            }
                        }
                    }
                    tableView.refresh();
                }
            });
        };

        mCurrentModel = mGlobalEventModel;
        mCurrentModel.addTableModelListener(mTableModelListener);

        refreshData();

        filteredData = new FilteredList<>(rowData, p -> checkFilter(p));
        SortedList<IDecodeEvent> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedData);

        setupColumns();
    }

    public void setActiveHistory(boolean active) {
        if (mCurrentModel != null) {
            mCurrentModel.removeTableModelListener(mTableModelListener);
        }
        mCurrentModel = active ? mEventModel : mGlobalEventModel;
        mCurrentModel.addTableModelListener(mTableModelListener);
    }

    public void refreshData() {
        rowData.clear();
        if (mCurrentModel != null) {
            for (int i = 0; i < mCurrentModel.getRowCount(); i++) {
                IDecodeEvent item = mCurrentModel.getItem(i);
                if (item != null) {
                    rowData.add(item);
                }
            }
        }
    }

    private void setupColumns() {
        TableColumn<IDecodeEvent, IDecodeEvent> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        timeCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(IDecodeEvent item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    SimpleDateFormat sdf = mUserPreferences.getDecodeEventPreference().getTimestampFormat().getFormatter();
                    setText(sdf.format(new Date(item.getTimeStart())));
                }
            }
        });



        TableColumn<IDecodeEvent, IDecodeEvent> fromCol = new TableColumn<>("From");
        fromCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        fromCol.setCellFactory(column -> new IdentifierCell(Role.FROM));

        TableColumn<IDecodeEvent, IDecodeEvent> toCol = new TableColumn<>("To");
        toCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        toCol.setCellFactory(column -> new IdentifierCell(Role.TO));

        TableColumn<IDecodeEvent, String> eventCol = new TableColumn<>("Event");
        eventCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEventType().getLabel()));

        TableColumn<IDecodeEvent, IDecodeEvent> durCol = new TableColumn<>("Duration");
        durCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        durCol.setCellFactory(column -> new TableCell<>() {
            private DecimalFormat df = new DecimalFormat("0.0");
            @Override
            protected void updateItem(IDecodeEvent item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    long duration = item.getDuration();
                    if (duration > 0) {
                        setText(df.format((double)duration / 1e3d));
                    } else {
                        setText(null);
                    }
                }
            }
        });

        TableColumn<IDecodeEvent, String> protoCol = new TableColumn<>("Protocol");
        protoCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProtocol().toString()));

        TableColumn<IDecodeEvent, IDecodeEvent> freqCol = new TableColumn<>("Frequency");
        freqCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        freqCol.setCellFactory(column -> new TableCell<>() {
            private DecimalFormat df = new DecimalFormat("0.00000");
            @Override
            protected void updateItem(IDecodeEvent item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    if (item.getChannelDescriptor() != null) {
                        long freq = item.getChannelDescriptor().getDownlinkFrequency();
                        if (freq > 0) {
                            setText(df.format(freq / 1e6d));
                        } else {
                            setText(null);
                        }
                    } else {
                        setText(null);
                    }
                }
            }
        });

        TableColumn<IDecodeEvent, String> chanCol = new TableColumn<>("Channel");
        chanCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getChannelDescriptor() != null) {
                return new SimpleStringProperty(cellData.getValue().getChannelDescriptor().toString());
            }
            return new SimpleStringProperty("");
        });

        tableView.getColumns().addAll(timeCol, fromCol, toCol, eventCol, durCol, protoCol, freqCol, chanCol);
    }

    private class IdentifierCell extends TableCell<IDecodeEvent, IDecodeEvent> {
        private Role mRole;

        public IdentifierCell(Role role) {
            mRole = role;
        }

        @Override
        protected void updateItem(IDecodeEvent item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setTextFill(Color.BLACK);
            } else {
                IdentifierCollection ic = item.getIdentifierCollection();
                if (ic == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                List<Identifier> ids = ic.getIdentifiers(mRole);
                if (ids == null || ids.isEmpty()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                AliasList aliasList = mAliasModel.getAliasList(ic);
                if (aliasList != null) {
                    StringBuilder sb = new StringBuilder();
                    Color color = Color.BLACK;
                    javax.swing.ImageIcon swingIcon = null;

                    for (Identifier id : ids) {
                        List<Alias> aliases = aliasList.getAliases(id);
                        if (!aliases.isEmpty()) {
                            if (sb.length() > 0) sb.append(",");
                            sb.append(Joiner.on(", ").skipNulls().join(aliases));
                            java.awt.Color awtColor = aliases.get(0).getDisplayColor();
                            color = Color.rgb(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
                            swingIcon = mIconModel.getIcon(aliases.get(0).getIconName(), IconModel.DEFAULT_ICON_SIZE);
                        }
                    }

                    if (sb.length() > 0) {
                        setText(sb.toString());
                        setTextFill(color);
                        if (swingIcon != null && swingIcon.getImage() != null) {
                            java.awt.image.BufferedImage bImg = new java.awt.image.BufferedImage(
                                swingIcon.getIconWidth(), swingIcon.getIconHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
                            java.awt.Graphics2D g = bImg.createGraphics();
                            swingIcon.paintIcon(null, g, 0, 0);
                            g.dispose();
                            Image fxImage = javafx.embed.swing.SwingFXUtils.toFXImage(bImg, null);
                            setGraphic(new ImageView(fxImage));
                        } else {
                            setGraphic(null);
                        }
                        return;
                    }
                }

                // Fallback to formatting
                StringBuilder sb = new StringBuilder();
                for(Identifier id: ids) {
                    if(sb.length() > 0) sb.append(",");
                    if(id.getForm() == Form.TALKGROUP || id.getForm() == Form.RADIO || id.getForm() == Form.PATCH_GROUP) {
                        sb.append(mUserPreferences.getTalkgroupFormatPreference().format(id));
                    } else {
                        sb.append(id);
                    }
                }
                setText(sb.toString());
                setTextFill(Color.BLACK);
                setGraphic(null);
            }
        }
    }

    public void setEventFilterSet(FilterSet<IDecodeEvent> filterSet) {
        mEventFilterSet = filterSet;
        if (filteredData != null) {
            filteredData.setPredicate(p -> checkFilter(p));
        }
    }

    public void refreshFilter() {
        if (filteredData != null) {
            filteredData.setPredicate(p -> checkFilter(p));
        }
    }

    private boolean checkFilter(IDecodeEvent event) {
        if (mEventFilterSet == null) return true;
        if (event != null) {
            return mEventFilterSet.canProcess(event) && mEventFilterSet.passes(event);
        }
        return false;
    }
}
