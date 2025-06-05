package net.runelite.client.plugins.microbot.geflipper.ui;

import net.runelite.client.plugins.microbot.geflipper.GeFlipperConfig;
import net.runelite.client.plugins.microbot.geflipper.GeFlipperPlugin;
import net.runelite.client.plugins.microbot.geflipper.managers.FlipManager;
import net.runelite.client.plugins.microbot.geflipper.model.Flip;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;
import java.util.List;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

public class FlipsOverlay extends Overlay {
    private final FlipManager flipManager;
    private final GeFlipperConfig config;

    private static final int COLUMN_WIDTH = 170;
    private static final int ROW_HEIGHT = 20;
    private static final int HEADER_HEIGHT = 25;
    private static final int FLIPS_PER_COLUMN = 3;
    private static final int MAX_COLUMNS = 3;
    private static final int PADDING = 5;

    @Inject
    public FlipsOverlay(GeFlipperPlugin plugin, GeFlipperConfig config, FlipManager flipManager) {
        this.flipManager = flipManager;
        this.config = config;

        setPosition(OverlayPosition.BOTTOM_LEFT);
        getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "GE Flipper Active Flips"));
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.enableOverlay()) {
            return null;
        }

        List<Flip> activeFlips = flipManager.getActiveFlips();
        if (activeFlips.isEmpty()) {
            return null;
        }

        int flipsToShow = Math.min(activeFlips.size(), FLIPS_PER_COLUMN * MAX_COLUMNS);
        int columnsNeeded = Math.min(MAX_COLUMNS, (int) Math.ceil((double) flipsToShow / FLIPS_PER_COLUMN));

        int totalWidth = columnsNeeded * COLUMN_WIDTH + (columnsNeeded - 1) * PADDING;
        int totalHeight = HEADER_HEIGHT + (FLIPS_PER_COLUMN * ROW_HEIGHT * 7) + PADDING * 2; // 7 rows per flip

        // Set up graphics
        graphics.setFont(FontManager.getRunescapeSmallFont());
        FontMetrics fm = graphics.getFontMetrics();

        // Draw background
        graphics.setColor(new Color(0, 0, 0, 180));
        graphics.fillRect(0, 0, totalWidth, totalHeight);

        // Draw border
        graphics.setColor(Color.GRAY);
        graphics.drawRect(0, 0, totalWidth - 1, totalHeight - 1);

        // Draw title
        graphics.setColor(Color.CYAN);
        String title = "Active Flips (" + activeFlips.size() + ")";
        int titleX = (totalWidth - fm.stringWidth(title)) / 2;
        graphics.drawString(title, titleX, HEADER_HEIGHT - 5);

        // Draw column headers
        for (int col = 0; col < columnsNeeded; col++) {
            int colX = col * (COLUMN_WIDTH + PADDING) + PADDING;

            // Draw column separator
            if (col < columnsNeeded - 1) {
                int separatorX = colX + COLUMN_WIDTH + PADDING / 2 - 5;
                graphics.drawLine(separatorX, HEADER_HEIGHT, separatorX, totalHeight);
            }
        }

        // Draw flips
        for (int col = 0; col < columnsNeeded; col++) {
            int colX = col * (COLUMN_WIDTH + PADDING) + PADDING;

            for (int row = 0; row < FLIPS_PER_COLUMN; row++) {
                int flipIndex = col * FLIPS_PER_COLUMN + row;
                if (flipIndex >= flipsToShow) break;

                Flip flip = activeFlips.get(flipIndex);
                int baseY = HEADER_HEIGHT + 20 + (row * ROW_HEIGHT * 7);

                drawFlip(graphics, flip, colX, baseY, COLUMN_WIDTH);

                // Draw separator line between flips
                if (row < FLIPS_PER_COLUMN - 1 && flipIndex + 1 < flipsToShow) {
                    graphics.setColor(Color.GRAY);
                    graphics.drawLine(colX, baseY + ROW_HEIGHT * 7 - ROW_HEIGHT / 2 - 5,
                            colX + COLUMN_WIDTH - 10, baseY + ROW_HEIGHT * 7 - ROW_HEIGHT / 2 - 5);
                }
            }
        }

        return new Dimension(totalWidth, totalHeight);
    }

    private void drawFlip(Graphics2D graphics, Flip flip, int x, int y, int width) {
        FontMetrics fm = graphics.getFontMetrics();
        int lineHeight = ROW_HEIGHT;

        // Item name
        graphics.setColor(Color.WHITE);
        String itemName = truncateString(flip.getItemName(), 20);
        graphics.drawString(itemName + " (" + flip.getItemId() + ")", x, y);
        y += lineHeight;

        // Buy price
        graphics.setColor(Color.YELLOW);
        graphics.drawString("Buy: " + formatPrice(flip.getBuyPrice()), x, y);
        y += lineHeight;

        // Sell price
        graphics.setColor(Color.YELLOW);
        graphics.drawString("Sell: " + formatPrice(flip.getSellPrice()), x, y);
        y += lineHeight;

        // Status
        Color statusColor = getStatusColor(flip.getStatus());
        graphics.setColor(statusColor);
        String status = flip.getStatus().toString();
        status += " (" + flip.getProgression() + "/" + flip.getQuantity() + ")";
        graphics.drawString(truncateString(status, 25), x, y);
        y += lineHeight;

        // Slot
        graphics.setColor(Color.WHITE);
        graphics.drawString("Slot: " + flip.getGeSlot(), x, y);
        y += lineHeight;

        // Profit
        int profit = flip.getProfit();
        graphics.setColor(profit >= 0 ? Color.GREEN : Color.RED);
        String profitText = (profit >= 0 ? "+" : "") + formatPrice(profit);
        graphics.drawString("Expected Profit: " + profitText, x, y);
        y += lineHeight;

        // Time (if available)
        if (flip.getBuyTime() != null) {
            graphics.setColor(Color.LIGHT_GRAY);
            long timeElapsed = System.currentTimeMillis() - flip.getBuyTime();
            graphics.drawString("Time: " + formatDuration(timeElapsed), x, y);
        }
    }

    private Color getStatusColor(Flip.FlipStatus status) {
        switch (status) {
            case PENDING: return Color.GRAY;
            case BUYING:
            case BOUGHT:
            case PENDING_SALE: return Color.WHITE;
            case SELLING: return Color.ORANGE;
            case SOLD: return Color.GREEN;
            default: return Color.WHITE;
        }
    }

    private String truncateString(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    private String formatPrice(int price) {
        if (price >= 1000000) {
            return String.format("%.1fM", price / 1000000.0);
        } else if (price >= 1000) {
            return String.format("%.1fK", price / 1000.0);
        } else {
            return String.valueOf(price);
        }
    }

    private String formatDuration(long durationMs) {
        if (durationMs <= 0) return "0s";

        long totalSeconds = durationMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else if (minutes > 0) {
            return minutes + "m";
        } else {
            return totalSeconds + "s";
        }
    }
}
