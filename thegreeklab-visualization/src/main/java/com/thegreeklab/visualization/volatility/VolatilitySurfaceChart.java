package com.thegreeklab.visualization.volatility;

import com.thegreeklab.finance.time.EpochNanos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Resizable JavaFX heatmap for a sampled implied-volatility surface.
 *
 * <p>The horizontal axis is expiry, the vertical axis is
 * {@code ln(K / F(T))}, and cell color encodes annualized implied
 * volatility. Move the pointer over a cell to inspect its sampled
 * coordinates and volatility.</p>
 */
public final class VolatilitySurfaceChart extends Region {

    private static final double MINIMUM_WIDTH = 480.0;
    private static final double MINIMUM_HEIGHT = 360.0;
    private static final double LEFT_MARGIN = 90.0;
    private static final double RIGHT_MARGIN = 32.0;
    private static final double TOP_MARGIN = 48.0;
    private static final double BOTTOM_MARGIN = 84.0;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final VolatilitySurfaceGrid grid;
    private final Canvas canvas;

    /**
     * Creates a heatmap for one immutable surface grid.
     *
     * @param grid sampled surface data
     * @throws NullPointerException if {@code grid} is {@code null}
     */
    public VolatilitySurfaceChart(VolatilitySurfaceGrid grid) {
        this.grid = Objects.requireNonNull(grid, "Surface grid cannot be null.");
        this.canvas = new Canvas();
        getChildren().add(canvas);
        setMinSize(MINIMUM_WIDTH, MINIMUM_HEIGHT);
        setPrefSize(720.0, 480.0);
        setAccessibleText("Implied volatility surface heatmap");
        canvas.setOnMouseMoved(event -> updateAccessibleCell(event.getX(), event.getY()));
        widthProperty().addListener(_ -> requestLayout());
        heightProperty().addListener(_ -> requestLayout());
    }

    /**
     * Returns the immutable grid rendered by this chart.
     *
     * @return sampled surface data
     */
    @SuppressWarnings("unused")
    public VolatilitySurfaceGrid grid() {
        return grid;
    }

    /**
     * Indicates that this JavaFX region participates in parent layout.
     *
     * @return always {@code true}
     */
    @Override
    public boolean isResizable() {
        return true;
    }

    /**
     * Returns the preferred chart width.
     *
     * @param height ignored
     * @return preferred width in pixels
     */
    @Override
    protected double computePrefWidth(double height) {
        return 720.0;
    }

    /**
     * Returns the preferred chart height.
     *
     * @param width ignored
     * @return preferred height in pixels
     */
    @Override
    protected double computePrefHeight(double width) {
        return 480.0;
    }

