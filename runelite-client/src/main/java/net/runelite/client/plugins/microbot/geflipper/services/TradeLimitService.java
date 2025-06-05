package net.runelite.client.plugins.microbot.geflipper.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.geflipper.GeFlipperConfig;
import net.runelite.client.plugins.microbot.geflipper.model.FlipOpportunity;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class TradeLimitService {

    private static final long TRADE_LIMIT_WINDOW = 4 * 60 * 60 * 1000; // 4 hours in milliseconds
    private static final long CLEANUP_INTERVAL = 5 * 60 * 1000; // Check every 5 minutes

    @Inject
    private GeFlipperConfig config;

    @Inject
    private PriceService priceService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<Integer, Integer> itemQuantities = new ConcurrentHashMap<>(); // itemId -> quantity bought
    private ScheduledExecutorService scheduler;

    // Global window start time - applies to ALL items
    private Long windowStartTime = null;

    // Connected item groups (items that share limits)
    private final Map<Integer, Integer> connectedItems = new HashMap<>() {{
        // Prayer potions (all doses share limit)
        put(2434, 2434); // Prayer potion(4) - base
        put(143, 2434);  // Prayer potion(3) - maps to base
        put(141, 2434);  // Prayer potion(2) - maps to base
        put(139, 2434);  // Prayer potion(1) - maps to base

        // Super combat potions
        put(12695, 12695); // Super combat potion(4) - base
        put(12697, 12695); // Super combat potion(3) - maps to base
        put(12699, 12695); // Super combat potion(2) - maps to base
        put(12701, 12695); // Super combat potion(1) - maps to base

        // To be continued...
    }};

    public void initialize() {
        loadTradeLimits();
        startCleanupScheduler();
        log.info("TradeLimitService initialized. Window start: {}, {} items tracked",
                windowStartTime != null ? new java.util.Date(windowStartTime) : "None",
                itemQuantities.size());
    }

    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            log.info("TradeLimitService scheduler stopped");
        }
    }

    /**
     * Record a purchase of items
     */
    public void recordPurchase(int itemId, int quantity) {
        int baseItemId = getBaseItemId(itemId);
        long now = System.currentTimeMillis();

        // Check if we need to start a new window or if current window has expired
        if (windowStartTime == null || (now - windowStartTime >= TRADE_LIMIT_WINDOW)) {
            // Start new 4-hour window
            if (windowStartTime != null) {
                log.info("4-hour trade limit window expired, starting new window");
            } else {
                log.info("Starting first 4-hour trade limit window");
            }
            windowStartTime = now;
            itemQuantities.clear(); // Reset all quantities
        }

        // Add to current window
        int currentQuantity = itemQuantities.getOrDefault(baseItemId, 0);
        itemQuantities.put(baseItemId, currentQuantity + quantity);

        saveTradeLimits();

        log.debug("Recorded purchase: {} x{}, total this window: {}/{}",
                itemId, quantity, itemQuantities.get(baseItemId), getItemLimit(baseItemId));
    }

    /**
     * Get remaining buy limit for an item
     */
    public int getRemainingLimit(int itemId) {
        int baseItemId = getBaseItemId(itemId);

        // Check if window has expired
        if (windowStartTime != null) {
            long now = System.currentTimeMillis();
            if (now - windowStartTime >= TRADE_LIMIT_WINDOW) {
                // Window expired, full limit available
                return getItemLimit(baseItemId);
            }
        } else {
            // No window started yet, full limit available
            return getItemLimit(baseItemId);
        }

        int quantityBought = itemQuantities.getOrDefault(baseItemId, 0);
        int itemLimit = getItemLimit(baseItemId);

        return Math.max(0, itemLimit - quantityBought);
    }

    /**
     * Get the base item ID for connected items
     */
    private int getBaseItemId(int itemId) {
        return connectedItems.getOrDefault(itemId, itemId);
    }

    /**
     * Get the buy limit for an item from PriceService
     */
    private int getItemLimit(int itemId) {
        if (!priceService.isInitialized()) {
            log.warn("PriceService not initialized, using default limit of 100 for item {}", itemId);
            return 100;
        }

        FlipOpportunity flipOpportunity = priceService.getItemPrices().get(itemId);
        if (flipOpportunity != null) {
            return flipOpportunity.getLimit();
        }

        log.debug("No limit data found for item {}, using default limit of 100", itemId);
        return 100;
    }

    /**
     * Start the cleanup scheduler
     */
    private void startCleanupScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(this::checkWindowExpiry,
                CLEANUP_INTERVAL, CLEANUP_INTERVAL, TimeUnit.MILLISECONDS);

        log.info("Started trade limit cleanup scheduler (checking every {} minutes)",
                CLEANUP_INTERVAL / (1000 * 60));
    }

    /**
     * Check if the current window has expired and reset if needed
     */
    private void checkWindowExpiry() {
        if (windowStartTime == null) return;

        long now = System.currentTimeMillis();
        if (now - windowStartTime >= TRADE_LIMIT_WINDOW) {
            log.info("4-hour trade limit window expired, all limits reset");
            itemQuantities.clear();
            windowStartTime = null;
            saveTradeLimits();
        }
    }

    /**
     * Load trade limits from config
     */
    private void loadTradeLimits() {
        try {
            String tradeLimitsJson = config.tradeLimits();
            if (tradeLimitsJson != null && !tradeLimitsJson.equals("{}")) {
                TradeLimitData data = objectMapper.readValue(tradeLimitsJson, TradeLimitData.class);

                this.windowStartTime = data.getWindowStartTime();
                this.itemQuantities.putAll(data.getItemQuantities());

                log.info("Loaded trade limits from config. Window start: {}, {} items tracked",
                        windowStartTime != null ? new java.util.Date(windowStartTime) : "None",
                        itemQuantities.size());
            }
        } catch (Exception e) {
            log.error("Failed to load trade limits from config", e);
            itemQuantities.clear();
            windowStartTime = null;
        }
    }

    /**
     * Save trade limits to config
     */
    private void saveTradeLimits() {
        try {
            TradeLimitData data = new TradeLimitData(windowStartTime, itemQuantities);
            String tradeLimitsJson = objectMapper.writeValueAsString(data);
            config.setTradeLimits(tradeLimitsJson);
        } catch (Exception e) {
            log.error("Failed to save trade limits to config", e);
        }
    }

    @Data
    @NoArgsConstructor
    public static class TradeLimitData {
        private Long windowStartTime;
        private Map<Integer, Integer> itemQuantities = new HashMap<>();

        public TradeLimitData(Long windowStartTime, Map<Integer, Integer> itemQuantities) {
            this.windowStartTime = windowStartTime;
            this.itemQuantities = new HashMap<>(itemQuantities);
        }
    }
}
