package io.github.dsheirer.filter;

import io.github.dsheirer.gui.help.HelpIconLabel;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.control.Tooltip;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Editor panel for managing the state of a filter set.
 *
 * @param <T> element type for the filter set.
 */
public class FilterEditorPanel<T> extends BorderPane
{
    private TreeView<Object> mTree;
    private FilterSet<T> mFilterSet;
    private FilterEditorPanelController mController;

    /**
     * Constructs an instance
     * @param filterSet to manage
     */
    public FilterEditorPanel(FilterSet<T> filterSet)
    {
        mFilterSet = filterSet;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/filter/FilterEditorPanel.fxml"));
            Parent root = loader.load();
            mController = loader.getController();
            mTree = mController.getTreeView();
            mTree.setCellFactory(tree -> new FilterTreeCell());
            setCenter(root);
            init();
        } catch (Exception e) {
            LoggerFactory.getLogger(FilterEditorPanel.class).error("Error loading FilterEditorPanel FXML", e);
        }
    }

    /**
     * Initializes the panel and the tree model.
     */
    private void init()
    {
        if (mTree != null) {
            TreeItem<Object> root = new TreeItem<>(mFilterSet);
            root.setExpanded(true);
            addFilterSet(mFilterSet, root);
            mTree.setRoot(root);
        }
    }

    /**
     * Updates the filter set for this editor panel
     * @param filterSet to use
     */
    public void updateFilterSet(FilterSet<T> filterSet)
    {
        mFilterSet = filterSet;
        init();
    }

    /**
     * Adds the filter set as a child tree node to the parent.
     * @param filterSet to add to the tree
     * @param parent node for the filter set child tree node.
     */
    private void addFilterSet(FilterSet<T> filterSet, TreeItem<Object> parent)
    {
        List<IFilter<T>> filters = filterSet.getFilters();

        for(IFilter<T> filter : filters)
        {
            TreeItem<Object> child = new TreeItem<>(filter);
            child.setExpanded(true);
            parent.getChildren().add(child);
            addFilterElements(filter, child);
        }
    }

    /**
     * Adds filter elements to the filter tree node.
     * @param filter to retrieve elements
     * @param parent node to attach element tree nodes
     */
    private void addFilterElements(IFilter<T> filter, TreeItem<Object> parent)
    {
        if(filter instanceof Filter) {
            for(Object obj : ((Filter<T, ?>)filter).getFilterElements())
            {
                FilterElement<T> element = (FilterElement<T>)obj;
                TreeItem<Object> child = new TreeItem<>(element);
                parent.getChildren().add(child);
            }
        }
    }

    /**
     * Sets all of the filter elements for this filter to the enable state.
     * @param filter to update
     * @param enable state
     */
    private void setFilterEnabled(IFilter<T> filter, boolean enable)
    {
        if(filter instanceof Filter) {

            for(Object obj : ((Filter<T, ?>)filter).getFilterElements())
            {
                FilterElement<T> element = (FilterElement<T>)obj;
                element.setEnabled(enable);
            }
            mTree.refresh();
        }
    }

    /**
     * Custom cell renderer
     */
    public class FilterTreeCell extends TreeCell<Object>
    {
        @Override
        protected void updateItem(Object item, boolean empty)
        {
            super.updateItem(item, empty);

            if(empty || item == null)
            {
                setText(null);
                setGraphic(null);
            }
            else
            {
                if(item instanceof FilterSet)
                {
                    setText(item.toString());
                    setGraphic(null);
                }
                else if(item instanceof IFilter filter)
                {
                    CheckBox checkBox = new CheckBox(filter.getName() + " (" + filter.getEnabledCount() + "/" + filter.getElementCount() + ")");
                    checkBox.setSelected(filter.isEnabled());

                    checkBox.setOnAction(e -> {
                        setFilterEnabled(filter, checkBox.isSelected());
                    });

                    HBox panel = new HBox(checkBox);

                    String filterName = filter.getName();
                    String tooltipContent = "Controls the enabling of elements for this specific filter set.\nBenefit: Instantly toggle related DSP components.";

                    if(filterName.equals("De-emphasis Filter")) {
                        tooltipContent = "Restores natural voice balance by reversing the pre-emphasis applied at the transmitter.\nBenefit: Significantly reduces harsh high-frequency hiss.";
                    } else if(filterName.equals("Decimation Filter")) {
                        tooltipContent = "Reduces the sample rate of the signal to lower processing requirements.\nBenefit: Vastly improves efficiency.";
                    } else if(filterName.equals("Squaring Filter")) {
                        tooltipContent = "Squares the input signal values.\nBenefit: Useful in energy detection algorithms.";
                    }

                    String tooltipText = filterName + "\n" + tooltipContent;

                    HelpIconLabel helpIcon = new HelpIconLabel(tooltipText);
                    panel.getChildren().add(helpIcon);

                    setGraphic(panel);
                    setText(null);
                }
                else if(item instanceof FilterElement element)
                {
                    CheckBox checkBox = new CheckBox(element.getName());
                    checkBox.setSelected(element.isEnabled());

                    checkBox.setOnAction(e -> {
                        element.setEnabled(checkBox.isSelected());
                        mTree.refresh();
                    });

                    HBox panel = new HBox(checkBox);

                    String elementName = element.getName();
                    String tooltipContent = "Filter configuration element.\nCheck documentation for specific DSP effect.";

                    if(elementName.equals("De-emphasis Filter")) {
                        tooltipContent = "Restores natural voice balance by reversing the pre-emphasis applied at the transmitter.\nBenefit: Significantly reduces harsh high-frequency hiss.";
                    } else if(elementName.equals("Decimation Filter")) {
                        tooltipContent = "Reduces the sample rate of the signal to lower processing requirements.\nBenefit: Vastly improves efficiency.";
                    } else if(elementName.equals("Squaring Filter")) {
                        tooltipContent = "Squares the input signal values.\nBenefit: Useful in energy detection algorithms.";
                    }

                    String tooltipText = elementName + "\n" + tooltipContent;

                    HelpIconLabel helpIcon = new HelpIconLabel(tooltipText);
                    panel.getChildren().add(helpIcon);

                    setGraphic(panel);
                    setText(null);
                }
            }
        }
    }
}
