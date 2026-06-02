



package io.github.dsheirer.gui.widget;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.layout.Region;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;


import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class Widget extends VBox {

    private final String mId;
    private final String mTitle;
    private final Region mContentComponent;
    private final HBox mHeaderPanel;
    private final Label mTitleLabel;
    private final Button mMinimizeButton;
    private final Button mCloseButton;
    private final WidgetContainer mContainer;
    private final int mMinHeight;
    private final VBox mResizeHandle;

    private boolean mMinimized = false;

    public Widget(String id, String title, Region contentComponent, WidgetContainer container, int minHeight) {
        mId = id;
        setId(id);
        mTitle = title;
        mContentComponent = contentComponent;
        mContainer = container;
        mMinHeight = minHeight;

        this.setMinWidth(0);

        setBackground(javafx.scene.layout.Background.EMPTY);
        // setLayout(new javafx.scene.layout.HBox(4));
        // setBorder(null.createEmptyBorder(0, 0, 0, 0));

        mHeaderPanel = new HBox(5);
        mHeaderPanel.setAlignment(Pos.CENTER_LEFT);
        mHeaderPanel.setPadding(new javafx.geometry.Insets(2, 5, 2, 5));
        mHeaderPanel.setBackground(javafx.scene.layout.Background.EMPTY);

        mTitleLabel = new Label(title);
        mTitleLabel.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12));
        mTitleLabel.setTextFill(javafx.scene.paint.Color.GRAY);
        mHeaderPanel.getChildren().add(mTitleLabel);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        mHeaderPanel.getChildren().add(spacer);

        mMinimizeButton = createHeaderButton();
        mMinimizeButton.setTooltip(new javafx.scene.control.Tooltip("Minimize"));
        mMinimizeButton.accessibleTextProperty().set("Minimize Widget");
        mMinimizeButton.accessibleHelpProperty().set("Minimizes or expands the widget");
        mMinimizeButton.setOnAction(e -> toggleMinimized());
        mHeaderPanel.getChildren().add(mMinimizeButton);

        mCloseButton = createHeaderButton();
        mCloseButton.setTooltip(new javafx.scene.control.Tooltip("Close"));
        mCloseButton.accessibleTextProperty().set("Close Widget");
        mCloseButton.accessibleHelpProperty().set("Closes the widget");
        mCloseButton.setOnAction(e -> closeWidget());
        mHeaderPanel.getChildren().add(mCloseButton);

        updateIcons();

        getChildren().add(mHeaderPanel);

        int savedHeight = minHeight;
        if (container != null && container.getPreference() != null) {
            savedHeight = container.getPreference().getWidgetHeight(id, minHeight);
        }
        if (savedHeight > 0) {
            mContentComponent.setPrefHeight(savedHeight);
        }
        if (minHeight > 0) {
            mContentComponent.setMinHeight(minHeight);
        }
        mContentComponent.setMaxWidth(Double.MAX_VALUE);
        mContentComponent.setMinWidth(0);
        mHeaderPanel.setMinWidth(0);
        VBox.setVgrow(mContentComponent, Priority.ALWAYS);
        getChildren().add(mContentComponent);
        mResizeHandle = new VBox();
        mResizeHandle.setCursor(javafx.scene.Cursor.S_RESIZE);
        mResizeHandle.setBackground(new javafx.scene.layout.Background(new javafx.scene.layout.BackgroundFill(javafx.scene.paint.Color.GRAY, javafx.scene.layout.CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY)));
        mResizeHandle.setPrefSize(0, 4);
        mResizeHandle.setOnMousePressed(e -> {
            mResizeHandle.getProperties().put("startY", e.getScreenY());
            mResizeHandle.getProperties().put("startHeight", mContentComponent.getHeight());
        });
        mResizeHandle.setOnMouseDragged(e -> {
            double startY = (double) mResizeHandle.getProperties().get("startY");
            double startHeight = (double) mResizeHandle.getProperties().get("startHeight");
            double delta = e.getScreenY() - startY;
            double newHeight = Math.max(mMinHeight, startHeight + delta);
            mContentComponent.setPrefSize(mContentComponent.getWidth(), newHeight);
            requestLayout();
            if (mContainer != null) mContainer.requestLayout();
        });
        mResizeHandle.setOnMouseReleased(e -> {
            if (mContainer != null && mContainer.getPreference() != null) {
                mContainer.getPreference().setWidgetHeight(mId, (int)mContentComponent.getHeight());
            }
        });
        getChildren().add(mResizeHandle);
    }

    private Button createHeaderButton() {
        Button button = new Button();
        button.setPadding(new javafx.geometry.Insets(0, 4, 0, 4));
        button.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        return button;
    }

    public void setCloseButtonVisible(boolean visible) {
        mCloseButton.setVisible(visible);
    }

    public void setMinimizeButtonVisible(boolean visible) {
        mMinimizeButton.setVisible(visible);
    }

    public boolean isMinimizeButtonVisible() {
        return mMinimizeButton.isVisible();
    }

    public String getWidgetId() {
        return mId;
    }

    public HBox getHeaderPanel() {
        return mHeaderPanel;
    }

    public void ensureContentComponentParent() {
        if (mContentComponent != null && mContentComponent.getParent() != this) {
            getChildren().add(1, mContentComponent);
            requestLayout();
            requestLayout();
        }
    }

    public void setDragging(boolean dragging) {
        if (dragging) {
            mHeaderPanel.setBackground(new javafx.scene.layout.Background(new javafx.scene.layout.BackgroundFill(javafx.scene.paint.Color.GRAY, javafx.scene.layout.CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY)));
            ;
        } else {
            mHeaderPanel.setBackground(javafx.scene.layout.Background.EMPTY);
            setBorder(null);
        }
        requestLayout();
    }

    public void setMinimized(boolean minimized) {
        mMinimized = minimized;
        mContentComponent.setVisible(!minimized);
        mContentComponent.setManaged(!minimized);
        updateIcons();
        mMinimizeButton.setTooltip(new javafx.scene.control.Tooltip(minimized ? "Expand" : "Minimize"));
        mMinimizeButton.accessibleTextProperty().set(minimized ? "Expand Widget" : "Minimize Widget");
        requestLayout();
        requestLayout();
    }

    public boolean isMinimized() {
        return mMinimized;
    }

    private void toggleMinimized() {
        setMinimized(!mMinimized);
        if (mContainer != null) {
            mContainer.onWidgetStateChanged(this);
        }
    }

    private void closeWidget() {
        if (mContainer != null) {
            mContainer.hideWidget(mId);
        }
    }

    // // @Override
    // protected void paintComponent(Graphics g) {
    //     super.paintComponent(g);
    //     Graphics2D g2 = (Graphics2D) g.create();
    //     g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    //     int arc = 12;
    //     Shape cardShape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

    //     g2.setColor(javafx.scene.paint.Color.GRAY);
    //     g2.fill(cardShape);

    //     g2.setColor(new Color(0, 0, 0, 25)); // 10% opacity black
    //     g2.setStroke(new BasicStroke(1.0f));
    //     g2.draw(cardShape);

    //     g2.dispose();
    // }

    // // @Override
    // // public void updateUI() {
    //     super.updateUI();
    //     updateIcons();
    // }

    private void updateIcons() {
        if (mMinimizeButton != null) {
            jiconfont.javafx.IconNode icon = new jiconfont.javafx.IconNode(mMinimized ? FontAwesome.PLUS_SQUARE_O : FontAwesome.MINUS_SQUARE_O);
            icon.setIconSize(14);
            icon.setFill(javafx.scene.paint.Color.GRAY);
            mMinimizeButton.setGraphic(icon);
        }
        if (mCloseButton != null) {
            jiconfont.javafx.IconNode icon = new jiconfont.javafx.IconNode(FontAwesome.TIMES);
            icon.setIconSize(14);
            icon.setFill(javafx.scene.paint.Color.GRAY);
            mCloseButton.setGraphic(icon);
        }
    }
}
