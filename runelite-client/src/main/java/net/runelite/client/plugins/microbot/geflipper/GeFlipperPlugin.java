package net.runelite.client.plugins.microbot.geflipper;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.*;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.grandexchange.GrandExchangePlugin;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.geflipper.managers.FlipManager;
import net.runelite.client.plugins.microbot.geflipper.managers.StrategyManager;
import net.runelite.client.plugins.microbot.geflipper.ui.FlipsOverlay;
import net.runelite.client.plugins.microbot.geflipper.ui.MainOverlay;
import net.runelite.client.plugins.microbot.geflipper.services.*;
import net.runelite.client.plugins.microbot.geflipper.ui.Panel;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.lang.reflect.Type;

@PluginDescriptor(
        name = "GE Flipper",
        description = "Automated Grand Exchange item flipping",
        tags = {"microbot", "ge", "grand exchange", "flipping", "trading"},
        enabledByDefault = false
)
@Slf4j
public class GeFlipperPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private GeFlipperConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private KeyManager keyManager;

    @Inject
    private MainOverlay mainOverlay;

    @Inject
    private FlipsOverlay flipsOverlay;

    @Inject
    @Getter
    private PriceService priceService;

    @Inject
    @Getter
    private TradeLimitService tradeLimitService;

    @Inject
    @Getter
    private GeService geService;

    @Inject
    @Getter
    private WealthService wealthService;

    @Inject
    @Getter
    private FlipManager flipManager;

    @Inject
    private FlipAnalyzer flipAnalyzer;

    private Panel panel;
    private NavigationButton navigationButton;

    @Inject
    private ClientToolbar clientToolbar;


    @Inject
    @Getter
    private StrategyManager strategyManager;

    @Inject
    @Getter
    private JavaScriptEvaluator evaluator;

    @Inject
    private GeFlipperScript script;

    @Getter
    @Setter
    private boolean paused = false;

    @Provides
    GeFlipperConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(GeFlipperConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        if (overlayManager != null) {
            overlayManager.add(mainOverlay);
            overlayManager.add(flipsOverlay);
        }

        // Create UI
        panel = new Panel(this);

        final BufferedImage icon = ImageUtil.loadImageResource(GrandExchangePlugin.class, "ge_icon.png");
        navigationButton = NavigationButton.builder()
                .tooltip("GE Flipper")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navigationButton);

        priceService.initialize();
        tradeLimitService.initialize();

        if (Microbot.isLoggedIn()) {
            strategyManager.initialize();
        }

        // Initialize services
        flipManager.loadFlips();

        script.run(this);

        log.info("GE Flipper plugin started");
    }

    @Override
    protected void shutDown() throws Exception {
        if (overlayManager != null) {
            overlayManager.remove(mainOverlay);
            overlayManager.remove(flipsOverlay);
        }

        if (navigationButton != null) {
            clientToolbar.removeNavigation(navigationButton);
        }

        // Stop services
        flipManager.saveFlips();
        tradeLimitService.shutdown();

        script.shutdown();

        // Log final stats
        if (wealthService.isInitialized()) {
            long sessionProfit = wealthService.getSessionActualProfitLoss();
            double profitPerHour = wealthService.getRealizedProfitPerHour();
            log.info("Session ended - Profit: {} gp ({} gp/h)", sessionProfit, profitPerHour);
        }

        log.info("GE Flipper plugin stopped");
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (event.getGroup().equals("geflipperconfig")) {
            if (!event.getKey().equals("activeFlips")
                    && !event.getKey().equals("tradeLimits")
                    && !event.getKey().equals("historicalProfit")
            ) {
                strategyManager.initialize();
            }
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGGED_IN) {
            strategyManager.initialize();
        }
    }


    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getContainerId() == InventoryID.INV && !wealthService.isInitialized()) {
            wealthService.initializeSession();
        }
    }

    public <T> T getConfiguration(String key, Type clazz) {
        return configManager.getConfiguration("geflipperconfig", key, clazz);
    }

    public void setConfiguration(String key, Object value) {
        configManager.setConfiguration("geflipperconfig", key, value);
    }
}
