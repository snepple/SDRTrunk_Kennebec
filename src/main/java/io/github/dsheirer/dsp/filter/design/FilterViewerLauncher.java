package io.github.dsheirer.dsp.filter.design;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;

public class FilterViewerLauncher
{
    private javafx.scene.layout.Pane mJFXPanel;

    public FilterViewerLauncher()
    {
        mJFXPanel = new javafx.scene.layout.Pane();

        Platform.runLater(() ->
        {
            FilterViewer filterViewer = new FilterViewer();
            Stage stage = new Stage();

            try
            {
                filterViewer.start(stage);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args)
    {
        FilterViewerLauncher filterViewerLauncher = new FilterViewerLauncher();
    }
}
