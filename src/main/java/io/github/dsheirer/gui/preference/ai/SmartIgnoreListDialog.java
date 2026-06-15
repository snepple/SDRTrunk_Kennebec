package io.github.dsheirer.gui.preference.ai;

import io.github.dsheirer.preference.ai.ToneDiscoveryManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Set;

public class SmartIgnoreListDialog extends Dialog<Void> {

    public SmartIgnoreListDialog(Window owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("AI Smart Ignore List");
        setHeaderText("Manage automatically ignored two-tone paging frequencies.");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setPrefWidth(400);

        Label desc = new Label("These tone sequences were discovered by AI, but were deleted from the playlist. They are currently ignored. Removing them from this list will allow the AI to discover them again.");
        desc.setWrapText(true);
        content.getChildren().add(desc);

        ListView<String> listView = new ListView<>();
        ObservableList<String> items = FXCollections.observableArrayList();
        
        ToneDiscoveryManager manager = ToneDiscoveryManager.getInstance();
        if (manager != null) {
            Set<String> finalizedTones = manager.getFinalizedTones();
            items.addAll(finalizedTones);
        }
        
        listView.setItems(items);
        
        listView.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(10);
                    String[] parts = item.split("_");
                    String display = item;
                    if (parts.length == 2) {
                        display = "Tone A: " + parts[0] + " Hz | Tone B: " + parts[1] + " Hz";
                    }
                    if (manager != null) {
                        String name = manager.getFinalizedToneName(item);
                        if (name != null && !name.trim().isEmpty()) {
                            display = name + " (" + display + ")";
                        }
                    }
                    Label label = new Label(display);
                    
                    Pane spacer = new Pane();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    
                    Button removeBtn = new Button("Un-Ignore");
                    removeBtn.setOnAction(e -> {
                        if (manager != null) {
                            manager.unignoreTone(item);
                            items.remove(item);
                        }
                    });
                    
                    hbox.getChildren().addAll(label, spacer, removeBtn);
                    setGraphic(hbox);
                }
            }
        });

        content.getChildren().add(listView);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }
}
