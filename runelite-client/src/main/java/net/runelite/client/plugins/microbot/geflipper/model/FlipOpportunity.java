package net.runelite.client.plugins.microbot.geflipper.model;

import lombok.Getter;
import lombok.Setter;
import net.runelite.client.plugins.microbot.geflipper.managers.StrategyManager;
import net.runelite.client.plugins.microbot.geflipper.services.TradeLimitService;
import net.runelite.client.plugins.microbot.geflipper.services.WealthService;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

@Getter
@Setter
public class FlipOpportunity {

    private final int itemId;
    private final String name;
    private final int limit;
    private final boolean members;

    private int lowPrice;
    private int lowVolume;
    private int highPrice;
    private int highVolume;
    private double roi;
    private double profit;

    private double trend;
    private double volatility;

    private WikiLatestResponse.PriceEntry latest;
    private final Map<TimeFrame, WikiHistoryResponse.PriceEntry> historicalData = new ConcurrentHashMap<>();

    public FlipOpportunity(WikiMappingResponse mapping) {
        this.itemId = mapping.getId();
        this.name = mapping.getName();
        this.limit = mapping.getLimit() == null ? 0 : mapping.getLimit();
        this.members = mapping.isMembers();
    }

    public void setHistoricalData(TimeFrame timeFrame, WikiHistoryResponse.PriceEntry data) {
        historicalData.put(timeFrame, data);

        this.lowPrice = getCurrentLowPrice();
        this.highPrice = getCurrentHighPrice();
        this.lowVolume = getDailyVolume().getLeft();
        this.highVolume = getDailyVolume().getRight();
        this.profit = highPrice * 0.98 - lowPrice; // 0.98 for tax
        this.roi = (profit / lowPrice) * 100;

        this.trend = calculateTrend();
        this.volatility = calculateVolatility();
    }

    private int getCurrentHighPrice() {
        if (latest != null && latest.getHigh() != null) {
            return latest.getHigh();
        }

        // Check all timeframes from newest to oldest
        for (TimeFrame timeframe : TimeFrame.getOrderedTimeFrames()) {
            WikiHistoryResponse.PriceEntry entry = historicalData.get(timeframe);
            if (entry != null && entry.getAvgHighPrice() != null) {
                return entry.getAvgHighPrice();
            }
        }

        return 0;
    }

    private int getCurrentLowPrice() {
        if (latest != null && latest.getLow() != null) {
            return latest.getLow();
        }

        // Check all timeframes from newest to oldest
        for (TimeFrame timeframe : TimeFrame.getOrderedTimeFrames()) {
            WikiHistoryResponse.PriceEntry entry = historicalData.get(timeframe);
            if (entry != null && entry.getAvgLowPrice() != null) {
                return entry.getAvgLowPrice();
            }
        }

        return 0;
    }

    public Pair<Integer, Integer> getDailyVolume() {
        // Try 24h volume first (most accurate)
        WikiHistoryResponse.PriceEntry dailyEntry = historicalData.get(TimeFrame.TWENTY_FOUR_HOURS);
        if (dailyEntry != null) {
            int lowVolume = dailyEntry.getLowPriceVolume() != null ? dailyEntry.getLowPriceVolume() : 0;
            int highVolume = dailyEntry.getHighPriceVolume() != null ? dailyEntry.getHighPriceVolume() : 0;
            if (lowVolume > 0 || highVolume > 0) {
                return Pair.of(lowVolume, highVolume);
            }
        }

        // Fallback: extrapolate from any available timeframe (longest to shortest)
        for (TimeFrame timeframe : TimeFrame.getOrderedTimeFramesOldestFirst()) {
            if (timeframe == TimeFrame.TWENTY_FOUR_HOURS) continue; // Already checked

            WikiHistoryResponse.PriceEntry entry = historicalData.get(timeframe);
            if (entry != null) {
                Integer lowVolume = entry.getLowPriceVolume();
                Integer highVolume = entry.getHighPriceVolume();

                if (lowVolume != null && highVolume != null && (lowVolume > 0 || highVolume > 0)) {
                    int totalVolume = lowVolume + highVolume;
                    int multiplier = getVolumeMultiplier(timeframe);
                    return Pair.of(lowVolume * multiplier, highVolume * multiplier);
                }
            }
        }

        return Pair.of(0, 0);
    }

    private int getVolumeMultiplier(TimeFrame timeframe) {
        return (int) (24 * 60 / timeframe.getDuration().toMinutes());
    }

    private double calculateTrend() {
        var currentPrice = (lowPrice + highPrice) / 2;

        List<Long> timePoints = new ArrayList<>();
        List<Integer> pricePoints = new ArrayList<>();

        if (currentPrice > 0) {
            timePoints.add(0L);
            pricePoints.add(currentPrice);
        }

        for (TimeFrame timeframe : TimeFrame.getOrderedTimeFrames()) {
            WikiHistoryResponse.PriceEntry entry = historicalData.get(timeframe);
            if (entry != null) {
                var price = entry.getAveragePrice();
                long minutesAgo = timeframe.getDuration().toMinutes();
                timePoints.add(minutesAgo);
                pricePoints.add(price);
            }
        }

        if (pricePoints.size() < 2) {
            return 0.0;
        }

        // Calculate linear regression slope
        int n = pricePoints.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = timePoints.get(i);
            double y = pricePoints.get(i);

            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        // least squares formula
        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) {
            return 0.0;
        }

        double slope = (n * sumXY - sumX * sumY) / denominator;

        double hourlyChange = -slope * 60;
        double avgPrice = sumY / n;

        if (avgPrice <= 0) {
            return 0.0;
        }

        return (hourlyChange / avgPrice) * 100.0;
    }

    private double calculateVolatility() {
        var currentPrice = (lowPrice + highPrice) / 2;

        List<Integer> pricePoints = new ArrayList<>();

        if (currentPrice > 0) {
            pricePoints.add(currentPrice);
        }

        for (TimeFrame timeframe : TimeFrame.getOrderedTimeFrames()) {
            WikiHistoryResponse.PriceEntry entry = historicalData.get(timeframe);
            if (entry != null) {
                var price = entry.getAveragePrice();
                pricePoints.add(price);
            }
        }

        if (pricePoints.size() < 2) {
            return 0.0;
        }

        double meanPrice = pricePoints.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        if (meanPrice <= 0) {
            return 0.0;
        }

        double variance = pricePoints.stream()
                .mapToDouble(price -> Math.pow(price - meanPrice, 2))
                .average()
                .orElse(0.0);

        double standardDeviation = Math.sqrt(variance);
        return (standardDeviation / meanPrice) * 100.0;
    }
}