package io.github.dsheirer.filter;

import javafx.fxml.FXML;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.cell.CheckBoxTreeCell;
import java.util.List;
import java.util.Comparator;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeCell;
import javafx.geometry.Insets;

public class FilterEditorPanelController<T> {

    @FXML
    private TreeView<Object> mTreeView;

    private FilterSet<T> mFilterSet;

    @FXML
    public void initialize() {
        mTreeView.setShowRoot(true);
        mTreeView.setCellFactory(tv -> new FilterTreeCell());
    }

    public void setFilterSet(FilterSet<T> filterSet) {
        mFilterSet = filterSet;
        buildTree();
    }

    private void buildTree() {
        CheckBoxTreeItem<Object> root = new CheckBoxTreeItem<>(mFilterSet);
        root.setExpanded(true);
        addFilterSet(mFilterSet, root);
        mTreeView.setRoot(root);
    }

    private void addFilterSet(FilterSet<T> filterSet, CheckBoxTreeItem<Object> parent) {
        List<IFilter<T>> filters = filterSet.getFilters();
        for (IFilter<T> filter : filters) {
            CheckBoxTreeItem<Object> childNode = new CheckBoxTreeItem<>(filter);
            childNode.setSelected(filter.isEnabled());
            childNode.selectedProperty().addListener((obs, oldVal, newVal) -> {
                setFilterEnabled(childNode, newVal);
            });
            parent.getChildren().add(childNode);

            if (filter instanceof FilterSet) {
                addFilterSet((FilterSet<T>) filter, childNode);
            } else if (filter instanceof Filter) {
                addFilter((Filter) filter, childNode);
            }
        }
    }

    private void addFilter(Filter filter, CheckBoxTreeItem<Object> parent) {
        List<FilterElement<?>> elements = filter.getFilterElements();
        elements.sort(Comparator.comparing(FilterElement::getName));
        for (FilterElement<?> element : elements) {
            CheckBoxTreeItem<Object> childNode = new CheckBoxTreeItem<>(element);
            childNode.setSelected(element.isEnabled());
            childNode.selectedProperty().addListener((obs, oldVal, newVal) -> {
                element.setEnabled(newVal);
                updateParents(childNode);
            });
            parent.getChildren().add(childNode);
        }
    }

    private void setFilterEnabled(CheckBoxTreeItem<Object> node, boolean enabled) {
        setFilterEnabledRecursive(node, enabled);
        updateParents(node);
    }

    private void setFilterEnabledRecursive(CheckBoxTreeItem<Object> node, boolean enabled) {
        Object userObject = node.getValue();
        if (userObject instanceof FilterElement) {
            ((FilterElement<?>) userObject).setEnabled(enabled);
        }

        for (TreeItem<Object> child : node.getChildren()) {
            if (child instanceof CheckBoxTreeItem) {
                ((CheckBoxTreeItem<Object>) child).setSelected(enabled);
                setFilterEnabledRecursive((CheckBoxTreeItem<Object>) child, enabled);
            }
        }
    }


    private void updateParents(TreeItem<Object> node) {
        mTreeView.refresh();
    }

    private class FilterTreeCell extends TreeCell<Object> {
        private final HBox container = new HBox(5);
        private final CheckBox checkBox = new CheckBox();
        private final Label label = new Label();
        private Label helpLabel = null;
        private CheckBoxTreeItem<Object> treeItem;

        public FilterTreeCell() {
            container.setAlignment(Pos.CENTER_LEFT);
            container.getChildren().addAll(checkBox, label);

            checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (treeItem != null) {
                    treeItem.setSelected(newVal);
                }
            });
        }

        @Override
        public void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                treeItem = (CheckBoxTreeItem<Object>) getTreeItem();
                checkBox.setSelected(treeItem.isSelected());

                String text = "";
                String tooltipContent = null;
                String name = "";

                if (item instanceof FilterSet) {
                    text = ((FilterSet<?>) item).getName();
                } else if (item instanceof IFilter) {
                    IFilter<?> filter = (IFilter<?>) item;
                    name = filter.getName();
                    text = name + " (" + filter.getEnabledCount() + "/" + filter.getElementCount() + ")";
                    tooltipContent = "Controls the enabling of elements for this specific filter set.\nBenefit: Instantly toggle related DSP components.";
                    if (name.equals("De-emphasis Filter")) {
                        tooltipContent = "Restores natural voice balance by reversing the pre-emphasis applied at the transmitter.\nBenefit: Significantly reduces harsh high-frequency hiss.";
                    } else if (name.equals("Decimation Filter")) {
                        tooltipContent = "Reduces the sample rate of the signal to lower processing requirements.\nBenefit: Vastly improves efficiency.";
                    } else if (name.equals("Squaring Filter")) {
                        tooltipContent = "Squares the input signal values.\nBenefit: Useful in energy detection algorithms.";
                    }
                } else if (item instanceof FilterElement) {
                    FilterElement<?> element = (FilterElement<?>) item;
                    name = element.getName();
                    text = name;
                    tooltipContent = "Filter configuration element.\nCheck documentation for specific DSP effect.";
                    if (name.equals("De-emphasis Filter")) {
                        tooltipContent = "Restores natural voice balance by reversing the pre-emphasis applied at the transmitter.\nBenefit: Significantly reduces harsh high-frequency hiss.";
                    } else if (name.equals("Decimation Filter")) {
                        tooltipContent = "Reduces the sample rate of the signal to lower processing requirements.\nBenefit: Vastly improves efficiency.";
                    } else if (name.equals("Squaring Filter")) {
                        tooltipContent = "Squares the input signal values.\nBenefit: Useful in energy detection algorithms.";
                    }
                } else {
                    text = item.toString();
                }

                label.setText(text);

                if (tooltipContent != null) {
                    if (helpLabel == null) {
                        helpLabel = io.github.dsheirer.gui.help.HelpIconLabelFactory.createHelpIcon("");
                        container.getChildren().add(helpLabel);
                    }
                    helpLabel.getTooltip().setText(name + "\n" + tooltipContent);
                } else {
                    if (helpLabel != null) {
                        container.getChildren().remove(helpLabel);
                        helpLabel = null;
                    }
                }

                setGraphic(container);
            }
        }
    }
}
