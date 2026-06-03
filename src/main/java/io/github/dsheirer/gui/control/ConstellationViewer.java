

package io.github.dsheirer.gui.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.control.Slider;

import javafx.scene.control.Button;
import javafx.scene.control.Label;

import io.github.dsheirer.buffer.CircularBuffer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.Complex;
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;

import org.apache.commons.math3.util.FastMath;







import java.util.List;
import javafx.animation.AnimationTimer;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConstellationViewer extends javafx.scene.layout.Pane implements Listener<Complex> {
    private static final Logger mLog = LoggerFactory.getLogger(ConstellationViewer.class);
    private static final long serialVersionUID = 1L;

    private int mSampleRate;
    private int mSymbolRate;
    private float mSamplesPerSymbol;
    private float mCounter = 0;
    private float mOffset = 0;
    private CircularBuffer<Complex> mBuffer = new CircularBuffer<>(5000);
    private Complex mPrevious = new Complex(1, 1);

    private ConstellationViewerController controller;
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public ConstellationViewer(int sampleRate, int symbolRate) {
        mSampleRate = sampleRate;
        mSymbolRate = symbolRate;
        mSamplesPerSymbol = (float) mSampleRate / (float) mSymbolRate;

        initGui();
    }

    private void initGui() {
        setPrefSize(100, 100); // new Dimension(200, 200));

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/dsheirer/gui/control/ConstellationViewer.fxml"));
                StackPane root = loader.load();
                controller = loader.getController();

                controller.rootPane.widthProperty().addListener((obs, oldVal, newVal) -> {
                    controller.fxCanvas.setWidth(newVal.doubleValue());
                    dirty.set(true);
                });
                controller.rootPane.heightProperty().addListener((obs, oldVal, newVal) -> {
                    controller.fxCanvas.setHeight(newVal.doubleValue());
                    dirty.set(true);
                });

                Scene scene = new Scene(root);
                scene.setFill(null);
                // setScene(scene);
                setBackground(javafx.scene.layout.Background.EMPTY);

                AnimationTimer timer = new AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        if (dirty.compareAndSet(true, false)) {
                            drawConstellation();
                        }
                    }
                };
                timer.start();
            } catch (Exception e) {
                mLog.error("Error in constellation viewer", e);
            }
        });

        setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                ContextMenu menu = new ContextMenu();
                menu.getItems().add(new javafx.scene.control.CustomMenuItem(new TimingOffsetItem((int) (mSamplesPerSymbol * 10), (int) (mOffset * 10))));
                menu.show(ConstellationViewer.this, e.getScreenX(), e.getScreenY());
            }
        });
    }

    @Override
    public void receive(Complex sample) {
        mBuffer.receive(sample);
        Complex angle = Complex.multiply(sample, mPrevious.conjugate());
        dirty.set(true);
    }

    public void setOffset(float offset) {
        mOffset = offset;
        dirty.set(true);
    }

    private void drawConstellation() {
        if (controller == null || controller.fxCanvas == null) return;

        GraphicsContext gc = controller.fxCanvas.getGraphicsContext2D();

        gc.clearRect(0, 0, controller.fxCanvas.getWidth(), controller.fxCanvas.getHeight());
        gc.setFill(Color.BLUE);

        List<Complex> samples = mBuffer.getElements();

        double centerX = controller.fxCanvas.getHeight() / 2.0d;
        double centerY = controller.fxCanvas.getWidth() / 2.0d;

        double scale = 0.5d;
        mCounter = 0;

        for (Complex sample : samples) {
            if (mCounter > (mOffset + mSamplesPerSymbol)) {
                double i = (sample.inphase() * mPrevious.inphase()) -
                        (sample.quadrature() * -mPrevious.quadrature());
                double q = (sample.quadrature() * mPrevious.inphase()) +
                        (sample.inphase() * -mPrevious.quadrature());

                double angle;

                if (i == 0) {
                    angle = 0.0;
                } else {
                    double denominator = 1.0d / i;
                    angle = FastMath.atan((double) q * denominator);
                }

                double ovalX = centerX - (i * scale) - 2;
                double ovalY = centerY - (q * scale) - 2;

                gc.strokeOval(ovalX, ovalY, 4, 4);

                mCounter -= mSamplesPerSymbol;
            }

            mCounter++;
        }
    }

    public class TimingOffsetItem extends Slider {
        private static final long serialVersionUID = 1L;

        public TimingOffsetItem(int maxValue, int currentValue) {
            super(0, maxValue, currentValue);

            setMajorTickUnit(10);
            setMinorTickCount(2);
            setShowTickMarks(true);
            setShowTickLabels(true);

            valueProperty().addListener((obs, oldVal, newVal) -> stateChanged(newVal));
        }

        private void stateChanged(Number newVal) {
            int value = newVal.intValue();
            setOffset((float) value / 10.0f);
        }
    }
}
