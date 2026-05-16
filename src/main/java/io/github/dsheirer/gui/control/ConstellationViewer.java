package io.github.dsheirer.gui.control;

import io.github.dsheirer.buffer.CircularBuffer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.Complex;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.apache.commons.math3.util.FastMath;

import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javafx.animation.AnimationTimer;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConstellationViewer extends JFXPanel implements Listener<Complex> {
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
        setPreferredSize(new Dimension(200, 200));

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
                setScene(scene);
                setBackground(new java.awt.Color(0, 0, 0, 0));

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
                e.printStackTrace();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    JPopupMenu menu = new JPopupMenu();
                    menu.add(new TimingOffsetItem((int) (mSamplesPerSymbol * 10), (int) (mOffset * 10)));
                    menu.show(ConstellationViewer.this, e.getX(), e.getY());
                }
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

    public class TimingOffsetItem extends JSlider implements ChangeListener {
        private static final long serialVersionUID = 1L;

        public TimingOffsetItem(int maxValue, int currentValue) {
            super(JSlider.HORIZONTAL, 0, maxValue, currentValue);

            setMajorTickSpacing(10);
            setMinorTickSpacing(5);
            setPaintTicks(true);
            setPaintLabels(true);

            addChangeListener(this);
        }

        @Override
        public void stateChanged(ChangeEvent event) {
            int value = ((JSlider) event.getSource()).getValue();
            setOffset((float) value / 10.0f);
        }
    }
}
