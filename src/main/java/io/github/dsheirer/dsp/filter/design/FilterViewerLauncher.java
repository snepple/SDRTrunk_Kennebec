package io.github.dsheirer.dsp.filter.design;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;

public class FilterViewerLauncher
{
    private static final Logger mLog = LoggerFactory.getLogger(FilterViewerLauncher.class);
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
                mLog.error("Error launching filter viewer", e);
            }
        });
    }

    public static void main(String[] args)
    {
        FilterViewerLauncher filterViewerLauncher = new FilterViewerLauncher();
    }
}
