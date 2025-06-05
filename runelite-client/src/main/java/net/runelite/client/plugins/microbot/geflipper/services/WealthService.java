package net.runelite.client.plugins.microbot.geflipper.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.geflipper.GeFlipperConfig;
import net.runelite.client.plugins.microbot.geflipper.managers.FlipManager;
import net.runelite.client.plugins.microbot.geflipper.model.Flip;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
@Slf4j
public class WealthService {
    @Inject
    private FlipManager flipManager;

    @Inject
    private GeFlipperConfig config;

    @Getter
    private long sessionStartActualWealth = 0;

    @Getter
    private long sessionAccumulatedProfit = 0;

    @Getter
    private long historicalAccumulatedProfit;

    @Getter
    private long sessionStartTime = 0;

    @Getter
    private boolean initialized = false;

    public void initializeSession() {
        initialized = true;
        sessionStartActualWealth = calculateActualWealth();
        sessionStartTime = System.currentTimeMillis();
        historicalAccumulatedProfit = config.historicalProfit();
        log.info("Session initialized with starting actual wealth: {} gp", sessionStartActualWealth);
    }

    public void appendSessionProfit(long profit) {
        sessionAccumulatedProfit += profit;
        historicalAccumulatedProfit += profit;
        config.setHistoricalProfit(historicalAccumulatedProfit);
    }

    public long getCurrentGold() {
        try {
            return Rs2Inventory.get(995).getQuantity(); // Coins item ID
        } catch (Exception e) {
            return 0;
        }
    }

    public long getInvestmentCapital() {
        return flipManager.getTotalInvestment();
    }
    /**
     * Actual wealth: Gold + value of active buy orders + inventory items (non-flip items)
     */
    public long calculateActualWealth() {
        long gold = getCurrentGold();
        long activeOrdersValue = calculateActiveOrdersCapital();

        return gold + activeOrdersValue;
    }

    /**
     * Calculate value that has not been / has been realized from active flips
     */
    public long calculateActiveOrdersCapital() {
        List<Flip> activeFlips = flipManager.getActiveFlips();
        long buyCapital = activeFlips.stream()
                .filter(flip -> flip.getStatus() == Flip.FlipStatus.BUYING)
                .mapToLong(flip -> (long) flip.getBuyPrice() * (flip.getQuantity() - flip.getProgression()))
                .sum();
        long sellCapital = activeFlips.stream()
                .filter(flip -> flip.getStatus() == Flip.FlipStatus.SELLING)
                .mapToLong(flip -> (long) ((long) flip.getSellPrice() * flip.getProgression() * 0.98))
                .sum();
        return buyCapital + sellCapital;
    }

    public long getSessionActualProfitLoss() {
        long currentActualWealth = calculateActualWealth();
        return currentActualWealth - sessionStartActualWealth;
    }

    public long getSessionDuration() {
        return System.currentTimeMillis() - sessionStartTime;
    }

    public String getSessionDurationString() {

        long totalSeconds = getSessionDuration() / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    public double getRealizedProfitPerHour() {
        long durationMs = getSessionDuration();
        if (durationMs == 0) return 0.0;

        double hours = durationMs / (1000.0 * 60.0 * 60.0);

        return sessionAccumulatedProfit / hours;
    }
}
