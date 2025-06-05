package net.runelite.client.plugins.microbot.geflipper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("geflipperconfig")
public interface GeFlipperConfig extends Config {

    @ConfigItem(
            keyName = "enableOverlay",
            name = "Enable Overlay",
            description = "Show profit information overlay",
            hidden = true
    )
    default boolean enableOverlay() {
        return true;
    }

    @ConfigItem(
            keyName = "activeFlips",
            name = "",
            description = "",
            hidden = true
    )
    default String activeFlips() {
        return "[]";
    }

    @ConfigItem(
            keyName = "activeFlips",
            name = "",
            description = "",
            hidden = true
    )
    void setActiveFlips(String activeFlips);

    @ConfigItem(
            keyName = "tradeLimits",
            name = "",
            description = "",
            hidden = true
    )
    default String tradeLimits() {
        return "{}";
    }

    @ConfigItem(
            keyName = "tradeLimits",
            name = "",
            description = "",
            hidden = true
    )
    void setTradeLimits(String tradeLimits);

    @ConfigItem(
            keyName = "historicalProfit",
            name = "",
            description = "",
            hidden = true
    )
    default long historicalProfit() {
        return 0;
    }

    @ConfigItem(
            keyName = "historicalProfit",
            name = "",
            description = "",
            hidden = true
    )
    void setHistoricalProfit(long historicalProfit);

    // Configurable by the UI Panel
    @ConfigItem(keyName = "reservedSlots",
            name = "Reserved Slots",
            description = "Number of GE slots to keep available for manual trading",
            hidden = true)
    @Range(min = 0, max = 8)
    default int reservedSlots() {
        return 0;
    }

    @ConfigItem(keyName = "maxInvestment",
            name = "Max Investment",
            description = "Maximum amount of gold to invest",
            hidden = true)
    default int maxInvestment() {
        return 1000;
    }

    @ConfigItem(keyName = "underCutPercentage",
            name = "Undercut Percentage",
            description = "What percentage to undercut by",
            hidden = true)
    default int underCutPercentage() {
        return 20;
    }
    @ConfigItem(keyName = "maxBuyTimeEnabled",
            name = "Max Buy Time Enabled",
            description = "Maximum buy time enabled",
            hidden = true)
    default boolean maxBuyTimeEnabled() {
        return true;
    }
    @ConfigItem(keyName = "maxBuyTime",
            name = "Max Buy Time",
            description = "Maximum time to spend buying in minutes",
            hidden = true)
    default int maxBuyTime() {
        return 1440;
    }

    @ConfigItem(keyName = "maxSellTimeEnabled",
            name = "Max Sell Time Enabled",
            description = "Maximum sell time enabled",
            hidden = true)
    default boolean maxSellTimeEnabled() {
        return true;
    }
    @ConfigItem(keyName="maxSellTime",
            name = "Max Sell Time",
            description = "Maximum time to spend selling in minutes",
            hidden = true)
    default int maxSellTime() {
        return 1440;
    }

    @ConfigItem(keyName = "minRoiEnabled",
            name = "Min ROI % Enabled",
            description = "Min ROI % enabled",
            hidden = true)
    default boolean minRoiEnabled() {
        return true;
    }
    @ConfigItem(keyName = "minRoi",
            name = "Min ROI %",
            description = "Minimum ROI %",
            hidden = true)
    default int minRoi() {
        return 0;
    }

    @ConfigItem(keyName = "maxRoiEnabled",
            name = "Max ROI % Enabled",
            description = "Max ROI % enabled",
            hidden = true)
    default boolean maxRoiEnabled() {
        return true;
    }
    @ConfigItem(keyName = "maxRoi",
            name = "Max ROI %",
            description = "Maximum ROI %",
            hidden = true)
    default int maxRoi() {
        return 10;
    }

    @ConfigItem(keyName = "minVolumeEnabled",
            name = "Min Volume Enabled",
            description = "Minimum daily volume enabled",
            hidden = true)
    default boolean minVolumeEnabled() {
        return true;
    }
    @ConfigItem(keyName = "minVolume",
            name = "Min Volume",
            description = "Minimum daily volume",
            hidden = true)
    default int minVolume() {
        return 0;
    }

    @ConfigItem(keyName = "maxVolumeEnabled",
            name = "Max Volume Enabled",
            description = "Maximum daily volume enabled",
            hidden = true)
    default boolean maxVolumeEnabled() {
        return true;
    }
    @ConfigItem(keyName = "maxVolume",
            name = "Max Volume",
            description = "Maximum daily volume",
            hidden = true)
    default int maxVolume() {
        return 100;
    }

    @ConfigItem(keyName = "minProfitEnabled",
            name = "Min Profit GP Enabled",
            description = "Minimum profit in GP Enabled",
            hidden = true)
    default boolean minProfitEnabled() {
        return true;
    }
    @ConfigItem(keyName = "minProfit",
            name = "Min Profit GP",
            description = "Minimum profit in GP",
            hidden = true)
    default int minProfit() {
        return 0;
    }

    @ConfigItem(keyName = "maxProfitEnabled",
            name = "Max Profit GP Enabled",
            description = "Maximum profit in GP Enabled",
            hidden = true)
    default boolean maxProfitEnabled() {
        return true;
    }
    @ConfigItem(keyName = "maxProfit",
            name = "Max Profit GP",
            description = "Maximum profit in GP",
            hidden = true)
    default int maxProfit() {
        return 100;
    }

    @ConfigItem(
            keyName = "customJavaScriptCode",
            name = "Custom JavaScript Code",
            description = "JavaScript code that returns boolean for filtering opportunities",
            hidden = true
    )
    default String customJavaScriptCode() { return ""; }
}
