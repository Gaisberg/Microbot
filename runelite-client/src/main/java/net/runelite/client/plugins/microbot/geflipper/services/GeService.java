package net.runelite.client.plugins.microbot.geflipper.services;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.geflipper.model.GeOffer;
import net.runelite.client.plugins.microbot.geflipper.reflection.GrandExchangeReflection;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;


@Singleton
@Slf4j
public class GeService {

    @Inject
    private Client client;

    // Grand Exchange Widget IDs
    private static final int GE_INTERFACE_GROUP = 465;

    // GE Slot widgets (0-7)
    private static final int GE_SLOT_BASE = 7;

    public boolean isGrandExchangeOpen() {
        return Rs2GrandExchange.isOpen();
    }

    public boolean openGrandExchange() {
        return Rs2GrandExchange.openExchange();
    }

    public GeOffer[] getOffers() {
        return GrandExchangeReflection.getGeOffers();
    }

    public Optional<Integer> getAvailableSlot() {
        return Arrays.stream(getOffers())
                .filter(geOffer -> geOffer.getState() == GrandExchangeOfferState.EMPTY)
                .findFirst()
                .map(GeOffer::getSlot);
    }

    public boolean slotsAvailable(){
        return getAvailableSlot().isPresent();
    }

    public int placeBuyOffer(int itemId, int quantity, int price) {
        Optional<Integer> slot = getAvailableSlot();
        if (slot.isEmpty()) {
            log.warn("No available GE slots for buy offer");
            return -1;
        }

        return placeBuyOffer(itemId, quantity, price, slot.get());
    }

    public int placeBuyOffer(int itemId, int quantity, int price, int slot) {
        if (!isGrandExchangeOpen()) {
            openGrandExchange();
        }

        try {
            log.info("Placing buy offer: {} x {} at {} gp in slot {}", itemId, quantity, price, slot);
            if (Rs2GrandExchange.buyItem(itemId, price, quantity, slot)) {
                log.info("Successfully placed buy offer");
                return slot;
            }

            throw new Exception();

        } catch (Exception e) {
            log.error("Failed to place buy offer", e);
            return -1;
        }
    }

    public int placeSellOffer(int itemId, int quantity, int price) {
        try {
            if (Rs2GrandExchange.sellItem(itemId, quantity, price)) {
                log.info("Successfully placed sell offer");

                return Arrays.stream(getOffers())
                        .filter(geOffer -> geOffer.getItemId() == itemId)
                        .findFirst()
                        .map(GeOffer::getSlot).orElse(-1);
            }
            throw new Exception();
        } catch (Exception e) {
            log.error("Failed to place sell offer", e);
            return -1;
        }
    }

    public boolean cancelOffer(int slot) {
        if (!isGrandExchangeOpen()) {
            return false;
        }

        try {
            log.info("Cancelling offer in slot {}", slot);
            Rs2GrandExchange.backToOverview();

            Rs2Widget.clickWidget(getSlotWidget(slot));
            Global.sleepUntilOnClientThread(() -> Rs2Widget.isWidgetVisible(465, 23, 0));
            Rs2Widget.clickWidget(465, 23, 0);
            Global.sleepUntilOnClientThread(() -> Rs2Widget.getWidget(465, 23, 4).getTextColor() == 9371648);
            collectOfferSlot(2);
            collectOfferSlot(3);
            return true;
        } catch (Exception e) {
            log.error("Failed to cancel offer in slot {}", slot, e);
            return false;
        }
    }

    public boolean collectOffer(int slot) {
        if (!isGrandExchangeOpen()) {
            return false;
        }

        try {
            log.info("Collecting offer from slot {}", slot);

            Widget slotWidget = getSlotWidget(slot);
            if (slotWidget != null) {
                Rs2Widget.clickWidget(slotWidget);
                Global.sleepUntilOnClientThread(() -> Rs2Widget.getWidget(465, 24) != null);
                collectOfferSlot(2);
                collectOfferSlot(3);
                Rs2Inventory.waitForInventoryChanges(3000);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("Failed to collect offer from slot {}", slot, e);
            return false;
        }
    }

    private void collectOfferSlot(int slot) {
        var widget = Rs2Widget.getWidget(465, 24).getChild(slot);
        if (widget == null) {
            return;
        }
        String[] actions = Microbot.getClientThread().runOnClientThreadOptional(widget::getActions).orElse(new String[0]);
        if (actions.length == 0) {
            return;
        }
        var itemName = widget.getName();
        var itemId = widget.getItemId();
        var bounds = widget.getBounds();

        var identifier = 2;
        if (Arrays.asList(actions).contains("Collect-notes")) {
            identifier = 1;
        }
        Microbot.doInvoke(new NewMenuEntry(slot, 30474264, MenuAction.CC_OP.getId(), identifier, itemId, itemName), bounds);
    }

    private Widget getSlotWidget(int slot) {
        return Rs2Widget.getWidget(GE_INTERFACE_GROUP, GE_SLOT_BASE + slot);
    }

}