    /**
     * Resizes the backing canvas and redraws the heatmap.
     */
    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        draw();
    }

    private void draw() {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0.0 || height <= 0.0) {
            return;
        }
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.clearRect(0.0, 0.0, width, height);
        double plotWidth = width - LEFT_MARGIN - RIGHT_MARGIN;
        double plotHeight = height - TOP_MARGIN - BOTTOM_MARGIN;
        if (plotWidth <= 0.0 || plotHeight <= 0.0) {
            return;
        }

        drawCells(graphics, plotWidth, plotHeight);
        drawAxes(graphics, plotWidth, plotHeight);
        drawLegend(graphics, plotWidth);
    }

    private void drawCells(GraphicsContext graphics, double plotWidth, double plotHeight) {
        double cellWidth = plotWidth / grid.expiryCount();
        double cellHeight = plotHeight / grid.moneynessCount();
        for (int expiryIndex = 0; expiryIndex < grid.expiryCount(); expiryIndex++) {
            for (int moneynessIndex = 0; moneynessIndex < grid.moneynessCount(); moneynessIndex++) {
                double volatility = grid.impliedVolatility(expiryIndex, moneynessIndex);
                graphics.setFill(colorFor(volatility));
                graphics.fillRect(
                        LEFT_MARGIN + expiryIndex * cellWidth,
                        TOP_MARGIN + (grid.moneynessCount() - 1 - moneynessIndex) * cellHeight,
                        cellWidth + 0.5,
                        cellHeight + 0.5
                );
            }
        }
    }

    private void drawAxes(GraphicsContext graphics, double plotWidth, double plotHeight) {
        graphics.setStroke(Color.BLACK);
        graphics.setLineWidth(1.0);
        graphics.strokeRect(LEFT_MARGIN, TOP_MARGIN, plotWidth, plotHeight);
        graphics.setFill(Color.BLACK);
        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.setTextBaseline(VPos.TOP);
        graphics.fillText("Expiry", LEFT_MARGIN + plotWidth / 2.0, TOP_MARGIN + plotHeight + 48.0);

        graphics.save();
        graphics.translate(22.0, TOP_MARGIN + plotHeight / 2.0);
        graphics.rotate(-90.0);
        graphics.fillText("ln(K / F(T))", 0.0, 0.0);
        graphics.restore();

        drawExpiryLabels(graphics, plotWidth, plotHeight);
        drawMoneynessLabels(graphics, plotHeight);
    }

    private void drawExpiryLabels(GraphicsContext graphics, double plotWidth, double plotHeight) {
        for (int index : labelIndices(grid.expiryCount())) {
            double coordinate = LEFT_MARGIN + (index + 0.5) * plotWidth / grid.expiryCount();
            graphics.setTextAlign(TextAlignment.CENTER);
            graphics.fillText(
                    DATE_FORMAT.format(EpochNanos.toUtc(grid.expiryTimestampsNanos().get(index))),
                    coordinate,
                    TOP_MARGIN + plotHeight + 14.0
            );
        }
    }

    private void drawMoneynessLabels(GraphicsContext graphics, double plotHeight) {
        for (int index : labelIndices(grid.moneynessCount())) {
            double coordinate = TOP_MARGIN
                    + (grid.moneynessCount() - index - 0.5) * plotHeight / grid.moneynessCount();
            graphics.setTextAlign(TextAlignment.RIGHT);
            graphics.fillText(String.format("%.2f", grid.logStrikeToForwards().get(index)), LEFT_MARGIN - 8.0, coordinate - 6.0);
        }
    }

    private void drawLegend(GraphicsContext graphics, double plotWidth) {
        double legendX = LEFT_MARGIN + plotWidth - 120.0;
        double legendY = TOP_MARGIN + 12.0;
        double legendWidth = 96.0;
        double legendHeight = 10.0;
        for (int index = 0; index < 96; index++) {
            double normalized = index / 95.0;
            graphics.setFill(colorForNormalized(normalized));
            graphics.fillRect(legendX + index, legendY, 1.0, legendHeight);
        }
        graphics.setFill(Color.BLACK);
        graphics.setTextAlign(TextAlignment.LEFT);
        graphics.fillText(String.format("%.2f%%", grid.minimumVolatility() * 100.0), legendX, legendY + 14.0);
        graphics.setTextAlign(TextAlignment.RIGHT);
        graphics.fillText(
                String.format("%.2f%%", grid.maximumVolatility() * 100.0),
                legendX + legendWidth,
                legendY + 14.0
        );
    }

    private Color colorFor(double volatility) {
        double minimum = grid.minimumVolatility();
        double maximum = grid.maximumVolatility();
        double normalized = maximum == minimum ? 0.5 : (volatility - minimum) / (maximum - minimum);
        return colorForNormalized(normalized);
    }

    private Color colorForNormalized(double normalized) {
        return Color.hsb(225.0 - 225.0 * Math.clamp(normalized, 0.0, 1.0), 0.72, 0.88);
    }

    private void updateAccessibleCell(double x, double y) {
        double plotWidth = getWidth() - LEFT_MARGIN - RIGHT_MARGIN;
        double plotHeight = getHeight() - TOP_MARGIN - BOTTOM_MARGIN;
        if (plotWidth <= 0.0 || plotHeight <= 0.0
                || x < LEFT_MARGIN || x >= LEFT_MARGIN + plotWidth
                || y < TOP_MARGIN || y >= TOP_MARGIN + plotHeight) {
            return;
        }
        int expiryIndex = Math.min(
                grid.expiryCount() - 1, (int) ((x - LEFT_MARGIN) / plotWidth * grid.expiryCount())
        );
        int moneynessFromTop = Math.min(
                grid.moneynessCount() - 1, (int) ((y - TOP_MARGIN) / plotHeight * grid.moneynessCount())
        );
        int moneynessIndex = grid.moneynessCount() - 1 - moneynessFromTop;
        setAccessibleText(String.format(
                "Expiry %s, log strike to forward %.4f, implied volatility %.2f percent",
                DATE_FORMAT.format(EpochNanos.toUtc(grid.expiryTimestampsNanos().get(expiryIndex))),
                grid.logStrikeToForwards().get(moneynessIndex),
                grid.impliedVolatility(expiryIndex, moneynessIndex) * 100.0
        ));
    }

    private static int[] labelIndices(int count) {
        if (count <= 5) {
            int[] indices = new int[count];
            for (int index = 0; index < count; index++) {
                indices[index] = index;
            }
            return indices;
        }
        return new int[]{0, count / 2, count - 1};
    }
}
