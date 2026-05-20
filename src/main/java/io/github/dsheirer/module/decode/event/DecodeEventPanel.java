package io.github.dsheirer.module.decode.event;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.filter.IFilter;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.preference.NowPlayingPreference;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.module.decode.event.filter.DecodeEventFilterSet;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

public class DecodeEventPanel extends VBox implements Listener<ProcessingChain>
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(DecodeEventPanel.class);

    private DecodeEventModel mEventModel = new DecodeEventModel();
    private DecodeEventModel mGlobalEventModel = new DecodeEventModel();
    private ChannelProcessingManager mChannelProcessingManager;
    private Listener<IDecodeEvent> mGlobalEventListener;

    private class ActiveModelWrapper extends ClearableHistoryModel<IDecodeEvent> {
        @Override
        public void clear() {
            mEventModel.clear();
            mGlobalEventModel.clear();
        }

        @Override
        public void setHistorySize(int size) {
            super.setHistorySize(size);
            mEventModel.setHistorySize(size);
            mGlobalEventModel.setHistorySize(size);
        }
    }

    private ActiveModelWrapper mActiveModelWrapper = new ActiveModelWrapper();
    private DecodeEventHistory mCurrentEventHistory;
    private IconModel mIconModel;
    private AliasModel mAliasModel;
    private UserPreferences mUserPreferences;
    private FilterSet<IDecodeEvent> mFilterSet = new DecodeEventFilterSet();
    private HistoryManagementPanel<IDecodeEvent> mHistoryManagementPanel;
    private DecodeEventTableController mTableController;

    public DecodeEventPanel(IconModel iconModel, UserPreferences userPreferences, AliasModel aliasModel, ChannelProcessingManager channelProcessingManager)
    {
        MyEventBus.getGlobalEventBus().register(this);

        mIconModel = iconModel;
        mAliasModel = aliasModel;
        mUserPreferences = userPreferences;
        mChannelProcessingManager = channelProcessingManager;

        mGlobalEventListener = event -> mGlobalEventModel.receive(event);
        mChannelProcessingManager.addDecodeEventListener(mGlobalEventListener);
        mGlobalEventModel.setHistorySize(mUserPreferences.getNowPlayingPreference().getEventHistorySize());

        NowPlayingPreference nowPlayingPreference = mUserPreferences.getNowPlayingPreference();
        restoreFilterStates(nowPlayingPreference);

        mActiveModelWrapper.setHistorySize(nowPlayingPreference.getEventHistorySize());
        mHistoryManagementPanel = new HistoryManagementPanel<>(
                mActiveModelWrapper,
                "Event Filter Editor",
                nowPlayingPreference.getEventHistorySize(),
                nowPlayingPreference::setEventHistorySize);

        mHistoryManagementPanel.updateFilterSet(mFilterSet);

        Platform.runLater(() -> {
            getChildren().add(mHistoryManagementPanel);

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/module/decode/event/DecodeEventTable.fxml"));
                Parent root = loader.load();
                mTableController = loader.getController();
                mTableController.initialize(mUserPreferences, mAliasModel);
                mTableController.setItems(mGlobalEventModel.getItems());
                mTableController.setFilterPredicate(this::evaluateFilter);

                VBox.setVgrow(root, Priority.ALWAYS);
                getChildren().add(root);
            } catch (IOException e) {
                mLog.error("Error loading DecodeEventTable.fxml", e);
            }
        });

        mFilterSet.register(() -> {
            saveFilterStates(nowPlayingPreference);
            Platform.runLater(() -> {
                if (mTableController != null) mTableController.updateFilter();
            });
        });
    }

    private void restoreFilterStates(NowPlayingPreference prefs)
    {
        for(IFilter<IDecodeEvent> ifilter : mFilterSet.getFilters())
        {
            if(ifilter instanceof Filter<?,?> filter)
            {
                for(FilterElement<?> element : filter.getFilterElements())
                {
                    String key = filter.getName() + "." + element.getName();
                    element.setEnabled(prefs.isFilterEnabled(key));
                }
            }
        }
    }

    private void saveFilterStates(NowPlayingPreference prefs)
    {
        for(IFilter<IDecodeEvent> ifilter : mFilterSet.getFilters())
        {
            if(ifilter instanceof Filter<?,?> filter)
            {
                for(FilterElement<?> element : filter.getFilterElements())
                {
                    String key = filter.getName() + "." + element.getName();
                    prefs.setFilterEnabled(key, element.isEnabled());
                }
            }
        }
    }

    public void dispose()
    {
        MyEventBus.getGlobalEventBus().unregister(this);
    }

    private boolean evaluateFilter(IDecodeEvent item) {
        if (mFilterSet != null && item != null) {
            return mFilterSet.canProcess(item) && mFilterSet.passes(item);
        }
        return false;
    }

    @Override
    public void receive(final ProcessingChain processingChain)
    {
        if(mCurrentEventHistory != null)
        {
            mCurrentEventHistory.removeListener(mEventModel);
        }

        if(processingChain != null)
        {
            mCurrentEventHistory = processingChain.getDecodeEventHistory();
            mEventModel.clearAndSet(mCurrentEventHistory.getItems());
            mCurrentEventHistory.addListener(mEventModel);
            Platform.runLater(() -> {
                if (mTableController != null) {
                    mTableController.setItems(mEventModel.getItems());
                    mTableController.updateFilter();
                }
                mHistoryManagementPanel.setEnabled(true);
            });
        }
        else
        {
            mCurrentEventHistory = null;
            Platform.runLater(() -> {
                if (mTableController != null) {
                    mTableController.setItems(mGlobalEventModel.getItems());
                    mTableController.updateFilter();
                }
                mHistoryManagementPanel.setEnabled(true);
            });
        }
    }
}
