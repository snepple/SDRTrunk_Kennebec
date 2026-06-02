package io.github.dsheirer.map;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.configuration.AliasListConfigurationIdentifier;
import io.github.dsheirer.module.decode.event.PlottableDecodeEvent;
import io.github.dsheirer.sample.Listener;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlottableEntityModel implements Listener<PlottableDecodeEvent> {
    private final static Logger LOGGER = LoggerFactory.getLogger(PlottableEntityModel.class);
    private static final String KEY_NO_ALIAS_LIST = "(no alias list)";
    private Map<String,PlottableEntityHistory> mEntityHistoryMap = new HashMap<>();
    private ObservableList<PlottableEntityHistory> mEntityHistories = FXCollections.observableArrayList();
    private List<IPlottableUpdateListener> mPlottableUpdateListeners = new ArrayList<>();
    private AliasModel mAliasModel;

    public PlottableEntityModel(AliasModel aliasModel) {
        mAliasModel = aliasModel;
    }

    public void deleteAllTracks() {
        Platform.runLater(() -> {
            mEntityHistoryMap.clear();
            mEntityHistories.clear();
        });
    }

    public void delete(List<PlottableEntityHistory> tracksToDelete) {
        Platform.runLater(() -> {
            for(PlottableEntityHistory track : tracksToDelete) {
                mEntityHistories.remove(track);
                mEntityHistoryMap.entrySet().removeIf(entry -> entry.getValue() == track);
            }
        });
    }

    @Override
    public void receive(PlottableDecodeEvent plottableDecodeEvent) {
        if(plottableDecodeEvent.isValidLocation()) {
            Platform.runLater(() -> {
                Identifier from = plottableDecodeEvent.getIdentifierCollection().getFromIdentifier();
                if(from != null && from.getForm() != Form.LOCATION) {
                    AliasListConfigurationIdentifier aliasList = plottableDecodeEvent.getIdentifierCollection().getAliasListConfiguration();
                    String key = (aliasList != null ? aliasList.toString() : KEY_NO_ALIAS_LIST) + from;
                    PlottableEntityHistory entityHistory = mEntityHistoryMap.get(key);

                    if(entityHistory == null) {
                        entityHistory = new PlottableEntityHistory(from, plottableDecodeEvent);
                        mEntityHistories.add(entityHistory);
                        mEntityHistoryMap.put(key, entityHistory);
                    } else {
                        entityHistory.add(plottableDecodeEvent);
                        // trigger update for ObservableList if needed
                        int index = mEntityHistories.indexOf(entityHistory);
                        if (index >= 0) {
                            mEntityHistories.set(index, entityHistory);
                        }
                    }
                    for(IPlottableUpdateListener listener : mPlottableUpdateListeners) {
                        listener.addPlottableEntity(entityHistory);
                    }
                }
            });
        }
    }

    public PlottableEntityHistory get(int index) {
        if(index >= 0 && index < mEntityHistories.size()) {
            return mEntityHistories.get(index);
        }
        return null;
    }

    public ObservableList<PlottableEntityHistory> getItems() {
        return mEntityHistories;
    }

    public List<PlottableEntityHistory> getAll() {
        return new ArrayList<>(mEntityHistories);
    }

    public void addListener(IPlottableUpdateListener listener) {
        mPlottableUpdateListeners.add(listener);
    }

    public void removeListener(IPlottableUpdateListener listener) {
        mPlottableUpdateListeners.remove(listener);
    }
}
