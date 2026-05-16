package io.github.dsheirer.module.decode.event.filter;

import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.filter.FilterEditor;
import javafx.application.Platform;
import javafx.scene.control.Button;

public class EventFilterButton<T> extends Button {

    private FilterSet<T> mFilterSet;
    private String mDialogTitle;

    public EventFilterButton(String dialogTitle, FilterSet<T> filterSet) {
        this("Filter", dialogTitle, filterSet);
    }

    public EventFilterButton(String buttonLabel, String dialogTitle, FilterSet<T> filterSet) {
        super(buttonLabel);
        mDialogTitle = dialogTitle;
        mFilterSet = filterSet;

        setOnAction(e -> {
            Platform.runLater(() -> {
                FilterEditor<T> editor = new FilterEditor<>(mDialogTitle, null, mFilterSet);
                editor.show();
            });
        });
    }
}
