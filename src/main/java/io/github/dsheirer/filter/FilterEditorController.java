package io.github.dsheirer.filter;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.HBox;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeItem.TreeModificationEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;

public class FilterEditorController<T> {

    @FXML
    private TreeView<Object> mTreeView;

    @FXML
    private Button mCloseButton;

    private FilterSet<T> mFilterSet;

    @FXML
    public void initialize() {
        mTreeView.setShowRoot(true);
        mTreeView.setCellFactory(treeView -> new FilterTreeCell());
    }

    public void setFilterSet(FilterSet<T> filterSet) {
        mFilterSet = filterSet;
        buildTree();
    }

    private void buildTree() {
        if (mFilterSet == null) return;

        FilterTreeItem rootItem = new FilterTreeItem(mFilterSet);
        addFilterSetToItem(mFilterSet, rootItem);

        mTreeView.setRoot(rootItem);
        rootItem.setExpanded(true);
    }

    private void addFilterSetToItem(FilterSet<T> filterSet, CheckBoxTreeItem<Object> parentItem) {
        List<IFilter<T>> filters = filterSet.getFilters();
        for (IFilter<T> filter : filters) {
            FilterTreeItem childItem = new FilterTreeItem(filter);
            parentItem.getChildren().add(childItem);

            if (filter instanceof FilterSet childFilterSet) {
                addFilterSetToItem(childFilterSet, childItem);
            } else if (filter instanceof Filter childFilter) {
                addFilterToItem(childFilter, childItem);
            }
        }
    }

    private void addFilterToItem(Filter filter, CheckBoxTreeItem<Object> parentItem) {
        List<FilterElement<?>> elements = filter.getFilterElements();
        elements.sort(Comparator.comparing(FilterElement::getName));

        for (FilterElement<?> element : elements) {
            FilterTreeItem childItem = new FilterTreeItem(element);
            parentItem.getChildren().add(childItem);
        }
    }

    @FXML
    private void handleClose() {
        if (mCloseButton.getScene() != null && mCloseButton.getScene().getWindow() instanceof Stage stage) {
            stage.close();
        }
    }

    private class FilterTreeItem extends CheckBoxTreeItem<Object> {
        public FilterTreeItem(Object value) {
            super(value);

            if (value instanceof IFilter<?> filter) {
                setSelected(filter.isEnabled());
            } else if (value instanceof FilterElement<?> element) {
                setSelected(element.isEnabled());
            }

            selectedProperty().addListener((obs, oldVal, newVal) -> {
                Object itemValue = getValue();
                if (itemValue instanceof IFilter<?> filter) {
                    setFilterEnabled(this, newVal);
                } else if (itemValue instanceof FilterElement<?> element) {
                    element.setEnabled(newVal);
                }

                // Fire update on parent chain
                FilterTreeItem parent = (FilterTreeItem) getParent();
                while (parent != null) {
                    // TreeItem valueChangedEvent expects the new value as a boolean in CheckBoxTreeItem, we don't really need to fire a custom event just force a cell update if needed, but JavaFX properties should handle it.
                    parent = (FilterTreeItem) parent.getParent();
                }
            });
        }

        private void setFilterEnabled(FilterTreeItem item, boolean enabled) {
            Object obj = item.getValue();
            if (obj instanceof FilterElement<?> element) {
                element.setEnabled(enabled);
            }

            for (TreeItem<Object> child : item.getChildren()) {
                if (child instanceof FilterEditorController.FilterTreeItem) {
                    FilterTreeItem childFilterItem = (FilterTreeItem) child;
                    childFilterItem.setSelected(enabled);
                    setFilterEnabled(childFilterItem, enabled);
                }
            }
        }
    }

    private class FilterTreeCell extends CheckBoxTreeCell<Object> {
        @Override
        public void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setTooltip(null);
            } else {
                String text = "";
                String tooltipText = null;

                if (item instanceof IFilter<?> filter) {
                    text = filter.getName() + " (" + filter.getEnabledCount() + "/" + filter.getElementCount() + ")";
                    String filterName = filter.getName();
                    String tooltipContent = "Controls the enabling of elements for this specific filter set.\nBenefit: Instantly toggle related DSP components.";

                    if (filterName.equals("De-emphasis Filter")) {
                        tooltipContent = "Restores natural voice balance by reversing the pre-emphasis applied at the transmitter.\nBenefit: Significantly reduces harsh high-frequency hiss.";
                    } else if (filterName.equals("Decimation Filter")) {
                        tooltipContent = "Reduces the sample rate of the signal to lower processing requirements.\nBenefit: Vastly improves efficiency.";
                    } else if (filterName.equals("Squaring Filter")) {
                        tooltipContent = "Squares the input signal values.\nBenefit: Useful in energy detection algorithms.";
                    }

                    tooltipText = filterName + "\n" + tooltipContent;

                } else if (item instanceof FilterElement<?> element) {
                    text = element.getName();
                    String elementName = element.getName();
                    String tooltipContent = "Filter configuration element.\nCheck documentation for specific DSP effect.";

                    if (elementName.equals("De-emphasis Filter")) {
                        tooltipContent = "Restores natural voice balance by reversing the pre-emphasis applied at the transmitter.\nBenefit: Significantly reduces harsh high-frequency hiss.";
                    } else if (elementName.equals("Decimation Filter")) {
                        tooltipContent = "Reduces the sample rate of the signal to lower processing requirements.\nBenefit: Vastly improves efficiency.";
                    } else if (elementName.equals("Squaring Filter")) {
                        tooltipContent = "Squares the input signal values.\nBenefit: Useful in energy detection algorithms.";
                    }

                    tooltipText = elementName + "\n" + tooltipContent;
                } else if (item instanceof FilterSet<?> filterSet) {
                    text = filterSet.getName() + " (" + filterSet.getEnabledCount() + "/" + filterSet.getElementCount() + ")";
                } else {
                    text = item.toString();
                }

                setText(text);
                if (tooltipText != null) {
                    setTooltip(new Tooltip(tooltipText));
                } else {
                    setTooltip(null);
                }
            }
        }
    }
}
