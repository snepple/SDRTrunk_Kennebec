package io.github.dsheirer.module.decode.event;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.filter.IFilter;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.MessageHistory;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.module.decode.DecoderFactory;
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

public class MessageActivityPanel extends VBox implements Listener<ProcessingChain>
{
    private static final long serialVersionUID = 1L;
    private static final Logger mLog = LoggerFactory.getLogger(MessageActivityPanel.class);
    private MessageActivityModel mMessageModel = new MessageActivityModel();
    private MessageHistory mCurrentMessageHistory;

    private UserPreferences mUserPreferences;
    private FilterSet<IMessage> mMessageFilterSet;
    private boolean mRestoringFilters = false;
    private HistoryManagementPanel<IMessage> mHistoryManagementPanel;
    private MessageActivityTableController mTableController;

    public MessageActivityPanel(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;

        mHistoryManagementPanel = new HistoryManagementPanel<>(mMessageModel, "Message Filter Editor");

        Platform.runLater(() -> {
            getChildren().add(mHistoryManagementPanel);

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/module/decode/event/MessageActivityTable.fxml"));
                Parent root = loader.load();
                mTableController = loader.getController();
                mTableController.setItems(mMessageModel.getItems());
                mTableController.setFilterPredicate(this::evaluateFilter);

                VBox.setVgrow(root, Priority.ALWAYS);
                getChildren().add(root);
            } catch (IOException e) {
                mLog.error("Error loading MessageActivityTable.fxml", e);
            }
        });
    }

    private boolean evaluateFilter(MessageItem item) {
        if (mMessageFilterSet != null && item != null && item.getMessage() != null) {
            IMessage message = item.getMessage();
            return mMessageFilterSet.canProcess(message) && mMessageFilterSet.passes(message);
        }
        return false;
    }

    @Override
    public void receive(ProcessingChain processingChain)
    {
        if(mCurrentMessageHistory != null)
        {
            mCurrentMessageHistory.removeListener(mMessageModel);
        }

        if(mMessageFilterSet != null)
        {
            mMessageFilterSet.register(null);
        }

        if(processingChain != null)
        {
            mCurrentMessageHistory = processingChain.getMessageHistory();

            mCurrentMessageHistory.addListener(mMessageModel);

            mMessageFilterSet = null;

            mHistoryManagementPanel.updateFilterSet(mMessageFilterSet);
            mHistoryManagementPanel.setEnabled(true);

            if(mMessageFilterSet != null)
            {
                restoreFilterStates();
                mMessageFilterSet.register(() -> {
                    saveFilterStates();
                    Platform.runLater(() -> mTableController.updateFilter());
                });
                Platform.runLater(() -> mTableController.updateFilter());
            }
        }
        else
        {
            mCurrentMessageHistory = null;
            mMessageModel.clear();
            mHistoryManagementPanel.setEnabled(false);
            mMessageFilterSet = null;
            mHistoryManagementPanel.updateFilterSet(null);
            Platform.runLater(() -> mTableController.updateFilter());
        }
    }

    private void restoreFilterStates()
    {
        mRestoringFilters = true;

        if(mMessageFilterSet != null)
        {
            for(IFilter<IMessage> ifilter: mMessageFilterSet.getFilters())
            {
                if(ifilter instanceof Filter<?,?> filter)
                {
                    for(FilterElement<?> filterElement: filter.getFilterElements())
                    {
                        filterElement.setEnabled(false);
                    }
                }
            }
        }

        mRestoringFilters = false;
    }

    private void saveFilterStates()
    {
        if(!mRestoringFilters && mMessageFilterSet != null)
        {
            for(IFilter<IMessage> ifilter: mMessageFilterSet.getFilters())
            {
                if(ifilter instanceof Filter<?,?> filter)
                {
                    for(FilterElement<?> filterElement: filter.getFilterElements())
                    {

                    }
                }
            }
        }
    }
}
