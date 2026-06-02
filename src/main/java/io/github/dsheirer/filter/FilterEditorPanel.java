/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.filter;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.Node;

import java.util.Comparator;
import java.util.List;

import io.github.dsheirer.gui.help.HelpIconLabel;

/**
 * Editor panel for managing the state of a filter set.
 *
 * @param <T> element type for the filter set.
 */
public class FilterEditorPanel<T> extends VBox
{
    private TreeView<Object> mTree;
    private TreeItem<Object> mRootItem;
    private FilterSet<T> mFilterSet;

    /**
     * Constructs an instance
     * @param filterSet to manage
     */
    public FilterEditorPanel(FilterSet<T> filterSet)
    {
        mFilterSet = filterSet;
        init();
    }

    /**
     * Initializes the panel and the tree model.
     */
    private void init()
    {
        mRootItem = new TreeItem<>(mFilterSet);
        mRootItem.setExpanded(true);
        addFilterSet(mFilterSet, mRootItem);

        mTree = new TreeView<>(mRootItem);
        mTree.setShowRoot(true);
        mTree.setCellFactory(treeView -> new FilterTreeCell());
        
        VBox.setVgrow(mTree, Priority.ALWAYS);
        getChildren().add(mTree);
    }

    /**
     * Updates the filter set for this editor panel
     * @param filterSet to use
     */
    public void updateFilterSet(FilterSet<T> filterSet)
    {
        getChildren().remove(mTree);
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
            TreeItem<Object> childNode = new TreeItem<>(filter);
            childNode.setExpanded(true);
            parent.getChildren().add(childNode);

            if(filter instanceof FilterSet childFilterSet)
            {
                addFilterSet(childFilterSet, childNode);
            }
            else if(filter instanceof Filter childFilter)
            {
                addFilter(childFilter, childNode);
            }
        }
    }

    /**
     * Adds the filter as a child tree node to the parent.
     *
     * @param filter to add
     * @param parent tree node
     */
    private void addFilter(Filter filter, TreeItem<Object> parent)
    {
        List<FilterElement<?>> elements = filter.getFilterElements();

        elements.sort(Comparator.comparing(FilterElement::getName));

        for(FilterElement<?> element : elements)
        {
            TreeItem<Object> child = new TreeItem<>(element);
            parent.getChildren().add(child);
        }
    }

    /**
     * Recursively sets the enabled state on all filter node children below the specified parent node.
     *
     * @param node - parent node
     * @param enabled - true or false to enable or disable the filters
     */
    private void setFilterEnabled(TreeItem<Object> node, boolean enabled)
    {
        Object obj = node.getValue();

        if(obj instanceof FilterElement filterElement)
        {
            filterElement.setEnabled(enabled);
        }

        /* Recursively set the children of this node */
        for(TreeItem<Object> child : node.getChildren())
        {
            setFilterEnabled(child, enabled);
        }
    }

    /**
     * Custom cell factory
     */
    private class FilterTreeCell extends TreeCell<Object>
    {
        private HBox graphicContainer = new HBox(5);
        private CheckBox checkBox = new CheckBox();
        private HelpIconLabel helpIcon = new HelpIconLabel("");
        private Label fallbackLabel = new Label();

        public FilterTreeCell()
        {
            checkBox.setOnAction(e -> {
                Object item = getItem();
                if (item instanceof IFilter filter) {
                    setFilterEnabled(getTreeItem(), checkBox.isSelected());
                    mTree.refresh();
                } else if (item instanceof FilterElement element) {
                    element.setEnabled(checkBox.isSelected());
                    mTree.refresh();
                }
            });
        }

        @Override
        protected void updateItem(Object item, boolean empty)
        {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                graphicContainer.getChildren().clear();

                if (item instanceof IFilter filter) {
                    checkBox.setSelected(filter.isEnabled());
                    checkBox.setText(filter.getName() + " (" + filter.getEnabledCount() + "/" + filter.getElementCount() + ")");
                    graphicContainer.getChildren().add(checkBox);
                    
                    String tooltipText = getTooltipForFilter(filter.getName(), true);
                    if (tooltipText != null) {
                        helpIcon.setHelpText(tooltipText);
                        graphicContainer.getChildren().add(helpIcon);
                    }
                    
                    setText(null);
                    setGraphic(graphicContainer);
                } else if (item instanceof FilterElement element) {
                    checkBox.setSelected(element.isEnabled());
                    checkBox.setText(element.getName());
                    graphicContainer.getChildren().add(checkBox);

                    String tooltipText = getTooltipForFilter(element.getName(), false);
                    if (tooltipText != null) {
                        helpIcon.setHelpText(tooltipText);
                        graphicContainer.getChildren().add(helpIcon);
                    }

                    setText(null);
                    setGraphic(graphicContainer);
                } else {
                    fallbackLabel.setText(item.toString());
                    setText(null);
                    setGraphic(fallbackLabel);
                }
            }
        }
        
        private String getTooltipForFilter(String name, boolean isFilterSet) {
            String tooltipContent = isFilterSet ? 
                "Controls the enabling of elements for this specific filter set.<br>Benefit: Instantly toggle related DSP components." :
                "Filter configuration element.<br>Check documentation for specific DSP effect.";

            if(name.equals("De-emphasis Filter")) {
                 tooltipContent = "Restores natural voice balance by reversing the pre-emphasis applied at the transmitter.<br>Benefit: Significantly reduces harsh high-frequency hiss.";
            } else if(name.equals("Decimation Filter")) {
                 tooltipContent = "Reduces the sample rate of the signal to lower processing requirements.<br>Benefit: Vastly improves efficiency.";
            } else if(name.equals("Squaring Filter")) {
                 tooltipContent = "Squares the input signal values.<br>Benefit: Useful in energy detection algorithms.";
            }

            return "<b>" + name + "</b><br>" + tooltipContent;
        }
    }
}
