package net.runelite.client.plugins.microbot.geflipper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.geflipper.managers.FlipManager;
import net.runelite.client.plugins.microbot.geflipper.managers.StrategyManager;
import net.runelite.client.plugins.microbot.geflipper.model.Flip;
import net.runelite.client.plugins.microbot.geflipper.model.FlipOpportunity;
import net.runelite.client.plugins.microbot.geflipper.model.GeOffer;
import net.runelite.client.plugins.microbot.geflipper.services.FlipAnalyzer;
import net.runelite.client.plugins.microbot.geflipper.services.GeService;
import net.runelite.client.plugins.microbot.geflipper.services.TradeLimitService;
import net.runelite.client.plugins.microbot.geflipper.services.WealthService;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.coords.Rs2WorldPoint;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GeFlipperScript extends Script {

    @Inject
    private GeService geService;

    @Inject
    private WealthService wealthService;

    @Inject
    private FlipManager flipManager;

    @Inject
    private StrategyManager strategyManager;

    @Inject
    private FlipAnalyzer flipAnalyzer;

    @Inject
    private TradeLimitService tradeLimitService;

    private boolean initialized = false;

    public boolean run(GeFlipperPlugin plugin) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (!Microbot.isLoggedIn() || !super.run() || plugin.isPaused()) return;

            try {
                if (!initialized) {
                    initialize();
                    return;
                }

                updateFlips(geService.getOffers());

                if(!geService.isGrandExchangeOpen()) geService.openGrandExchange();

                // Main flipping cycle
                executeFlippingCycle();

            } catch (Exception e) {
                log.error("Error in GeFlipperScript cycle", e);
            }

        }, 0, 500, TimeUnit.MILLISECONDS); // Run every 0.5 seconds

        return true;
    }

    private void initialize() {
        log.info("Initializing GeFlipperScript...");
        if (Rs2WorldPoint.quickDistance(BankLocation.GRAND_EXCHANGE.getWorldPoint(), Rs2Player.getRs2WorldPoint().getWorldPoint()) > 10) {
            Rs2Walker.walkWithState(Rs2Bank.getNearestBank().getWorldPoint());
            Rs2Bank.openBank();
            Rs2Bank.withdrawItem("Ring of wealth (");
            Rs2Inventory.equip("Ring of wealth (");
            Rs2Walker.walkWithState(BankLocation.GRAND_EXCHANGE.getWorldPoint());
        }
        var capital = wealthService.getCurrentGold() + wealthService.getInvestmentCapital();
        if (capital < strategyManager.getMaxInvestment()) {
            Rs2Bank.openBank();
            Rs2Bank.withdrawAllButOne("Coins");
        }
        initialized = true;
    }

    public void updateFlips(GeOffer[] offers) {
        for (GeOffer offer : offers) {
            flipManager.getFlipByItemId(offer.getItemId()).ifPresent(flip -> {
                flip.setProgression(offer.getQuantitySold());
                flip.setGeSlot(offer.getSlot());
                switch (offer.getState()) {
                    case EMPTY: // We picked up the item after buying it.
                        flip.setStatus(Flip.FlipStatus.PENDING_SALE);
                        break;
                    case SOLD:
                        flip.setStatus(Flip.FlipStatus.SOLD);
                        break;
                    case BOUGHT:
                        flip.setStatus(Flip.FlipStatus.BOUGHT);
                        tradeLimitService.recordPurchase(flip.getItemId(), offer.getQuantitySold());
                        break;
                    case BUYING:
                        flip.setStatus(Flip.FlipStatus.BUYING);
                        tradeLimitService.recordPurchase(flip.getItemId(), offer.getQuantitySold());
                        break;
                    case SELLING:
                        flip.setStatus(Flip.FlipStatus.SELLING);
                        break;
                }
            });
        }
        flipManager.saveFlips();
    }

    private void executeFlippingCycle() {

        if (!flipManager.getActiveFlips().isEmpty()) {
            // Vanished flips, user going ham!
            flipManager.getActiveFlips().removeIf(flip ->
                    Rs2Inventory.get(flip.getItemName()) == null &&
                            Arrays.stream(geService.getOffers()).noneMatch(geOffer -> geOffer.getItemId() == flip.getItemId())
            );

            // Sold offers
            for (Flip flip : flipManager.getFlipsByStatus(Flip.FlipStatus.SOLD)) {
                geService.collectOffer(flip.getGeSlot());
                flipManager.completeFlip(flip);
                wealthService.appendSessionProfit(flip.getProfit());
                return;
            }

            // Bought offers
            for (Flip flip : flipManager.getFlipsByStatus(Flip.FlipStatus.BOUGHT)) {
                geService.collectOffer(flip.getGeSlot());
                flip.setStatus(Flip.FlipStatus.PENDING_SALE);
                return;
            }

            // Pending sale offers
            for (Rs2ItemModel item : Rs2Inventory.items()) {
                int itemId = item.getId();
                if (item.isNoted()) {
                    itemId = item.getItemComposition().getLinkedNoteId();
                }
                flipManager.getFlipByItemId(itemId).ifPresent( flip -> {
                    geService.placeSellOffer(flip.getItemId(), flip.getQuantity(), flip.getSellPrice());
                    flip.setStatus(Flip.FlipStatus.SELLING);
                    flip.setSellTime(System.currentTimeMillis());
                });
            }

            // Stale offers
            List<Flip> activeFlips = flipManager.getActiveFlips();
            for (Flip flip : activeFlips) {
                if (shouldCancelFlip(flip)) {
                    int slot = flip.getGeSlot();
                    GeOffer offer = geService.getOffers()[slot];
                    if (flip.getStatus() == Flip.FlipStatus.SELLING) {
                        if (offer.getQuantitySold() != flip.getQuantity()) {
                            wealthService.appendSessionProfit((long) (flip.getProgression() * flip.getSellPrice() * 0.98));
                            flip.setQuantity(flip.getQuantity() - flip.getProgression());
                            flip.setProgression(0);
                        }
                        flip.setSellPrice(flipAnalyzer.proposePrice(flip));
                    }
                    if (flip.getStatus() == Flip.FlipStatus.BUYING) {
                        flip.setQuantity(offer.getQuantitySold());
                        flip.setProgression(0);
                    }
                    geService.cancelOffer(flip.getGeSlot());
                    return;
                }
            }
        }
        
        if (geService.slotsAvailable()) {
            lookForNewOpportunities();
        }
    }

    private void lookForNewOpportunities() {
        try {
            // Find opportunities
            List<FlipOpportunity> opportunities = flipAnalyzer.findBestOpportunities();

            for (FlipOpportunity opportunity : opportunities) {
                if (!canStartFlip(opportunity)) {
                    continue;
                }

                if (flipManager.getFlipByItemId((opportunity.getItemId())).isPresent()) {
                    continue;
                }

                executeNewFlip(opportunity);
                break; // Start one flip at a time
            }

        } catch (Exception e) {
            log.error("Error looking for new opportunities", e);
        }
    }

    private void executeNewFlip(FlipOpportunity opportunity) {
        try {
            // Ensure GE is open
            if (!geService.isGrandExchangeOpen()) {
                if (!geService.openGrandExchange()) {
                    log.warn("Could not open Grand Exchange");
                    return;
                }
            }

            int quantity = flipAnalyzer.getSuggestedQuantity(opportunity);
            int buyPrice = flipAnalyzer.getSuggestedBuyPrice(opportunity);
            int sellPrice = flipAnalyzer.getSuggestedSellPrice(opportunity);

            // Create and track the flip
            Flip flip = new Flip(
                    opportunity.getItemId(),
                    quantity,
                    buyPrice,
                    sellPrice
            );

            flipManager.addFlip(flip);
            int slot = geService.placeBuyOffer(opportunity.getItemId(), quantity, buyPrice);
            flip.setStatus(Flip.FlipStatus.BUYING);
            flip.setGeSlot(slot);
            log.info("Started new flip: {} x {} @ {} gp (Expected profit: {} gp)",
                    flip.getItemName(), quantity, buyPrice, flip.getProfit());

        } catch (Exception e) {
            log.error("Error executing new flip for {}", opportunity.getName(), e);
        }
    }

    public boolean canStartFlip(FlipOpportunity opportunity) {
        // Check total investment limit
        int currentInvestment = flipManager.getTotalInvestment();
        int proposedQuantity = flipAnalyzer.getSuggestedQuantity(opportunity);
        if (proposedQuantity == 0) {
            log.debug("Cannot start flip: no volume available");
            return false;
        }
        int proposedInvestment = flipAnalyzer.getSuggestedBuyPrice(opportunity) * proposedQuantity;

        if (currentInvestment + proposedInvestment > strategyManager.getMaxInvestment()) {
            log.debug("Cannot start flip: would exceed total investment limit");
            return false;
        }

        // Check if player has enough gold
        long capitalLeft = strategyManager.getMaxInvestment() - wealthService.getInvestmentCapital();
        if (proposedInvestment > capitalLeft) {
            log.debug("Cannot start flip: insufficient gold ({} needed, {} available)",
                    proposedInvestment, capitalLeft);
            return false;
        }

        return true;
    }

    public boolean shouldCancelFlip(Flip flip) {
        // Cancel if flip is taking too long
        if (flip.getStatus() == Flip.FlipStatus.SELLING) {
            var timeElapsed = flip.getSellTime() == null ? System.currentTimeMillis() : System.currentTimeMillis() - flip.getSellTime();
//            if (timeElapsed > strategyManager.getMaxSellTime() * 60000L) {
            if (timeElapsed > 1800000) { // 30 minutes for now
                log.info("Should cancel flip for item {} - exceeded max duration", flip.getItemId());
                return true;
            }
        }

        // Cancel if stuck in buying state for too long
        if (flip.getStatus() == Flip.FlipStatus.BUYING) {
            var timeElapsed = System.currentTimeMillis() - flip.getBuyTime();
//            if (timeElapsed > strategyManager.getMaxBuyTime() * 60000L) {
            if (timeElapsed > 900000) { // 15 minutes for now
                log.info("Should cancel flip for item {} - stuck in buying state", flip.getItemId());
                return true;
            }
        }

        return false;
    }

    @Override
    public void shutdown() {
        log.info("Shutting down GeFlipperScript...");
        initialized = false;
        super.shutdown();
    }
}
