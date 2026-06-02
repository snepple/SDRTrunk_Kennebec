package io.github.dsheirer.gui.hud;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TacticalHUD extends HBox {
    private static final Logger mLog = LoggerFactory.getLogger(TacticalHUD.class);
    private final Label mTunerStatus;
    private final Label mStreamStatus;
    private final Label mCpuStatus;
    private final Label mMemStatus;
    private final Circle mTunerDot;
    private final Circle mStreamDot;
    private final Circle mCpuDot;
    private final Circle mMemDot;
    
    public TacticalHUD() {
        setSpacing(12); setAlignment(Pos.CENTER_LEFT); setPadding(new Insets(4, 8, 4, 8));
        setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 16;");
        mTunerDot = new Circle(5, Color.GRAY); mTunerStatus = createPill("TUNER");
        mStreamDot = new Circle(5, Color.GRAY); mStreamStatus = createPill("STREAM");
        mCpuDot = new Circle(5, Color.GRAY); mCpuStatus = createPill("CPU");
        mMemDot = new Circle(5, Color.GRAY); mMemStatus = createPill("MEM");
        getChildren().addAll(mTunerDot, mTunerStatus, mStreamDot, mStreamStatus, mCpuDot, mCpuStatus, mMemDot, mMemStatus);
    }

    private Label createPill(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: white; -fx-font-size: 10; -fx-font-weight: bold;");
        return l;
    }

    public void updateTuner(boolean connected, String name) {
        javafx.application.Platform.runLater(() -> {
            mTunerDot.setFill(connected ? Color.LIMEGREEN : Color.RED);
            mTunerStatus.setText(connected ? "TUNER: " + name : "NO TUNER");
        });
    }

    public void updateStream(boolean active, int count) {
        javafx.application.Platform.runLater(() -> {
            mStreamDot.setFill(active ? Color.LIMEGREEN : Color.ORANGE);
            mStreamStatus.setText("STREAM: " + count);
        });
    }

    public void updateCpu(double percent) {
        javafx.application.Platform.runLater(() -> {
            Color c = percent > 80 ? Color.RED : percent > 50 ? Color.ORANGE : Color.LIMEGREEN;
            mCpuDot.setFill(c); mCpuStatus.setText(String.format("CPU: %.0f%%", percent));
        });
    }

    public void updateMemory(double percent) {
        javafx.application.Platform.runLater(() -> {
            Color c = percent > 80 ? Color.RED : percent > 50 ? Color.ORANGE : Color.LIMEGREEN;
            mMemDot.setFill(c); mMemStatus.setText(String.format("MEM: %.0f%%", percent));
        });
    }
}
