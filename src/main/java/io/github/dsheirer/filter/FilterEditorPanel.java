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

import java.util.Comparator;
import java.util.List;

import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.util.Callback;

/**
 * Editor panel for managing the state of a filter set.
 *
 * @param <T> element type for the filter set.
 */
public class FilterEditorPanel<T> extends VBox
{
    private static final long serialVersionUID = 1L;
    private TreeView<Object> mTree;
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
        TreeItem<Object> root = new TreeItem<>(mFilterSet);
        addFilterSet(mFilterSet, root);
        root.setExpanded(true);

        mTree = new TreeView<>(root);
        mTree.setShowRoot(true);
        mTree.setCellFactory(new Callback<TreeView<Object>, TreeCell<Object>>() {
            @Override
            public TreeCell<Object> call(TreeView<Object> param) {
                return new FilterTreeCell();
            }
        });

        VBox.setVgrow(mTree, Priority.ALWAYS);
        this.getChildren().add(mTree);
    }

    /**
     * Updates the filter set for this editor panel
     * @param filterSet to use
     */
    public void updateFilterSet(FilterSet<T> filterSet)
    {
        this.getChildren().clear();
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

    private void setFilterEnabled(TreeItem<Object> node, boolean enabled) {
        Object obj = node.getValue();
        if (obj instanceof FilterElement filterElement) {
            filterElement.setEnabled(enabled);
        }

        for (TreeItem<Object> child : node.getChildren()) {
            setFilterEnabled(child, enabled);
        }
    }

    private void updateParentage(TreeItem<Object> node) {
        // TreeTableView/TreeView will update automatically if properties are bound,
        // but since we are modifying underlying models we force a refresh by re-setting the value.
        TreeItem<Object> current = node;
        while (current != null) {
            Object val = current.getValue();
            current.setValue(null);
            current.setValue(val);
            current = current.getParent();
        }
    }

    private class FilterTreeCell extends TreeCell<Object> {
        private HBox hbox = new HBox();
        private CheckBox checkBox = new CheckBox();

        public FilterTreeCell() {
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.setSpacing(5);
            hbox.getChildren().add(checkBox);

            checkBox.setOnAction(e -> {
                TreeItem<Object> item = getTreeItem();
                if (item != null) {
                    Object obj = item.getValue();
                    boolean selected = checkBox.isSelected();
                    if (obj instanceof IFilter filter) {
                        setFilterEnabled(item, selected);
                        updateParentage(item);
                    } else if (obj instanceof FilterElement element) {
                        element.setEnabled(selected);
                        updateParentage(item);
                    }
                }
            });
        }

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                setTooltip(null);
            } else {
                if (item instanceof IFilter filter) {
                    checkBox.setText(filter.getName() + " (" + filter.getEnabledCount() + "/" + filter.getElementCount() + ")");
                    checkBox.setSelected(filter.isEnabled());

                    String tooltipContent = "Controls the enabling of elements for this specific filter set.\nBenefit: Instantly toggle related DSP components.";
                    if (filter.getName().equals("De-emphasis Filter")) {
                        tooltipContent = "Restores natural voice balance by reversing the pre-emphasis applied at the transmitter.\nBenefit: Significantly reduces harsh high-frequency hiss.";
                    } else if (filter.getName().equals("Decimation Filter")) {
                        tooltipContent = "Reduces the sample rate of the signal to lower processing requirements.\nBenefit: Vastly improves efficiency.";
                    } else if (filter.getName().equals("Squaring Filter")) {
                        tooltipContent = "Squares the input signal values.\nBenefit: Useful in energy detection algorithms.";
                    }

                    Tooltip tooltip = new Tooltip(filter.getName() + "\n" + tooltipContent);
                    setTooltip(tooltip);
                    setGraphic(hbox);
                    setText(null);
                } else if (item instanceof FilterElement element) {
                    checkBox.setText(element.getName());
                    checkBox.setSelected(element.isEnabled());

                    String tooltipContent = "Filter configuration element.\nCheck documentation for specific DSP effect.";
                    if (element.getName().equals("De-emphasis Filter")) {
                        tooltipContent = "Restores natural voice balance by reversing the pre-emphasis applied at the transmitter.\nBenefit: Significantly reduces harsh high-frequency hiss.";
                    } else if (element.getName().equals("Decimation Filter")) {
                        tooltipContent = "Reduces the sample rate of the signal to lower processing requirements.\nBenefit: Vastly improves efficiency.";
                    } else if (element.getName().equals("Squaring Filter")) {
                        tooltipContent = "Squares the input signal values.\nBenefit: Useful in energy detection algorithms.";
                    }

                    Tooltip tooltip = new Tooltip(element.getName() + "\n" + tooltipContent);
                    setTooltip(tooltip);
                    setGraphic(hbox);
                    setText(null);
                } else if (item instanceof FilterSet filterSet) {
                    setGraphic(null);
                    setText(filterSet.getName());
                    setTooltip(null);
                } else {
                    setGraphic(null);
                    setText(item.toString());
                    setTooltip(null);
                }
            }
        }
    }
}
