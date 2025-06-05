package net.runelite.client.plugins.microbot.geflipper.managers;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.geflipper.GeFlipperConfig;
import net.runelite.client.plugins.microbot.geflipper.model.FlipOpportunity;
import net.runelite.client.plugins.microbot.geflipper.services.JavaScriptEvaluator;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;

@Singleton
@Slf4j
public class StrategyManager {

    @Inject
    private GeFlipperConfig config;

    @Inject
    private JavaScriptEvaluator evaluator;

    @Getter
    private int flipSlots = 0;
    @Getter
    private int maxInvestment = 0;
    @Getter
    private int underCutPercentage;
    @Getter
    private int maxBuyTime;
    @Getter
    private int maxFlipDuration;

    // Filtering criteria
    @Getter
    private boolean minRoiEnabled;
    @Getter
    private int minROI;
    @Getter
    private boolean maxRoiEnabled;
    @Getter
    private int maxROI;
    @Getter
    private boolean minVolumeEnabled;
    @Getter
    private int minVolume;
    @Getter
    private boolean maxVolumeEnabled;
    @Getter
    private int maxVolume;
    @Getter
    private boolean minProfitEnabled;
    @Getter
    private int minProfit;
    @Getter
    private boolean maxProfitEnabled;
    @Getter
    private int maxProfit;
    @Getter
    private int maxSellTime;

    @Getter
    private String customJavaScriptCode;


    public void initialize() {
        updateFromConfig();
        log.info("Initialized flip strategy");
    }

    public void updateFromConfig() {
        // Basic settings
        flipSlots = (Rs2Player.isMember() ? 8 : 3) - config.reservedSlots();
        maxInvestment = config.maxInvestment();
        underCutPercentage = config.underCutPercentage();
        maxSellTime = config.maxSellTime();
        maxBuyTime = config.maxBuyTime();

        // Filtering criteria
        minRoiEnabled = config.minRoiEnabled();
        minROI = config.minRoi();
        maxRoiEnabled = config.maxRoiEnabled();
        maxROI = config.maxRoi();
        minVolumeEnabled = config.minVolumeEnabled();
        minVolume = config.minVolume();
        maxVolumeEnabled = config.maxVolumeEnabled();
        maxVolume = config.maxVolume();
        minProfitEnabled = config.minProfitEnabled();
        minProfit = config.minProfit();
        maxProfitEnabled = config.maxProfitEnabled();
        maxProfit = config.maxProfit();

        customJavaScriptCode = config.customJavaScriptCode();

        log.info("Updated strategy from config");
    }

    public boolean meetsRequirements(FlipOpportunity opportunity) {
        return opportunity.getLowPrice() < maxInvestment
                && opportunity.getProfit() > 10;
    }

    public boolean meetsCriteria(FlipOpportunity opportunity) {
        // If custom JavaScript logic is enabled, use it instead of standard criteria
        if (!Objects.equals(customJavaScriptCode, "")) {
            try {
                return evaluator.evaluate(customJavaScriptCode, opportunity);
            } catch (Exception e) {
                log.error("Error evaluating custom JavaScript logic: {}", e.getMessage());
                return false; // Fallback to rejecting the opportunity
            }
        }

        // Existing standard criteria logic
        return meetsStandardCriteria(opportunity);
    }

    private boolean meetsStandardCriteria(FlipOpportunity opportunity) {
        // Check profit filter
        if (minProfitEnabled && opportunity.getProfit() < minProfit) {
            return false;
        }
        if (maxProfitEnabled && opportunity.getProfit() > maxProfit) {
            return false;
        }

        // Check ROI filter
        if (minRoiEnabled && opportunity.getRoi() < minROI) {
            return false;
        }
        if (maxRoiEnabled && opportunity.getRoi() > maxROI) {
            return false;
        }

        // Check volume filter
        if (minVolumeEnabled && opportunity.getDailyVolume().getLeft() < minVolume) {
            return false;
        }
        if (maxVolumeEnabled && opportunity.getDailyVolume().getLeft() > maxVolume) {
            return false;
        }

        return true;
    }

}
