package net.runelite.client.plugins.microbot.geflipper.services;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.geflipper.managers.FlipManager;
import net.runelite.client.plugins.microbot.geflipper.managers.StrategyManager;
import net.runelite.client.plugins.microbot.geflipper.model.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class FlipAnalyzer {

    @Inject
    private PriceService priceService;

    @Inject
    private FlipManager flipManager;

    @Inject
    private StrategyManager strategyManager;

    @Inject
    private TradeLimitService tradeLimitService;

    @Inject
    private WealthService wealthService;

    @Inject
    private AdvancedCriteriaEvaluator advancedCriteriaEvaluator;

    public List<FlipOpportunity> findFlipOpportunities() {
        return new ArrayList<>(priceService.getItemPrices().values());
    }

    public List<FlipOpportunity> findBestOpportunities() {
        List<FlipOpportunity> opportunities = findFlipOpportunities();

        return opportunities.stream()
                .filter(opportunity -> flipManager.getFlipByItemId(opportunity.getItemId()).isEmpty())
                .filter(strategyManager::meetsRequirements)
                .filter(strategyManager::meetsCriteria)
                .sorted(this::compareOpportunities)
                .collect(Collectors.toList());
    }

    public FlipOpportunity analyzeItem(int itemId) {
        FlipOpportunity flipOpportunity = priceService.getItemPrices().get(itemId);
        if (flipOpportunity == null) {
            return null;
        }

        return flipOpportunity;
    }

    public int proposePrice(Flip flip) {
        FlipOpportunity opportunity = analyzeItem(flip.getItemId());
        if (opportunity == null) {
            return 0;
        }
        int suggestedPrice = getSuggestedSellPrice(opportunity);
        // If the suggested price is significantly lower than the current sell price, adjust it according to undercut percentage
        if ((double) suggestedPrice / flip.getSellPrice() < 0.90 || suggestedPrice == flip.getSellPrice()) {
            return getSuggestedSellPrice(opportunity);
        }
        return suggestedPrice;
    }

    private int compareOpportunities(FlipOpportunity a, FlipOpportunity b) {
        return Double.compare(b.getProfit(), a.getProfit());
    }

    public int getSuggestedQuantity(FlipOpportunity opportunity)
    {
        int suggestedBuyPrice = getSuggestedBuyPrice(opportunity);
        long capitalLeft = Math.min(wealthService.getCurrentGold(), strategyManager.getMaxInvestment() - wealthService.getInvestmentCapital());
        int affordableQuantity = (int) Math.floor(capitalLeft / (double) suggestedBuyPrice);
        int maxPossible = Math.min(opportunity.getLowVolume(), tradeLimitService.getRemainingLimit(opportunity.getItemId()));
        return Math.min(affordableQuantity, maxPossible);
    }

    public int getSuggestedSellPrice(FlipOpportunity opportunity) {
        return (int) Math.floor(opportunity.getHighPrice() * (1 - (double) strategyManager.getUnderCutPercentage() / 100));
    }

    public int getSuggestedBuyPrice(FlipOpportunity opportunity) {
        return (int) Math.ceil(opportunity.getLowPrice() * ((double) strategyManager.getUnderCutPercentage() / 100 + 1));
    }
}
