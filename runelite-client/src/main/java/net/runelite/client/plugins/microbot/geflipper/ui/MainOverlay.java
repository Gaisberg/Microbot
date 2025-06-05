package net.runelite.client.plugins.microbot.geflipper.ui;

import net.runelite.client.plugins.microbot.geflipper.GeFlipperConfig;
import net.runelite.client.plugins.microbot.geflipper.GeFlipperPlugin;
import net.runelite.client.plugins.microbot.geflipper.services.WealthService;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

public class MainOverlay extends OverlayPanel {
    private final GeFlipperConfig config;
    private final WealthService wealthService;

    @Inject
    public MainOverlay(GeFlipperPlugin plugin, GeFlipperConfig config, WealthService wealthService) {
        super(plugin);
        this.config = config;
        this.wealthService = wealthService;

        setPosition(OverlayPosition.TOP_LEFT);

        // Add menu entries
        getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "GE Flipper Overlay"));
        getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY, "Open Debug Console", "GE Flipper"));
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.enableOverlay()) {
            return null;
        }

        panelComponent.getChildren().clear();

        // Title
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("GE Flipper")
                .color(Color.GREEN)
                .build());

        // Wealth information
        long sessionProfit = wealthService.getSessionAccumulatedProfit();
        double profitPerHour = wealthService.getRealizedProfitPerHour();
        long historicalProfit = wealthService.getHistoricalAccumulatedProfit();

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Duration:")
                .right(wealthService.getSessionDurationString())
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Session:")
                .right(String.format("%+,d gp", sessionProfit))
                .rightColor(sessionProfit >= 0 ? Color.GREEN : Color.RED)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Per Hour:")
                .right(String.format("%+,.0f gp/h", profitPerHour))
                .rightColor(profitPerHour >= 0 ? Color.GREEN : Color.RED)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Total:")
                .right(String.format("%+,d gp", historicalProfit))
                .rightColor(historicalProfit >= 0 ? Color.GREEN : Color.RED)
                .build());

        return super.render(graphics);
    }
}
