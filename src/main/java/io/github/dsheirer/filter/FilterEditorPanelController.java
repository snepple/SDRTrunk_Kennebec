package io.github.dsheirer.filter;

import javafx.fxml.FXML;
import javafx.scene.control.TreeView;

public class FilterEditorPanelController {

    @FXML
    private TreeView<Object> treeView;

    public TreeView<Object> getTreeView() {
        return treeView;
    }
}
